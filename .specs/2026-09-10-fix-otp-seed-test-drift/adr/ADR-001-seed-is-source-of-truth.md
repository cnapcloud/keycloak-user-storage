# ADR-001: 시드 데이터를 source-of-truth로 두고 테스트 단언을 정합한다

- Status: accepted
- Date: 2026-09-10
- Feature: 2026-09-10-fix-otp-seed-test-drift

## Context
`_baseline.json`의 `unit` 게이트가 실패 3건으로 캡처돼 있다. 원인은 커밋 `8bdd8d6`
"Change test user otpMethod from SKIP to SMS" 가 `src/main/resources/user-data.json` 의 두
시드 사용자(`john`, `jane`)를 `otpMethod=SKIP` → `SMS` 로 바꿨으나 `UserStorageIntegrationTest`
의 단언(시드 16명 전원 `SKIP` 가정)을 갱신하지 않은 것.

프로덕션 코드(`/user/search`, `/user/count`)는 시드 대비 정확히 동작한다. 결함은 테스트 단언 쪽뿐.

## Decision
시드 데이터(`SKIP` 14 / `SMS` 2 = `john`·`jane`)를 정답으로 간주하고, 어긋난 테스트 단언
3건과 그것들을 오도하는 테스트 이름을 시드에 맞게 고친다. `user-data.json` 은 건드리지 않는다.

- `otpMethod=SKIP` 검색/카운트 기대값 16 → 14.
- 필드+속성 교집합 테스트를 `?username=john&otpMethod=SMS` → `john` 으로 재작성 (`john` 의 현재
  시드 속성이 `SMS`).
- 스테일 메서드명(`count_..._returns17`, `..._returnsAllUsers`)을 실제와 일치하게 rename.

## Alternatives considered
- **`user-data.json` 의 `SMS` 2건을 `SKIP` 로 되돌린다 (커밋 `8bdd8d6` revert)** — 커밋 메시지가
  변경을 의도로 명시하고, 사용자 요청도 "단언 미갱신"으로 표현(시드가 아니라 테스트가 뒤처짐).
  의도된 데이터 변경을 되돌리는 것은 회귀. 채택 안 함.
- **실패 3건을 `@Disabled` 로 격리** — drift를 숨길 뿐 해소 안 됨. 하드 밴 대상. 채택 안 함.

## Consequences
- Positive: `unit` 게이트가 실패 0으로 내려가 `_baseline.json` ratchet down 가능. 시드에
  `otpMethod` 다양성(`SMS`)이 유지되어 향후 OTP 경로 테스트에 활용 가능.
- Negative / follow-up: fix 후 `harness.sh --baseline` (또는 `/onboard`) 재실행으로
  `_baseline.json` 을 사람이 갱신해야 함 — 이 스펙 범위 밖 (Non-Goal).
- Gates affected: `_baseline.json` `unit` 기준선을 fail → pass 로 ratchet down (커버리지 waiver 없음,
  새 ArchUnit 규칙 없음).
