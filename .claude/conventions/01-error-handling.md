# Error Handling

## 원칙
- **Service 레이어** — 리소스 미발견 시 `ResponseStatusException` 사용
- **Controller 레이어** — `Optional`은 `.orElseThrow(() -> new ResponseStatusException(...))` 패턴 사용

## HTTP 상태 코드 규칙 (USP 계약 기준)

| 상황 | 코드 |
|------|------|
| GET 조회 성공 | 200 OK |
| POST 생성 성공 | 201 Created |
| DELETE / PUT / PATCH 성공 | **204 No Content** (본문 없음) |
| 리소스 없음 | 404 Not Found |
| username/email 중복 | 409 Conflict |
| 서버 오류 | 500 Internal Server Error |

## 패턴

### Service에서 Not Found
```java
userRepository.findById(id)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
```

### Controller에서 Optional 반환 (GET)
```java
return userService.getUserById(id)
    .map(ResponseEntity::ok)
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
```

### Controller에서 204 반환 (DELETE / PATCH)
```java
userService.deleteUser(id);
return ResponseEntity.noContent().build();
```

### Service에서 Conflict
```java
if (userRepository.findByUsername(user.getUsername()).isPresent()) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "user already exists");
}
```

## 주의
- Controller에서 `RuntimeException`을 직접 throw하지 않는다 — `ResponseStatusException` 사용
- 현재 전역 `@ControllerAdvice` 없음 — `ResponseStatusException`은 Spring이 자동으로 JSON 에러 응답 생성
- DELETE/PATCH 성공 시 본문 없이 **204** 반환 (200 + body 반환 금지)
- 에러 응답 포맷은 `{"error": "..."}` 통일 예정 (BL-05 — 전역 핸들러로 정리)
