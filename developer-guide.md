# Developer Guide — spec-driven harness 사용법

이 repo에서 기능 하나를 개발하는 전체 과정. 예시로 **"사용자 주소 관리"** 기능을 처음부터 끝까지 따라간다.

---

## 개발자가 하는 일은 3가지뿐

1. 명령을 순서대로 입력한다: `/spec` → `/spec-review` → `/plan` → `/build T-NNN` (태스크 수만큼) → `/validate` → `/review`
2. 명령이 멈추고 `Q-NNN`을 물으면 답한다.
3. `/build` 태스크 사이마다 `git commit` 한다. (agent는 자동 커밋 안 함 — 요청하면 대신 해준다)

나머지(hook, skill, agent, template)는 명령 안에서 자동으로 돌아간다. 직접 부르지 않는다.

`/onboard`는 repo당 1회. 이미 실행됨 (`.specs/_stack.json`, `_baseline.json`, `_onboarding.md` 존재).

---

## 산출물 흐름

각 명령이 `.specs/<날짜>-<기능이름>/` 아래에 번호 매긴 파일을 만든다.

```
/spec          → 01-spec.md            요구사항 (EARS 형식 AC-NNN + 미확정 Q-NNN)
/spec-review   → 02-spec-review.md     스펙 자체 감사, PASS/FAIL
/plan          → 03-design.md          API 계약 + 레이어 설계 + ADR
                 04-tasks.md           1~4시간짜리 태스크 T-001, T-002 …
                 .tdd-state.json       TDD 상태 머신
/build T-NNN   → 05-implementation-log.md   태스크별 red→green→refactor→simplify 기록
                 + 실제 코드 + 테스트
/validate      → 07-validation-report.md    harness 결과 + 기준선 대비 판정
                 07a-traceability.md         AC ↔ 테스트 ↔ 코드 매핑
/review        → 08-code-review.md      커밋 전 자가 리뷰
```

---

## 워크스루: 주소 관리 기능

### 1. `/spec "사용자 주소 관리 기능 추가"`

`spec-author` agent가 `.specs/2026-09-08-user-address/01-spec.md` 초안을 만든다.
요청/코드에 없는 값은 지어내지 않고 `## Open Questions`에 남긴다:

```
## Open Questions

- Q-001: 주소는 USP 필수 엔드포인트가 아니다. Keycloak이 이 데이터를 호출하나,
         아니면 관리 목적의 자체 API인가?
  - Why it matters: 스펙 외 엔드포인트는 이 프로젝트에서 제거 대상 (BL-09 선례)
  - Status: open

- Q-002: 사용자당 주소 1개인가, 여러 개(집/직장 등)인가?
  - Status: open

- Q-003: 저장 위치 — 새 USER_ADDRESS 테이블인가, 기존 attributes 맵에 문자열로 넣나?
  - Status: open

- Q-004: 필수 필드 목록과 우편번호/국가코드 포맷 검증 규칙은?
  - Status: open
```

**당신이 하는 것** — 각 질문에 답한다. 예:

> Q-001: 자체 관리 API다. Keycloak은 호출 안 함.
> Q-002: 여러 개. 각 주소에 label(집/직장) 붙음.
> Q-003: 새 USER_ADDRESS 테이블. attributes는 key=value 한 쌍이라 구조적 데이터에 안 맞음.
> Q-004: 필수 = label, line1, city, postalCode, country. postalCode는 문자열 그대로 저장(검증 안 함). country는 ISO 3166-1 alpha-2.

agent가 답을 `## Resolved Questions`에 기록하고 `Status: resolved`로 바꾼다. 그리고 AC를 확정한다:

```
- AC-001: When 클라이언트가 존재하는 사용자 id로 POST /user/{id}/addresses 를 유효한 본문과 함께 호출하면,
          the system shall 주소를 저장하고 201과 생성된 주소 id를 반환한다.
- AC-002: If 사용자 id가 없으면, then the system shall 404와 {"error": "user not found"} 를 반환한다.
- AC-003: If 필수 필드가 누락되면, then the system shall 400과 {"error": "..."} 를 반환한다.
- AC-004: When GET /user/{id}/addresses 를 호출하면, the system shall 해당 사용자의 주소 배열을 반환한다.
- AC-005: When DELETE /user/{id}/addresses/{addressId} 를 호출하면, the system shall 204를 반환한다.
- AC-006: When 사용자가 삭제되면, the system shall 그 사용자의 모든 주소도 함께 삭제한다.
```

> 미해결 `Q-NNN`이 하나라도 남아 있으면 `src/**` 편집이 hook에 막힌다. 다음 단계로 못 감.

### 2. `/spec-review`

같은 agent가 체크리스트(`.claude/checklists/spec-review.md`)로 스펙을 감사 → `02-spec-review.md`에 `PASS` 또는 `FAIL` + 사유.
FAIL이면 `/spec --continue`로 고친 뒤 다시 리뷰. PASS면 다음.

### 3. `/plan`

`architect` agent가 3개를 만든다.

`03-design.md` — API 계약(OpenAPI 스케치) + 레이어 설계:

