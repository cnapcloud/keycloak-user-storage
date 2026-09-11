# Developer Guide — spec-driven harness 사용법

이 repo에서 기능 하나를 개발하는 전체 과정. 예시로 **"사용자 주소 관리"** 기능을 처음부터 끝까지 따라간다.

---

## 처음 시작할 때: `/onboard`

repo당 **1회만** 실행하는 command. `.specs/_stack.json`(스택 분류) · `_baseline.json`(harness
기준선) · `_onboarding.md`(분류 결과 + 권장 첫 `/spec`)를 만든다. 이 세 파일이 이미 있으면
(이 repo처럼) onboard는 끝난 것 — 다시 실행할 필요 없다.

`/onboard`는 `src/main/java` · `src/test/java`에 있는 파일을 보고 repo를 둘 중 하나로 분류한다
(`.claude/agents/architect.md` 참조):

- **Greenfield** — 코드가 없거나 거의 없는 새 프로젝트. 기준선에 잡을 "기존 실패"가 없다.
- **Brownfield** — 이미 동작하는 코드베이스.

> **이 repo는 이미 Brownfield로 onboard 완료됨** (`.specs/_onboarding.md`)

**Greenfield repo라면 `/onboard` 전에 순서가 있다** — `/onboard`는 스캐폴딩도 harness 레이어
설정도 만들어주지 않고, 그 시점 `build.gradle`/소스 상태를 스캔·기록만 하기 때문:

1. **프로젝트 스캐폴딩** — Spring Initializr로 먼저 생성한다.
2. **harness layer 구성** — `.claude/docs/harness-gradle.md`를 참조해 checkstyle·spotbugs·
   archunit·mutation·openapi·owasp 중 쓸 레이어를 `build.gradle`에 이때 넣는다.
3. **그다음 `/onboard`** 실행.

> onboard 이후 레이어를 추가/변경했다면 `.claude/scripts/harness.sh --baseline`를 실행한다.

---

## 개발자가 하는 일은 3가지뿐

1. 명령을 순서대로 입력한다: `/spec` → `/spec-review` → `/plan` → `/build T-NNN` (태스크 수만큼) → `/validate` → `/review`
2. 명령이 멈추고 `Q-NNN`을 물으면 답한다.
3. `/build` 태스크 사이마다 `git commit` 한다. (agent는 자동 커밋 안 함 — 요청하면 대신 해준다)

나머지(hook, skill, agent, template)는 명령 안에서 자동으로 돌아간다. 직접 부르지 않는다.

---

## 산출물 흐름

각 명령이 `.specs/<날짜>-<기능이름>/` 아래에 번호 매긴 파일을 만든다.

```
/spec          → 01-spec.md            요구사항 (EARS 형식 AC-NNN + 미확정 Q-NNN)
                 .specs/README.md       "진행 중 (spec)" 행 추가
/spec-review   → 02-spec-review.md     스펙 자체 감사, PASS/FAIL
/plan          → 03-design.md          API 계약 + 레이어 설계
                 04-tasks.md           1~4시간짜리 태스크 T-001, T-002 …
                 adr/ADR-NNN-*.md      대안이 있던 결정마다
                 .tdd-state.json       TDD 상태 머신
/build T-NNN   → 05-implementation-log.md   태스크별 red→green→refactor→simplify 기록
                 + 실제 코드 + 테스트
/validate      → 07-validation-report.md    harness 결과 + 기준선 대비 판정
                 07a-traceability.md         AC ↔ 테스트 ↔ 코드 매핑
/review        → 08-code-review.md      커밋 전 자가 리뷰
                 .specs/README.md       Approve 시 "완료 이력"으로 이동
```

---

## 워크스루: 주소 관리 기능 (`.specs/2026-09-11-address-management/` 실제 완료 사례)

### 1. `/spec "주소 관리 기능"`

`spec-author` agent가 `01-spec.md` 초안을 만들고, 값이 정해지지 않은 부분은 `Q-001`~`Q-008`로
남긴다(엔드포인트 nest 여부, 단건/목록 조회 제공 범위, id 포맷, 필드별 검증 규칙, 404 처리,
cascade 삭제 여부, 응답 필드, 개수 상한 등).

**당신이 하는 것** — 각 질문에 답한다. 이번엔 "예시이니 일반적인 상황을 가정해서 답을 채워라"고
지시했고, agent가 그 지시에 따라 통상적인 REST 기본값(User 하위 nest, `addr-XXXXXXXX` id,
우편번호 5자리/도로명주소 필수, 사용자 삭제 시 cascade, 개수 무제한 등)으로 `## Resolved
Questions`를 채우고 AC를 확정했다 — 최종 `01-spec.md`에는 이 답이 "티켓 값"이 아니라 "합리적
기본값"이라는 표시가 남는다. 실제 값을 알고 있다면 이렇게 위임하지 말고 직접 답하는 편이 낫다.

