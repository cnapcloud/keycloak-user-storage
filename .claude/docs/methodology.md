# Methodology — Spec-Driven Development for keycloak-user-storage

Loiane Groner의 [specs-driven-development-spring-angular](https://github.com/loiane/specs-driven-development-spring-angular)
방법론을 이 백엔드 서비스(Gradle · Java 17 · Spring Boot 3.3.4 · H2)에 맞게 이식한 것.
UI/프론트엔드 영역은 제외.

## 핵심 원칙

1. **Spec 먼저.** 모든 기능은 `.specs/<feature-id>/` 아래 번호가 매겨진 산출물 체인을 거친다.
2. **No invention.** 요청·대화·코드에 없는 값(필드명, 기본값, 에러 시맨틱, SLA, 상태코드…)은
   임의로 정하지 않는다. `Q-NNN`으로 기록하고 사용자에게 묻는다.
3. **Phase-exit 게이트.** 미해결 `Q-NNN`이 있으면 다음 단계로 못 넘어간다 (`block-progress-on-open-questions` hook).
4. **TDD by construction.** `/build`는 실패하는 테스트를 먼저 쓰고
   `.tdd-state.json`에 `red_failure_excerpt`를 기록해야 `src/main/**` 수정이 허용된다
   (`block-impl-without-failing-test` hook).
5. **Self-validating harness.** `/validate`는 `.claude/scripts/harness.sh`가 만든 리포트를 읽고
   결정론적 verdict를 낸다. 추측 금지.
6. **커밋은 사람이.** 어떤 agent도 자동으로 `git commit` 하지 않는다. 사용자가 명시적으로 요청하면 그때만 수행한다 (`git push`는 여전히 금지).

## 7단계 (+ Phase 0)

```
/onboard → /spec → /spec-review → /plan → (/build T-NNN → commit) ×N → /validate → /review → commit
  (0)       (1)        (2)          (3)     (4, 태스크마다 TDD + 커밋)      (5)         (6)
```

| Phase | Command | Owner agent | 산출물 |
|---|---|---|---|
| 0 bootstrap | `/onboard` | `architect` (onboarding hat) | `.specs/_stack.json`, `_baseline.json`, `_onboarding.md` |
| 1 specify | `/spec` | `spec-author` (author hat) | `01-spec.md` |
| 2 review | `/spec-review` | `spec-author` (review hat) | `02-spec-review.md` |
| 3 plan | `/plan` | `architect` (design hat) | `03-design.md`, `04-tasks.md`, `adr/`, `.tdd-state.json` |
| 4 build | `/build T-NNN` | `test-engineer` (red) + `implementer` (green/refactor/simplify) | code + tests, `05-implementation-log.md` |
| 5 validate | `/validate` | `validator` | `07-validation-report.md`, `07a-traceability.md` |
| 6 review | `/review` | `validator` (review hat) | `08-code-review.md` |

(`06-test-plan.md`은 이 lean 편성에서 생략 — 번호는 호환을 위해 비워둠. 커버리지/traceability 갭은 별도 문서 없이 처리한다: `/validate`가 `07-validation-report.md`에 `Gap-NNN`으로 식별 → `/plan`이 이를 덮는 gap task를 `04-tasks.md`에 추가 → `/build`가 정규 red→green 루프로 닫는다. 대응 AC가 없는 orphan 라인은 `Q-NNN`으로 스펙에 반송.)

**command와 agent의 분리.** `commands/*.md`는 얇다 — frontmatter(`agent:` + 두 hat agent는 `hat:`) + `## Purpose / Inputs / Outputs / Next` 섹션 + owning agent 포인터만.
절차 · 거부조건 · 완료조건 · `<feature-id>` 생략 시 해석은 owning agent(`agents/*.md`)에 있고, agent는 커맨드 없이 직접 호출돼도 자립 실행된다.
`spec-author` · `architect` · `validator`는 각각 두 hat을 담고 커맨드가 hat을 지정한다.
`/build`만 예외 — 두 agent를 지휘하므로 메인 세션이 오케스트레이션하고 `commands/build.md`가 그 순서를 담는다.
`/status` · `/help`는 owning agent 없이 자체 완결.

## `.specs/` 레이아웃

```
.specs/
├── README.md            # 기능 인덱스 + 완료 이력(BL-01~10)
├── _onboarding.md        _stack.json        _baseline.json
└── YYYY-MM-DD-<feature-id>/
    ├── 01-spec.md   02-spec-review.md   03-design.md   04-tasks.md
    ├── 05-implementation-log.md   07-validation-report.md   07a-traceability.md   08-code-review.md
    ├── .tdd-state.json
    └── adr/ADR-NNN-*.md
```

## `.tdd-state.json`

`/plan`이 생성, `/build`가 phase를 전이시킨다. `phase`: `pending → red → green → refactor → simplify → done`.
`active_task`는 태스크 사이에서 `null`. hook 3종이 이 파일을 읽어 `src/main/**` 편집을 강제한다.

## 브라운필드 기준선 (`_baseline.json`) 관리

`/onboard`가 캡처한 `_baseline.json`은 `/validate`의 **허용 바닥**이다. `/validate`는 이 파일 대비
**회귀만** 차단한다 — baseline이 `unit: fail (3)`이면 실패 3건까지는 통과 (신규/변경 라인은 항상 풀 기준).

### `/validate` 최종 판정

`/validate`는 콘솔 출력이 아니라 **`build/harness-summary.json`**(harness가 리포트를 파싱해 만든
게이트별 `status`)을 읽고, 각 게이트를 `_baseline.json`의 기록값과 대조한다.
gradle exit code(`build_exit`)는 기록만 되고 판정에 쓰이지 않는다.

| 조건 | verdict |
|---|---|
| 모든 활성 게이트 `pass` + baseline 대비 회귀 없음 | **PASS** |
| 활성 게이트는 pass지만 절대 목표(예: 커버리지 90%) 미달 + 회귀 없음 + 모든 waiver가 ADR 참조 | **WARN** |
| 활성 게이트 중 `fail` 하나라도 · baseline 대비 회귀 · 테스트 0개 AC | **FAIL** |

- `skipped`(플러그인 미배선) = pass도 fail도 아님. `_stack.json`이 active로 표시했는데 리포트 없음 = **error**.
- "회귀" = 그 게이트가 `_baseline.json`의 값보다 나빠짐 (unit 실패 수 증가, 커버리지 하락 등).
- 그래서 baseline을 안 조이면 (아래 "재캡처를 안 하면") 새 실패도 "회귀 아님"으로 흡수된다.
- 상세 규칙: `.claude/skills/harness-report-parsing/SKILL.md`.

### baseline에 실패 게이트가 있을 때

`_onboarding.md`에 캡처된 선행 실패(예: `unit: fail`)는 방치하지 않고 **정식 feature로** 해소한다:

1. `.specs/README.md` "진행 중 / 예정"에 행 추가 → `/spec "<실패 설명>"`.
2. 정규 라이프사이클 완주: `/spec-review → /plan → /build T-NNN → /validate → /review`.
3. **모든 태스크·리뷰가 끝나 머지 가능한 상태가 되면** `harness.sh --baseline`를 재실행해
   `_baseline.json`을 새 결과로 다시 캡처한다 (**ratchet down** — 허용 실패 수가 줄고, 이후 되돌릴 수 없음).
   - feature 커밋과 **분리된 커밋**으로 한다. spec/ADR에서 `_baseline.json` 편집은 Non-Goal.
   - 브랜치에서 떠서 함께 머지하거나, 머지 후 `main`에서 떠도 된다 (feature 커밋을 순수하게 유지하려는 관례일 뿐 필수 아님).

### 재캡처(ratchet)를 안 하면

- **게이트가 느슨한 채 남는다.** baseline이 여전히 `unit: fail (3)`이면, fix 이후 누군가 테스트를
  1~2건 깨뜨려도 `/validate`가 "baseline보다 나쁘지 않음"으로 **통과시킨다.** 방금 고친 것이 조용히 되돌아간다.
- **`/validate` 리포트가 노이즈가 된다.** "baseline delta"가 매번 "fail → pass, better"로 떠서
  진짜 회귀와 구분이 흐려진다.
- **`_onboarding.md`의 "선행 실패" 목록이 거짓이 된다.** 이미 해소된 항목을 미해결로 안내한다.
- coverage·mutation 등 다른 레이어를 나중에 배선할 때, 낡은 baseline 위에서 새 기준선이 잘못 잡힌다.

## 강제 장치 (hooks — `.claude/settings.json`)

| Hook | 이벤트 | 차단 조건 |
|---|---|---|
| `block-impl-without-failing-test.sh` | Edit/Write | 활성 `.tdd-state.json` 없이 / phase≠red계열 / `red_failure_excerpt` 비어있음 / scope 밖 파일 → `src/main/**` 수정 차단 |
| `enforce-files-in-scope.sh` | Edit/Write | 활성 태스크 진행 중 `src/**` 편집이 `files_in_scope` 밖 |
| `block-progress-on-open-questions.sh` | Edit/Write | 활성 feature의 `01/03/04` 문서에 미해결 `Q-NNN` 있는데 `src/**` 편집 |
| `forbid-skip-flags.sh` | Bash | `./gradlew -x test`, `-Dpitest.skip`, `--no-verify` 등 |
| `route-natural-language-aliases.sh` | UserPromptSubmit | (차단 아님) "스펙 짜줘" → `/spec` 힌트 |

## 이 프로젝트 고유 규칙 (다른 Spring 프로젝트 의견을 들여오지 말 것)

- `ResponseEntity` 반환은 이 프로젝트에서 정상. Loiane의 "avoid ResponseEntity" 규칙은 적용 안 함.
- 에러: `ResponseStatusException`, DELETE/PATCH 성공은 204, 중복은 409, 봉투 `{"error": "..."}`.
- `User.attributes`는 절대 null 아님 (`new HashMap<>()`), `FetchType.EAGER`.
- 날짜: `LocalDateTime` + `CustomLocalDateTimeSerializer` (`yyyy-MM-dd'T'HH:mm:ss`).
- 동적 쿼리: Criteria API (`UserRepositoryImpl`).
- H2 in-memory, `ddl-auto=update` — 마이그레이션 파일 없음.
