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
| _(없음)_ | | 다음 작업을 `/spec "<설명>"` 으로 시작 |

> `/spec`이 이 표에 새 feature를 `진행 중 (spec)`으로 추가하고, `/review`가 Approve 시 "완료 이력"으로 옮긴다.
> 상태 라벨: `예정` → `진행 중 (spec)` → 중간 phase는 사람이 갱신 (`(plan)`/`(build)`/`(validate)`) → `/review` Approve → "완료 이력".

---

## 후속 인프라 작업

harness 레이어(checkstyle · spotbugs · pitest · archunit · openapi · owasp) 설정은
정식 feature로 태우지 않는다 — 필요할 때 개발자가 [`../.claude/docs/harness-gradle.md`](../.claude/docs/harness-gradle.md)의
패치를 `build.gradle`에 직접 적용하고 `harness.sh --baseline`로 baseline을 갱신한다.
플러그인은 Gradle Plugin Portal, 의존성은 Maven Central에서 해결.

---

## 완료 이력

### harness 워크플로우 (`.specs/YYYY-MM-DD-<id>/`)
`/review` Approve 시 이 표로 이동. 상세는 `.specs/<id>/` 산출물 체인.

| feature-id | verdict | 결과 |
|---|---|---|
| `2026-09-10-fix-otp-seed-test-drift` | validate PASS · review Approve | 시드 `otpMethod` drift 해소 — `UserStorageIntegrationTest` 단언 16→14, 교집합 질의 `SKIP`→`SMS`, `otpMethod=SMS` characterization 테스트 추가. `unit` 3f→0, 7/7 AC. should-fix 1(`@DisplayName` follow-up). 커밋 `3ac2f1a`·`1805205` |

### 구 백로그 (harness 이전 · 자유서술형)
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

### 알려진 회귀 — 해소됨 (baseline ratchet down 대기)

`_baseline.json` `gates.unit.status = "fail"` (선행 실패 3건, 커밋 `8bdd8d6` 시드 `otpMethod` SKIP→SMS 후
단언 미갱신)은 **`2026-09-10-fix-otp-seed-test-drift`에서 해소**됨 (커밋 `3ac2f1a`·`1805205`, `unit` 3f→0).
머지 후 `harness.sh --baseline` 재실행으로 `_baseline.json`을 `fail`→`pass`로 ratchet down.
