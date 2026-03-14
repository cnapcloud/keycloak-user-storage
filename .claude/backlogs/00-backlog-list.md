# USP Integration Guide 기반 API 재편 — 백로그 목록

> 참조: `keycloak-extension-spi/docs/06-usp-integration-guide.md`
> 목표: USP(User Storage Provider)가 호출하는 10개 엔드포인트를 완전히 규격에 맞게 재편

---

## 갭 분석 요약

| 항목 | 현재 | 목표(USP 규격) |
|------|------|----------------|
| User PK | `username` | 별도 `id` (UUID 형식) |
| User attributes | 필드 직접 매핑 | `Map<String, String>` attributes 맵 |
| enabled / emailVerified | 없음 | 필수 필드 |
| DELETE /user/{id} | 없음 | 필수 (204) |
| PATCH /user/{id}/attributes | 없음 (PUT /user/{id} 존재) | partial update 필수 |
| POST /user 응답 | 전체 User 객체 반환 | {id, username, email} 만 반환, 409 지원 |
| 에러 응답 포맷 | 비일관 (RuntimeException) | {"error": "..."} 통일 |
| PUT /credential/{id} 응답 | 200 + body | 204 No Content |
| 스펙 외 엔드포인트 | GET /credential, POST /credential, PUT /user/{id}/group, PUT /user/{id}/role | 제거 또는 분리 |
| /user/search attributes 검색 | phoneNumber 등 미지원 | attributes 키 검색 지원 |

---

## 백로그 목록 (구현 순서)

| 번호 | 파일 | 제목 | 상태 |
|------|------|------|------|
| BL-01 | [bl-01-user-model-redesign.md](bl-01-user-model-redesign.md) | User 모델 재설계: id 분리 + attributes 맵 + enabled/emailVerified | 완료 |
| BL-02 | [bl-02-delete-user.md](bl-02-delete-user.md) | DELETE /user/{id} 엔드포인트 추가 | 완료 |
| BL-03 | [bl-03-patch-user-attributes.md](bl-03-patch-user-attributes.md) | PUT /user/{id} → PATCH /user/{id}/attributes 교체 (partial update) | 완료 |
| BL-04 | [bl-04-post-user-response.md](bl-04-post-user-response.md) | POST /user 응답 형식 변경 및 409 Conflict 처리 | 완료 |
| ~~BL-05~~ | ~~bl-05-error-response-unification.md~~ | ~~에러 응답 규격 통일~~ | BL-09로 통합 |
| ~~BL-06~~ | ~~bl-06-search-attributes.md~~ | ~~/user/search — attributes 키 검색 지원~~ | BL-10으로 통합 |
| ~~BL-07~~ | ~~bl-07-credential-put-204.md~~ | ~~PUT /credential/{id} 규격 맞추기~~ | BL-09로 통합 |
| ~~BL-08~~ | ~~bl-08-remove-out-of-spec.md~~ | ~~스펙 외 엔드포인트 제거~~ | BL-09로 통합 |
| BL-09 | [bl-09-api-compliance-cleanup.md](bl-09-api-compliance-cleanup.md) | USP API 규격 정리 (에러 포맷 + Credential 정리 + 스펙 외 제거) | 완료 |
| BL-10 | [bl-10-search-attributes.md](bl-10-search-attributes.md) | /user/search — attributes 키(phoneNumber 등) 검색 지원 | 완료 |

---

## 의존 관계

```
BL-01 (User 모델) ✅
  └── BL-02 (DELETE /user) ✅
  └── BL-03 (PATCH attributes) ✅
  └── BL-04 (POST 응답 형식) ✅
  └── BL-09 (에러 포맷 + Credential 정리 + 스펙 외 제거)  ← 다음
  └── BL-10 (attributes 검색)  ← BL-09와 독립, 병렬 가능
```

---

## 참고: USP 필수 10개 엔드포인트

| 메서드 | 경로 | 호출 시점 |
|--------|------|-----------|
| GET | /user/{id} | 로그인, 토큰 갱신 |
| GET | /user/search | 관리자 콘솔 목록 |
| GET | /user/count | 페이지 계산 |
| GET | /user/count/all | 전체 수 |
| POST | /user | 사용자 생성 |
| DELETE | /user/{id} | 사용자 삭제 |
| PATCH | /user/{id}/attributes | 로그인 후 lastLoginDate 등 기록 |
| GET | /credential/{id} | 비밀번호 검증 |
| PUT | /credential/{id} | 비밀번호 변경 |
| DELETE | /credential/{id} | 자격증명 삭제 |
