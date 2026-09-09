# Plan: keycloak-user-storage를 Harness 기반 Spec-Driven 개발 환경으로 전환

## Context

현재 `.claude/`는 자유 서술형 규칙(`rules/`) + 코딩 컨벤션(`conventions/`) + 수동 백로그(`backlogs/`) + 수동 계획서(`plans/`) 구조다.
AI가 이 문서들을 "참고"하지만, **강제(enforcement)되는 게이트가 없다** — 스펙 없이 코드가 시작될 수 있고,
테스트 없이 구현이 들어갈 수 있고, `./gradlew build -x test` 같은 우회가 가능하다.

목표는 Loiane Groner의
[specs-driven-development-spring-angular](https://github.com/loiane/specs-driven-development-spring-angular)
토큿킷(+ "Harness Engineering" 아티클)의 방법론을 **백엔드(Spring) 전용**으로 이식하여:

1. 모든 기능이 `.specs/<feature>/` 아래 **번호가 매겨진 산출물 체인**(01-spec → 08-review)을 거치도록 하고
2. 각 단계 전환을 **hook / harness 게이트**로 강제하며 (스펙 없이 진행 불가, 실패 테스트 없이 `src/main` 수정 불가, skip 플래그 금지)
3. 기존 `conventions/`는 **skill**로, 기존 `rules/`는 **methodology + skill**로 재편하고
4. Gradle 기반 **자체 검증 harness 스크립트**를 만들어 `/validate`가 소비하도록 한다.

**UI/프론트엔드 영역은 전부 제외** — Angular agent/skill, Testcontainers, Flyway/Liquibase 가정 모두 드롭.

### 결정된 방침 (사용자 확인 완료)

| 항목 | 결정 |
|---|---|
| 이식 범위 | **Lean spec-driven core** — 핵심 command/agent/skill만, 풀 토큿킷의 엄격한 부분집합으로 설계해 나중에 확장 가능 |
| 기존 파일 | **전량 이관 후 원본 삭제** — `backlogs/`·`plans/`는 완료 이력으로 압축(BL-01~10, step01~10 모두 완료 상태), git history가 상세 보존 |
| Harness 게이트 강도 | **Brownfield ratchet** — `/onboard`가 baseline 캡처, 게이트는 baseline 대비 회귀만 차단, 신규 코드는 풀 기준(변경 라인 커버리지 ≥95%) |
| build.gradle 변경 | **이번 변경에서는 스캐폴딩만** — harness.sh + 문서화된 build.gradle 패치를 파일로 남기고, 실제 플러그인 배선은 후속 spec으로 분리 (내부 `reposilite.kind.internal` 저장소 플러그인 미러링 불확실) |

---

## 작업 브랜치

```bash
git checkout -b chore/spec-driven-harness   # main에서 분기
```
프로덕션 코드 변경 없음. `.claude/` 재편 + `.specs/` 신설 + `CLAUDE.md`/`MEMORY.md` 갱신만.

---

## 최종 `.claude/` 구조 (Lean, 백엔드 전용)

```
.claude/
├── README.md                     # 워크플로우 진입점 (구 rules/README.md 대체)
├── settings.json                 # + hooks{} + permissions.deny (skip 플래그, git commit/push)
├── settings.local.json           # 유지
├── docs/
│   ├── methodology.md            # 6-phase 방법론 (구 rules/ 전체를 여기로)
│   ├── spec-format.md            # EARS-lite AC 작성법 + ID 규칙
│   └── harness-gradle.md         # ★적용 안 함★ — build.gradle 패치 명세 (후속 spec 입력물)
├── commands/
│   ├── onboard.md                # Phase 0 — _stack.json, _baseline.json, _onboarding.md
│   ├── spec.md                   # Phase 1 — 01-spec.md (EARS-lite, Q-NNN, 도메인/데이터 인테이크)
│   ├── spec-review.md            # Phase 2 — 02-spec-review.md
│   ├── plan.md                   # Phase 3 — 03-design.md + 04-tasks.md + adr/ + .tdd-state.json
│   ├── build.md                  # Phase 4 — TDD red→green→refactor→simplify + 05-implementation-log.md
│   ├── validate.md               # Phase 5 — harness.sh 실행 + 07-validation-report.md + 07a-traceability.md
│   ├── review.md                 # Phase 6 — 08-code-review.md (루브릭 대조)
│   ├── status.md                 # 읽기 전용 — 기능 파이프라인 현황
│   └── help.md                   # command 카탈로그
├── agents/
│   ├── spec-author.md            # Phase 1–2 소유
│   ├── architect.md              # Phase 3 소유 (design + tasks + ADR) — 구 plans/ 작성자 역할
│   ├── implementer.md            # Phase 4 green/refactor/simplify
│   ├── test-engineer.md          # Phase 4 red + Phase 5 갭 테스트
│   └── validator.md              # Phase 5–6 (validate + code-review 통합)
├── skills/
│   │  # ── 방법론 skill (구 rules/) ──
│   ├── ears-spec-authoring/SKILL.md
│   ├── requirements-traceability/SKILL.md         # AC-NNN ↔ test ↔ code ↔ gate
│   ├── vertical-slicing/SKILL.md                  # 구 rules/02 — /plan 태스크 분해(1–4h)
│   ├── context-curation/SKILL.md                  # 구 rules/03 — command "Reads" 근거
│   ├── openapi-contract-first/SKILL.md            # 구 rules/01 — JSON 계약 → 03-design.md OpenAPI 스케치
│   ├── tdd-red-green-refactor/SKILL.md
│   ├── clarity-over-cleverness/SKILL.md
│   ├── adr-authoring/SKILL.md
│   │  # ── 컨벤션 skill (구 conventions/) ──
│   ├── spring-layer-conventions/
│   │   ├── SKILL.md                               # 구 conventions/03 서술부
│   │   └── references/{tech-stack.md, layer-structure.md}   # 구 conventions/00, 03
│   ├── spring-error-handling/SKILL.md             # 구 conventions/01
│   ├── spring-logging/SKILL.md                    # 구 conventions/02
│   ├── usp-integration-testing/SKILL.md           # 구 conventions/04 (H2, Testcontainers 아님)
│   │  # ── harness skill ──
│   ├── gradle-harness/SKILL.md                    # harness.sh ↔ Gradle 태스크 매핑
│   ├── harness-report-parsing/SKILL.md            # build/reports, build/test-results 파싱
│   ├── jacoco-coverage-policy/SKILL.md            # ratchet 정책
│   ├── archunit-rules/SKILL.md                    # 구 conventions/03 "Controller≠Repository" 규칙 명세
│   └── code-review-rubric/SKILL.md                # Spring 코드리뷰 체크포인트
├── templates/
│   ├── spec.template.md
│   ├── spec-review.template.md
│   ├── design.template.md
│   ├── tasks.template.md
│   ├── adr.template.md
│   ├── implementation-log.template.md
│   ├── validation-report.template.md
│   ├── traceability.template.md
│   └── code-review.template.md
├── checklists/
│   ├── spec-review.md
│   ├── design-review.md
│   ├── implementation-dod.md
│   └── validation-gates.md
├── hooks/
│   ├── route-natural-language-aliases.sh          # "스펙 짜줘" → /spec 등 (한국어 별칭 포함)
│   ├── block-impl-without-failing-test.sh         # src/main/** 수정: .tdd-state.json phase=red + red_failure_excerpt 필수
│   ├── enforce-files-in-scope.sh                  # 수정 파일이 활성 태스크 files_in_scope 안에 있어야
│   ├── block-progress-on-open-questions.sh        # 활성 spec에 미해결 Q-NNN 있으면 구현 차단
│   └── forbid-skip-flags.sh                       # Bash: -x test, -x check, -Dpitest.skip, --no-verify 등 차단
└── scripts/
    ├── harness.sh                                 # Gradle 포팅 — 아래 "Harness" 절
    ├── detect-stack.sh                            # _stack.json 생성
    ├── check-new-code-coverage.sh                 # origin/main 대비 변경 라인 커버리지
    └── traceability.sh                            # AC-NNN → @Tag/@DisplayName 스캔
```

### `.specs/` (신설)

```
.specs/
├── README.md                     # 기능 인덱스 + "완료 이력 (BL-01 ~ BL-10)" 압축 표 (구 backlogs/00 대체)
├── _onboarding.md                # /onboard 산출
├── _stack.json                   # /onboard 산출 (detect-stack.sh)
├── _baseline.json                # /onboard --baseline 산출 (harness 기준선)
└── YYYY-MM-DD-<feature-id>/
    ├── 01-spec.md
    ├── 02-spec-review.md
    ├── 03-design.md
    ├── 04-tasks.md
    ├── 05-implementation-log.md
    ├── 07-validation-report.md
    ├── 07a-traceability.md
    ├── 08-code-review.md
    ├── .tdd-state.json           # { active_task, tasks: { "T-001": { phase, files_in_scope, acs_covered, red_failure_excerpt } } }
    └── adr/ADR-NNN-*.md
```
(06-test-plan은 lean에서 생략 — 갭 테스트는 `/validate`가 흡수. 번호는 비워 호환 유지.)

---

## 워크플로우 (6-phase + Phase 0)

```
/onboard  →  /spec  →  /spec-review  →  /plan  →  /build T-NNN  →  /validate  →  /review  →  commit
 (0)          (1)         (2)            (3)        (4, TDD 반복)      (5)          (6)
```

강제 규칙:
- **No invention** — 티켓/대화/코드에 없는 값은 `Q-NNN`으로 기록하고 사용자에게 질문. 기본값 임의 선택 금지.
- **Phase-exit 게이트** — 미해결 `Q-NNN`이 있으면 다음 단계 진입 불가.
- **TDD by construction** — `/build`는 실패하는 테스트를 먼저 쓰고 `.tdd-state.json`에 `red_failure_excerpt` 기록해야 `src/main/**` 수정 허용. (hook이 이중 강제)
- **한 번에 한 태스크** — 다른 태스크가 in-flight면 `/build` 거부.
- **커밋 전 정지** — `/build`는 태스크 완료 시 커밋 리마인더만, 자동 진행 안 함.

---

## 기존 파일 이관 매핑 (원본 삭제)

| 현재 | 이관 대상 | 변환 |
|---|---|---|
| `rules/README.md` | `.claude/README.md` + `docs/methodology.md` | 6-phase 방법론으로 재작성 (한국어) |
| `rules/01-contract-first.md` | `skills/openapi-contract-first/SKILL.md` | JSON-first 정신 유지 + "OpenAPI 스케치는 03-design.md" 명시 |
| `rules/02-vertical-slicing.md` | `skills/vertical-slicing/SKILL.md` | `/plan` 태스크 분해 기준(1–4h)으로 연결 |
| `rules/03-context-curation.md` | `skills/context-curation/SKILL.md` | command "Reads" 절 근거로 연결 |
| `rules/04-backlog-creation.md` | `commands/spec.md` 인테이크 절 | **UI 질문 삭제**, 도메인 엔티티 + 데이터 모델 질문만 유지 |
| `conventions/00-tech-stack.md` | `skills/spring-layer-conventions/references/tech-stack.md` | 거의 그대로 |
| `conventions/01-error-handling.md` | `skills/spring-error-handling/SKILL.md` | 그대로 + 전역 핸들러 현황 주석 |
| `conventions/02-logging.md` | `skills/spring-logging/SKILL.md` | 그대로 |
| `conventions/03-layer-structure.md` | `skills/spring-layer-conventions/SKILL.md` + `skills/archunit-rules/SKILL.md` | 서술→conventions skill / "Controller는 Repository 직접 주입 금지" 등→ArchUnit 규칙 명세 |
| `conventions/04-testing.md` | `skills/usp-integration-testing/SKILL.md` | 그대로 (H2 in-memory, RANDOM_PORT, PATCH 설정) |
| `backlogs/*` (BL-01~10, 전부 완료) | `.specs/README.md` "완료 이력" 표 (제목 + 1줄 결과 + 커밋 참조) | 폴더 삭제, 상세는 git history |
| `plans/step01~10` (전부 완료) | 위 표에 plan 링크 컬럼으로 흡수 | 폴더 삭제 |
| `settings.json` | `.claude/settings.json` | `hooks{}` 추가 + `permissions.deny`에 skip 플래그·`git commit`·`git push` |

---

## Harness (Gradle) — 스캐폴딩만

### `.claude/scripts/harness.sh` (동작 O)
- `./gradlew check` 실행 → `test` + `jacocoTestReport` (현재 있는 플러그인) 수행
- 이후 존재하는 태스크만 조건부 실행: `pitest`, `checkstyleMain`, `spotbugsMain`, `archTest`, OpenAPI 덤프
- `build/test-results/**/*.xml`, `build/reports/jacoco/**/jacoco.xml` 파싱 → `build/harness-summary.json` 생성
- **플러그인 없는 레이어는 `"status":"skipped"`** — 오늘 현재 상태에서 green으로 통과 (unit + coverage 게이트만 활성)
- `--report` / `--baseline` 모드 (Loiane와 동일 인터페이스)

### `detect-stack.sh` 출력 예시
```json
{ "build": "gradle", "java": "17", "spring_boot": "3.3.4", "db": "h2", "migration": "none",
  "harness_layers": { "unit": true, "coverage": true, "mutation": false,
                      "checkstyle": false, "spotbugs": false, "archunit": false,
                      "owasp": false, "openapi": false } }
```

### `.claude/docs/harness-gradle.md` (적용 X — 후속 spec 입력물)
문서로만 남길 `build.gradle` 패치:
- `plugins`: `checkstyle`, `com.github.spotbugs`, `info.solidsoft.pitest`, `org.springdoc.openapi-gradle-plugin`, (선택) `org.owasp.dependencycheck`
- `dependencies`: `com.tngtech.archunit:archunit-junit5`, `org.springdoc:springdoc-openapi-starter-webmvc-api`
- `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`, pitest `targetClasses`, `jacocoTestCoverageVerification` (ratchet 임계값), `ArchitectureTest.java` 스켈레톤
- 후속 spec: `.specs/<date>-wire-gradle-harness/` — 이 문서를 01-spec 입력으로

---

## settings.json — hooks 블록

```json
"hooks": {
  "PreToolUse": [
    { "matcher": "Edit|Write", "command": ".claude/hooks/block-impl-without-failing-test.sh" },
    { "matcher": "Edit|Write", "command": ".claude/hooks/enforce-files-in-scope.sh" },
    { "matcher": "Edit|Write", "command": ".claude/hooks/block-progress-on-open-questions.sh" },
    { "matcher": "Bash",       "command": ".claude/hooks/forbid-skip-flags.sh" }
  ],
  "UserPromptSubmit": [
    { "command": ".claude/hooks/route-natural-language-aliases.sh" }
  ]
}
```
`forbid-skip-flags.sh` 차단 패턴 (Gradle): `-x test`, `-x check`, `-x pitest`, `--exclude-task`, `-Dpitest.skip`, `-Dskip.tests`, `--no-verify`.
`permissions.deny`: `Bash(git commit *)`, `Bash(git push *)`, `Bash(./gradlew * -x *)`, `Bash(rm -rf *)`.
**전제**: hook은 bash + `jq` 필요 → `README.md` 프리레퀴짓에 명시.

---

## CLAUDE.md / MEMORY.md 갱신

**CLAUDE.md**:
- "Conventions & Rules" 표 → `.claude/README.md` 방법론 + `.specs/` 포인터로 교체
- "Project Status / 진행 중" 백로그 참조 → `.specs/README.md`
- "File Structure"의 `.claude/` 트리 갱신
- 신규 절: "Spec-Driven Workflow" — `/onboard → /spec → … → /review` 요약
- Quick Start, Known Gotchas, Key API Endpoints는 유지

**MEMORY.md** (`/Users/lemon/.claude/projects/-Users-lemon-Devel-apps-keycloak-user-storage/memory/`):
- "폴더 구조" 항목: `.claude/backlogs/` → `.specs/`, `.claude/plans/` → `.specs/<feature>/03-design.md` 로 수정
- 신규 pointer 파일 1개: `spec-driven-workflow.md` (methodology 요약 + `[[commit_messages]]` 링크) → `MEMORY.md` 인덱스에 1줄 추가

---

## 검증 (구현 후)

1. **Hook 스모크** — 표준입력 JSON으로 각 hook 단독 실행:
   - `block-impl-without-failing-test.sh`: `.tdd-state.json` 없이 `src/main/**` Write → exit 2
   - `block-progress-on-open-questions.sh`: 활성 `01-spec.md`에 `Q-` 라인 있을 때 `src/main/**` Write → exit 2
   - `forbid-skip-flags.sh`: `./gradlew build -x test` → exit 2
   - `route-natural-language-aliases.sh`: "스펙 리뷰해줘" → `/spec-review` 힌트 emit
2. **harness.sh** — `bash .claude/scripts/harness.sh` exit 0, `build/harness-summary.json`에 `unit`/`coverage` = pass, 나머지 = skipped
3. **회귀 없음** — `./gradlew test` 여전히 green (build.gradle 무변경)
4. **`/onboard` 드라이런** — `.specs/_onboarding.md`, `_stack.json`, `_baseline.json` 생성 확인
5. **엔드투엔드 드라이런** — 작은 범위 항목 1개를 `/spec` → `/spec-review` → `/plan` → `/build T-001` → `/validate` 로 통과시켜 산출물 체인 + hook 게이트 동작 확인 (별도 후속 세션 권장)

---

## 규모 / 영향

- 신규 파일 약 55–65개 (command 9, agent 5, skill ~18, template 9, checklist 4, hook 5, script 4, docs 3, README)
- 대부분 Loiane 원문 + 기존 `conventions/`·`rules/` 이식·번역 — **프로덕션 코드 0 변경**
- 삭제: `.claude/{backlogs,conventions,plans,rules}/` (내용은 skill/docs/`.specs/README.md`로 이관)
- 후속 spec 1건: `wire-gradle-harness` (build.gradle 실제 배선 + 플러그인 저장소 확인)

---

## 대안 (참고 — 채택 안 함)

- **B. 풀 포트(백엔드)** — agent 7 / skill ~15 / command 11 + `wire-harness`·`ship`·`epic-plan`. 이 단일 모듈 서비스엔 과함. Lean이 부분집합이라 나중에 증분 추가 가능.
- **C. GitHub Spec Kit** — `/specify /plan /tasks /implement` + `memory/constitution.md`. 툴 중립·GitHub 관리. 단 mutation/archunit/coverage harness·TDD-state 강제가 없어 별도 구축 필요.
- **D. Harness+hook만** — `rules/`·`backlogs/` 유지, harness.sh + TDD/skip hook만 추가. 최소 변경이나 "스펙 중심 산출물 체인"이라는 목표 미충족.
