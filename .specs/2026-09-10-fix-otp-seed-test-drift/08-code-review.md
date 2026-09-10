# Code Review: 2026-09-10-fix-otp-seed-test-drift

> Owner: `validator` (review hat) · Phase 6 · Template: `.claude/templates/code-review.template.md`
> Diff: `git diff 462df66..HEAD -- src/ .claude/hooks/` (feature 커밋 `3ac2f1a`, `1805205` + hook `b516699`)
> Validation verdict: PASS

## Final verdict
**Approve** — must-fix 0. 테스트 전용 변경(프로덕션·스키마·계약 무변경), 회귀 0, 7/7 AC 커버.
should-fix 1건은 공개 동작이 아닌 컨벤션 갭.

## Findings
| ID | Severity | File:line | Finding | Suggested change |
|----|----------|-----------|---------|------------------|
| F-001 | should-fix | `UserStorageIntegrationTest.java` (변경/신규 4개 `@Test`) | rubric §7·`tdd-red-green-refactor` skill이 요구하는 `@DisplayName("T-NNN: given…, when…, then…")`가 없음. `@Tag`만 있음 (traceability는 `@Tag`로 충족되므로 추적성 문제는 아님) | 이 4개에 `@DisplayName` 추가하거나, 파일 전체(39개 모두 없음)에 대해 별도 follow-up. 파일 관례가 `@DisplayName` 미사용이라 부분 도입은 불균형 — follow-up 권장 |
| F-002 | nit | `UserStorageIntegrationTest.java` `count_byAttributeKey_otpMethod_skip_returns14` 앞 | 빈 줄 2개 (combined 테스트와 사이). diff에서 `+` 빈 줄 추가됨 | 빈 줄 1개로 |
| F-003 | nit | `UserStorageIntegrationTest.java` `search_byAttributeKey_otpMethod_sms_returnsJohnAndJane` | `u -> (String) u.get("username")` unchecked 캐스트 | 파일 내 다른 캐스트와 동일 스타일이라 수용 가능. 굳이면 `String.valueOf(u.get("username"))` |
| F-004 | praise | `UserStorageIntegrationTest.java` `search_combinedFieldAndAttribute_returnsIntersection` / `..._sms_returnsJohnAndJane` | 교집합 테스트가 `size()==1` **와** `username=="john"` 둘 다 단언 → 단일 필터가 아닌 실제 교집합 검증. SMS 테스트는 count가 아닌 정렬된 `{jane, john}` 정확 집합 단언 | — |
| F-005 | praise | `block-progress-on-open-questions.sh:43-56` | awk를 `## Open Questions` 섹션으로 스코프 제한 — hook 헤더 주석의 원래 의도와 일치. `## Resolved Questions`의 `- Q-NNN` 오탐 제거 | 참고: `## Open Questions` 헤딩이 없는 파일은 이제 스캔되지 않음 (템플릿이 해당 헤딩을 강제하므로 실무상 무해) |

## Rubric section results
| Section | Result | Notes |
|---------|--------|-------|
| 1 Traceability | ok | AC-001~007 전부 `@Tag` 테스트. orphan 코드 없음 (`src/main` 무변경) |
| 2 Layer boundaries | n/a | 프로덕션 코드 변경 없음 |
| 3 Spring idioms | n/a | 〃 |
| 4 Error handling | n/a | 새 에러 경로 없음 |
| 5 Data access | n/a | 리포지토리·엔티티·`attributes` 무변경 |
| 6 Dates & serialisation | n/a | 날짜/직렬화 코드 무변경 |
| 7 Test quality | findings | F-001(`@DisplayName` 누락, should-fix), F-003(nit). 약화된 단언 없음 — 16→14는 **정정**(시드 기준), SMS 테스트는 오히려 강화 |
| 8 Clarity | findings | F-002(빈 줄). `@Order` 생략 사유 주석 명시 — 양호 |
| 9 Migration / contract | ok | 스키마·OpenAPI 변경 없음. `otpMethod=SMS` 동작은 기존 |

## Summary
- must-fix: 0 · should-fix: 1 · nit: 2 · praise: 2
- Waivers: 없음 (F-001은 공개 동작 아님 — Approve 유지)

## Next action
must-fix 0 → 사용자가 커밋. 제안 메시지 (validate/review 산출물 07·07a·08):

```
Validate + review fix-otp-seed-test-drift: PASS / Approve

- unit 게이트 3f -> 0, 7/7 AC covered
- review: must-fix 0, should-fix 1 (@DisplayName follow-up)

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
```

> follow-up 후보: `UserStorageIntegrationTest` 전체에 `@DisplayName` 도입 (F-001) — 별도 `/spec` 또는 소규모 chore.
> feature 머지 후 `harness.sh --baseline` 재실행으로 `_baseline.json` `unit` fail→pass ratchet down.