결과: **AC-001~AC-015** (15개). 미해결 `Q-NNN`이 남아 있으면 `src/**` 편집이 hook에 막힌다.

### 2. `/spec-review`

체크리스트로 스펙을 감사한다. 이번 라운드에서 실제로 지적된 것: AC-005와 AC-010이
"조건 A 또는 B → 같은 결과"로 묶여 있어 atomic 규칙(항목 5) 위반 — 각각 AC-013(도로명주소
공백), AC-014(소유자 불일치)로 쪼개서 재작성했다. `/plan` 진행 중 발견된 목록 조회 404 누락도
`AC-015`로 별도 보완. → `02-spec-review.md`: **PASS** (acs_total 15, open_questions 0).

FAIL이었다면 `/spec --continue`로 고친 뒤 다시 리뷰.

### 3. `/plan`

`architect` agent가 `03-design.md` + `04-tasks.md` + ADR 3개 + `.tdd-state.json`을 만든다.

- 레이어: `AddressController → AddressService(iface)/Impl → AddressRepository → Address`.
  이 repo 최초의 "자체 PK를 갖는 1:N 자식 엔티티"라 설계 결정마다 대안이 있었고, 그래서
  ADR도 3개나 나왔다 — **ADR-001** JPA `@OneToMany` 대신 평범한 `userId` 컬럼 + 파생 쿼리
  (이유: 코드베이스에 JPA 연관관계 전례가 전무), **ADR-002** id 포맷 `addr-XXXXXXXX`를
  `User`와 동일하게 서비스 레이어에서 수동 생성, **ADR-003** Bean Validation 대신 수동
  `if` 검증(이유: 이 repo에 `@NotBlank`류 전례가 전무).
- 태스크 7개(`T-001`~`T-007`), 각각 `files_in_scope`로 편집 가능 파일이 화이트리스트로 고정됨.

**당신이 하는 것** — 설계와 ADR을 검토. 대안이 왜 기각됐는지(주로 "이 코드베이스에 전례 없음")가
근거로 붙어 있으니 그게 납득되는지 확인.

### 4. `/build T-001` ~ `/build T-007`

태스크마다 red(실패 테스트) → green(최소 구현) → refactor → simplify. 끝나면 커밋 메시지를
제안하고 멈춘다 — `git add -A && git commit` 후 다음 태스크로.

**실제로 걸렸던 부분**: `traceability.sh`는 테스트의 `@Tag("AC-NNN")`만 보고 `@DisplayName`
문구는 보지 않는다. T-001의 "무제한 등록" 테스트(AC-012)에 `@Tag("AC-001")`만 붙이고
`@Tag("AC-012")`를 빠뜨렸는데, 태그를 안 봐도 테스트 자체는 통과하기 때문에 `/build` 단계에서는
안 걸리고 `/validate`의 traceability 매트릭스에서만 "AC-012 미커버(`Gap-001`)"로 드러났다.
→ 태그 하나 추가하는 것만으로 해소(아래 5, 6 참조). **AC를 검증하는 로직이 있어도 태그를
빠뜨리면 harness는 못 잡아준다** — `/build`에서 태그를 정확히 다는 게 핵심.

### 5. `/validate`

`.claude/scripts/harness.sh` 실행 후 리포트를 읽어 판정.

```
07-validation-report.md:
  unit: pass — 71 tests, 0 failures (baseline 39t/0f 대비 회귀 없음, +32 신규 전부 green)
  coverage / archunit / checkstyle / spotbugs / mutation: skipped (레이어 미배선, baseline과 동일)
  verdict: PASS

07a-traceability.md: AC-001~015 15/15 covered (Gap-001은 이 라운드에서 해소)
```

`/validate`는 baseline 대비 **회귀만** 막는다 — 이 4개 레이어가 skipped인 것도 baseline과
동일해서 문제 삼지 않는다. FAIL이면 원인 태스크로 돌아가 `/build`.

### 6. `/review`

diff를 루브릭 9개 항목으로 대조 → `08-code-review.md`. 실제 결과: **should-fix 1건**
(F-001 — 위 AC-012 태그 수정이 작업 트리에는 있지만 아직 커밋 안 됨, "이 파일도 커밋에
포함하라"는 지적), nit 2건(field injection 하나가 새 클래스 관례와 다름, 컨트롤러에 불필요한
`@Transactional` — 둘 다 기존 관례를 그대로 옮긴 것이라 must-fix 아님), praise 2건(레이어 분리,
ADR 3개 다 남긴 것). **must-fix 0 → Approve.**

