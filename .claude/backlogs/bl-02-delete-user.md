# BL-02: DELETE /user/{id} 엔드포인트 추가

> **상태: 완료** — BL-01 구현 과정에서 함께 반영됨

---

## Why

**현재 문제:**
- `DELETE /user/{id}` 엔드포인트 없음 → USP가 사용자 삭제 시 호출할 수단 없음
- Keycloak 관리자 콘솔에서 사용자 삭제 시 USP 연동 실패

**원하는 상태:**
- `DELETE /user/{id}` → 204 No Content (성공)
- `DELETE /user/{존재하지않는id}` → 404 Not Found

---

## Scope

**변경할 것:**
- `UserService` 인터페이스에 `deleteUser(String id)` 추가
- `UserServiceImpl`에 존재 여부 확인 후 삭제 로직 구현
- `UserController`에 `@DeleteMapping("/{id}")` 추가

**변경하지 않을 것:**
- `CredentialData` — 연관 자격증명은 별도 `DELETE /credential/{id}`로 처리 (BL-08)
- 다른 엔드포인트 경로 및 응답 형식

---

## UI

해당 없음 (백엔드 전용)

---

## DB

- 별도 스키마 변경 없음
- JPA `deleteById()` 호출 → `USER` 및 `USER_ATTRIBUTES` 행 자동 삭제 (`@ElementCollection` cascade)

---

## API Contract

```json
{
  "endpoint": "DELETE /user/{id}",
  "request": {
    "path_param": { "id": "u-9f3a2c1b" }
  },
  "success_response": {
    "status": 204,
    "body": null
  },
  "error_response": {
    "status": 404,
    "body": { "error": "user not found" }
  }
}
```

---

## Dependencies

- BL-01 완료 필수 — `User.id` (String PK)가 있어야 `deleteById(id)` 사용 가능

---

## Notes

- `userRepository.deleteById(id)`는 존재하지 않는 id면 예외를 던지지 않음
  → 반드시 `existsById(id)` 선행 확인 후 404 처리
- `USER_ATTRIBUTES` 행은 `@ElementCollection` cascade로 자동 삭제됨
- `CredentialData`는 별도 테이블 (PK = user.id), 연동 삭제는 BL-08 범위

---

## Acceptance Criteria

```
[x] DELETE /user/{id} → 204 No Content
[x] DELETE /user/{없는id} → 404
[x] ./gradlew build 성공
[x] ./gradlew test 성공 (통합 테스트 포함)
```