```
POST /user/{id}/addresses
  201 → {"id": "addr-3f2a1c9b", "label": "집", ...}
  404 → {"error": "user not found"}
  400 → {"error": "line1 is required"}

레이어:
  AddressController  → AddressService(iface) → AddressServiceImpl → AddressRepository
  model: Address (PK addr-XXXXXXXX, FK userId, label/line1/line2/city/postalCode/country)
  User 1..* Address — 사용자 삭제 시 cascade
ADR-001: 새 테이블 vs attributes 맵 → 새 테이블 (구조적 데이터, 조회 패턴)
```

`04-tasks.md` — 태스크 분해:

```
| ID    | Title                              | acs_covered      | depends_on |
| T-001 | Address 엔티티 + Repository        | AC-001           | —          |
| T-002 | POST /user/{id}/addresses 생성     | AC-001,002,003   | T-001      |
| T-003 | GET /user/{id}/addresses 조회      | AC-004           | T-002      |
| T-004 | DELETE + 사용자 삭제 cascade       | AC-005,006       | T-002      |
```

각 태스크에 `files_in_scope`(건드려도 되는 파일 화이트리스트)와 `gates`가 붙는다.
`.tdd-state.json`도 이때 생성된다.

**당신이 하는 것** — 설계와 태스크 분해를 검토. 이상하면 지적, 괜찮으면 승인.

### 4. `/build T-001` (태스크마다 반복)

`git status`가 깨끗해야 시작된다(이전 태스크 커밋 필수). 그다음 4단계가 강제된다:

1. **red** — `test-engineer`가 실패하는 테스트를 먼저 쓴다. `@Tag("AC-001")` 붙임. 실행해서 빨간 것 확인 → 실패 로그를 `.tdd-state.json`에 기록.
   (이 기록이 없으면 `src/main/**` 편집이 hook에 막힌다)
2. **green** — `implementer`가 테스트를 통과시킬 **최소 코드**만 쓴다. `files_in_scope` 밖 파일 건드리면 막힌다.
3. **refactor** — 동작 유지하며 구조 정리. 테스트 재실행.
4. **simplify** — 삼항 풀기, early return, 스펙 용어로 이름짓기.

끝나면 `.tdd-state.json` phase=done, 커밋 메시지를 제안하고 **멈춘다**.

**당신이 하는 것:**

```bash
git add -A && git commit      # agent가 안 하므로 직접
/build T-002                   # 다음 태스크
```

T-002 → 커밋 → T-003 → 커밋 → T-004 → 커밋.

### 5. `/validate`

`validator` agent가 `.claude/scripts/harness.sh`를 돌리고 리포트를 **읽어서** 판정한다.

```
07-validation-report.md:
  unit:     pass (신규 12 테스트 통과, 기준선 실패 3건은 변동 없음 → 회귀 아님)
  coverage: 신규 라인 96% (기준 95%) → pass
  verdict:  PASS

07a-traceability.md:
  AC-001 → T-002 → AddressControllerTest.create_validBody_returns201 → AddressController.java:24 → unit covered
  AC-006 → T-004 → ...
```

기준선(`.specs/_baseline.json`)의 선행 실패 3건은 통과시키고, **새로 생긴 실패만** 막는다.
FAIL이면 원인 태스크로 돌아가 `/build`로 고친다.

### 6. `/review`

diff를 루브릭 9개 항목(레이어 경계, 에러 포맷, 로깅, attributes null, 이모지 없음 등)으로 대조 → `08-code-review.md`.
지적사항 있으면 고치고 다시.

### 7. 커밋 / PR

**개발자가 직접.** agent는 자동 커밋하지 않지만, 명시적으로 요청하면 `git commit`은 해준다. `git push`는 권한 deny로 막혀 있다.

```bash
git push -u origin feature/user-address
gh pr create
```

`.specs/README.md`의 "진행 중" 행을 "완료 이력"으로 옮긴다.

---

## hook이 막을 때 (자주 겪는 경우)

| 증상 | 원인 | 해결 |
|---|---|---|
| `BLOCKED: unresolved Open Questions` | `01-spec.md`에 `Status: open`인 `Q-NNN` 남음 | 답을 받아 `Status: resolved`로 바꾼다 |
| `BLOCKED: no .tdd-state.json` | `/plan` 안 하고 `/build` 시도 | `/plan` 먼저 |
| `BLOCKED: phase is 'pending'` / `red_failure_excerpt is empty` | 실패 테스트 없이 `src/main` 편집 | red 단계부터. 실패하는 테스트 먼저 쓰고 실행 |
| `BLOCKED: not in files_in_scope` | 태스크 범위 밖 파일 편집 | `04-tasks.md` + `.tdd-state.json`의 `files_in_scope` 넓히고 `/plan` 재실행, 아니면 별도 태스크로 |
| `BLOCKED: '-x test' skips a harness layer` | `./gradlew build -x test` 류 실행 | skip 없이 `./gradlew test` 또는 `.claude/scripts/harness.sh` |
| `/build` 거부 (uncommitted changes) | 이전 태스크 커밋 안 함 | `git commit` 먼저 |

---

## 참조

- 워크플로우 진입점·hook 목록: `.claude/README.md`
- 방법론 상세: `.claude/docs/methodology.md`
- 명령 진입점: `.claude/commands/<name>.md` (얇음) — 절차·거부조건·완료조건은 `.claude/agents/<name>.md`
- 현재 기능 현황: `.specs/README.md` · `/status`
