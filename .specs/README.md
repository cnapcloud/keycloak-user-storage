# .specs/ — 기능 스펙 인덱스

이 서비스는 **harness 기반 spec-driven 워크플로우**로 개발한다.
진입점 · 강제 장치: [`../.claude/README.md`](../.claude/README.md) · 방법론: [`../.claude/docs/methodology.md`](../.claude/docs/methodology.md).

```
/onboard → /spec → /spec-review → /plan → (/build T-NNN → commit) ×N → /validate → /review → commit
```

각 기능은 `YYYY-MM-DD-<feature-id>/` 폴더 아래 번호가 매겨진 산출물 체인
(`01-spec.md` … `08-code-review.md`)을 거친다.

---

## 리포지토리 파일 (`/onboard` 산출)

| 파일 | 내용 |
|---|---|
| [`_stack.json`](_stack.json) | 스택 분류 + 활성 harness 레이어 |
| [`_baseline.json`](_baseline.json) | brownfield 기준선 — 이후 게이트는 이 대비 회귀만 차단 |
| [`_onboarding.md`](_onboarding.md) | 분류 결과 + 기준선 게이트 표 + 미설정 레이어 + 권장 첫 `/spec` |

---

## 진행 중 / 예정 기능

| 상태 | feature-id | 요약 |
|---|---|---|
| 진행 중 (spec) | `2026-09-10-fix-otp-seed-test-drift` | 시드 사용자 `otpMethod` ↔ `UserStorageIntegrationTest` 단언 정합. 커밋 `8bdd8d6`(SKIP→SMS 2건)이 단언 미갱신 → 선행 실패 3건. 고친 뒤 `harness.sh --baseline` 재실행해 `_baseline.json` ratchet down. |

> `/spec`이 이 표를 관리한다 — 새 feature를 `진행 중 (spec)`으로 추가/갱신한다.
> 상태 라벨: `예정` → `진행 중 (spec)` → 이후 phase는 사람이 갱신 (`(plan)`/`(build)`/…) → 완료 시 "완료 이력"으로 이동.

---

## 후속 인프라 작업

harness 레이어(checkstyle · spotbugs · pitest · archunit · openapi · owasp) 설정은
정식 feature로 태우지 않는다 — 필요할 때 개발자가 [`../.claude/docs/harness-gradle.md`](../.claude/docs/harness-gradle.md)의
패치를 `build.gradle`에 직접 적용하고 `harness.sh --baseline`로 baseline을 갱신한다.
플러그인은 Gradle Plugin Portal, 의존성은 Maven Central에서 해결.

---

## 완료 이력 (구 `.claude/backlogs/` · `.claude/plans/`)

BL-01~10 은 이 harness 도입 **이전**에 자유서술형 백로그/계획서 방식으로 완료됨.
상세 근거는 git history. 아래는 압축 인덱스.

| BL | 제목 | 결과 |
|---|---|---|
| BL-01 | User 모델 재설계 | `id`(String PK, `u-XXXXXXXX`) 분리, `username` unique 컬럼화, `enabled`/`emailVerified` 추가, 도메인 컬럼 → `attributes` `Map<String,String>` (`@ElementCollection`, `USER_ATTRIBUTES`, `FetchType.EAGER`), `createdDate` `LocalDateTime` + `CustomLocalDateTimeSerializer`, `groups`/`roles` 제거 |
| BL-02 | `DELETE /user/{id}` | `existsById` 선행 확인 → 204 / 404. `USER_ATTRIBUTES` cascade 삭제 |
| BL-03 | `PATCH /user/{id}/attributes` | `PUT /user/{id}` 대체. partial update — `null` 값=키 삭제, 미포함 키=기존 값 유지 → 204 / 404 |
| BL-04 | `POST /user` 응답 형식 | 전체 User → `{id, username, email}` 만 반환. 201 / 409(username 중복) |
| ~~BL-05~~ | 에러 응답 통일 | → BL-09 로 통합 |
| ~~BL-06~~ | attributes 검색 | → BL-10 으로 통합 |
| ~~BL-07~~ | `PUT /credential` 204 | → BL-09 로 통합 |
| ~~BL-08~~ | 스펙 외 엔드포인트 제거 | → BL-09 로 통합 |
| BL-09 | USP API 규격 정리 | `GlobalExceptionHandler`(`@RestControllerAdvice`) → 전 에러 `{"error": "..."}`. `PUT /credential/{id}` upsert 버그 수정 → 204 / 404. 스펙 외 `GET /credential`·`POST /credential` 제거 |
| BL-10 | `/user/search` attributes 키 검색 | `UserRepositoryImpl` Criteria API — unknown 필드는 `USER_ATTRIBUTES` `MapJoin` EXISTS 서브쿼리로. 복합 조건 AND. `count` 동일 적용 |

### 알려진 회귀 (baseline 캡처됨, 미해결)

`_baseline.json` `gates.unit.status = "fail"` — `UserStorageIntegrationTest` 3건 실패:

- `search_byAttributeKey_otpMethod_returnsAllUsers` — `otpMethod=SKIP` 16 기대, 실제 14
- `count_byAttributeKey_otpMethod_returns17` — 동일 원인
- `search_combinedFieldAndAttribute_returnsIntersection` — 교집합 1 기대, 실제 0

원인: 커밋 `8bdd8d6`(시드 사용자 `otpMethod` SKIP→SMS 2건)이 테스트 단언을 갱신하지 않음.
harness 도입과 무관한 선행 breakage. 별도 `/spec` (예: `fix-otp-seed-test-drift`)으로 처리 권장 —
그때 `_baseline.json` 을 함께 갱신(ratchet down)한다.