Approve되면 `.specs/README.md`의 이 feature 행이 자동으로 "완료 이력"으로 옮겨진다.

### 7. 커밋 / PR

**개발자가 직접.** agent는 자동 커밋하지 않지만 명시적으로 요청하면 `git commit`은 해준다.
`git push`는 권한 deny로 막혀 있다.

```bash
git push -u origin spec/2026-09-11-address-management
gh pr create
```

`.specs/README.md` 완료 처리는 `/review` Approve가 이미 했다 — PR만 올리면 된다.

---

## baseline에 실패가 있으면

`_onboarding.md`에 캡처된 선행 실패(예: `unit: fail`)는 방치하면 게이트가 느슨한 채 남는다.
그 상태로는 fix 이후 누가 테스트를 깨도 `/validate`가 "baseline보다 나쁘지 않음"으로 통과시킨다.

해소 절차:

1. 그 실패를 하나의 feature로 잡는다 — `.specs/README.md`에 행 추가 → `/spec "<실패 설명>"`.
2. 정규 라이프사이클 완주 (`/spec-review → /plan → /build → /validate → /review`).
3. 머지 가능 상태가 되면 **`.claude/scripts/harness.sh --baseline`** 재실행 → `_baseline.json`을
   새 결과로 다시 캡처 (**ratchet down**: 허용 실패 수가 줄고 이후 되돌릴 수 없음).
   feature 커밋과 **분리된 커밋**으로.
4. `_onboarding.md`의 "선행 실패" / "기준선 게이트" 표도 손으로 최신화한다.

상세·재캡처를 안 할 때의 문제 전체: `.claude/docs/methodology.md` → "브라운필드 기준선 (`_baseline.json`) 관리".

---

## hook이 막을 때 (자주 겪는 경우)

| 증상 | 원인 | 해결 |
|---|---|---|
| `BLOCKED: unresolved Open Questions` | `01/03/04` 문서의 `## Open Questions` 섹션에 `Q-NNN`이 남음 (`Status: open` 또는 Status 줄 없음) | 답을 받아 그 항목을 `## Resolved Questions`로 옮긴다 (또는 근거와 함께 `Status: deferred`) |
| `BLOCKED: no .tdd-state.json` | `/plan` 안 하고 `/build` 시도 | `/plan` 먼저 |
| `BLOCKED: phase is 'pending'` / `red_failure_excerpt is empty` | 실패 테스트 없이 `src/main` 편집 | red 단계부터. 실패하는 테스트 먼저 쓰고 실행 |
| `BLOCKED: not in files_in_scope` | 태스크 범위 밖 파일 편집 | `04-tasks.md` + `.tdd-state.json`의 `files_in_scope` 넓히고 `/plan` 재실행, 아니면 별도 태스크로 |
| `BLOCKED: '-x test' skips a harness layer` | `./gradlew build -x test` 류 실행 | skip 없이 `./gradlew test` 또는 `.claude/scripts/harness.sh` |
| `/build` 거부 (uncommitted changes) | 이전 태스크 커밋 안 함 | `git commit` 먼저 |

---

## 참조

- **방법론 상세**: `.claude/docs/methodology.md`
  - 7단계 + Phase 0, command↔agent 분리, hook 강제 장치 표
  - `.tdd-state.json` 상태 머신 (`pending → red → green → refactor → simplify → done`)
  - **"브라운필드 기준선 (`_baseline.json`) 관리"** — baseline 실패 게이트 해소 · ratchet down · 재캡처 안 할 때의 문제
  - 이 프로젝트 고유 규칙 (`ResponseEntity` 허용, 에러 봉투, `attributes` null 금지, `LocalDateTime`, Criteria API, 이모지 금지)
- 워크플로우 진입점·hook 목록: `.claude/README.md`
- harness 동작·레이어·게이트 추가: `.claude/docs/harness-gradle.md`
- 명령 진입점: `.claude/commands/<name>.md` (얇음, frontmatter `agent:`/`hat:` + I/O 계약) — 절차·거부조건·완료조건은 `.claude/agents/<name>.md`
- skill(코딩 컨벤션·EARS·traceability 등): `.claude/skills/<name>/SKILL.md` — 각 command가 자동 로드
- 현재 기능 현황: `.specs/README.md` · `/status`
