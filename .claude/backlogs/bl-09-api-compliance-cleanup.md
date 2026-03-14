# BL-09: USP API 규격 정리 (에러 포맷 + Credential 정리)

> 통합 대상: BL-05 (에러 응답 통일) + BL-07 (PUT /credential 204) + BL-08 (스펙 외 엔드포인트 제거)

---

## Why

**현재 문제:**
- 에러 응답 형식 비일관 — `ResponseStatusException`은 Spring 기본 포맷(`timestamp`, `path` 포함)으로 반환됨. USP는 `{"error": "..."}` 단순 포맷 기대
- `PUT /credential/{id}` → 200+body 반환. USP 규격은 204 No Content
  - 로직 버그: `existed` 체크를 upsert *이후*에 하므로 항상 `true` → 항상 200
- `GET /credential` (전체 목록), `POST /credential` — USP 스펙에 없는 엔드포인트 잔존

**원하는 상태:**
- 모든 에러 응답: `{"error": "메시지"}` 형식 통일
- `PUT /credential/{id}` → 204 No Content (성공), 404 (없는 id)
- 스펙 외 엔드포인트 제거

---

## Scope

**변경할 것:**
- `GlobalExceptionHandler` 신규 추가 (`@ControllerAdvice`) — `ResponseStatusException` 처리
- `CredentialDataController` — `PUT /{id}` 응답을 204로 변경, 404 처리 추가
- `CredentialDataController` — `GET /` (전체 목록), `POST /` 제거
- `CredentialDataService` / `CredentialDataServiceImpl` — upsert 시 존재 여부 사전 확인 로직 정리

**변경하지 않을 것:**
- `GET /credential/{id}` — 유지 (USP 스펙)
- `DELETE /credential/{id}` — 유지 (USP 스펙)
- User 관련 컨트롤러/서비스 — 변경 없음

---

## UI

해당 없음 (백엔드 전용)

---

## DB

스키마 변경 없음

---

## API Contract

### 에러 응답 포맷 (전체 통일)

```json
{
  "error": "user not found"
}
```

### PUT /credential/{id}

```json
{
  "endpoint": "PUT /credential/{id}",
  "request": {
    "body": {
      "credentialData": "...",
      "secretData": "...",
      "additionalParameters": "{}"
    }
  },
  "success_response": { "status": 204, "body": null },
  "error_response": { "status": 404, "body": { "error": "credential not found" } }
}
```

### 제거 엔드포인트

```
DELETE  GET  /credential          (전체 목록 — 스펙 외)
DELETE  POST /credential          (생성 — 스펙 외)
```

---

## 슬라이스 (구현 순서)

### Slice 1 — GlobalExceptionHandler 추가 (10분)

```
신규 파일: config/GlobalExceptionHandler.java

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(Map.of("error", ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }
}

검증:
  - GET /user/없는id → {"error": "user not found"} (404)
  - DELETE /user/없는id → {"error": "user not found"} (404)
```

### Slice 2 — CredentialDataController 정리 (15분)

```
변경:
  - GET / (getAllCredentials) 메서드 삭제
  - POST / (createCredential) 메서드 삭제
  - PUT /{id}: 204 반환으로 변경, 404 처리 추가

PUT /{id} 수정 후:
  @PutMapping("/{id}")
  public ResponseEntity<Void> updateCredential(@PathVariable String id,
          @RequestBody CredentialData credential) {
      credentialDataService.updateCredential(id, credential);  // 없으면 404
      return ResponseEntity.noContent().build();
  }

검증:
  - PUT /credential/{id} → 204
  - PUT /credential/없는id → 404 {"error": "..."}
  - GET /credential → 404 (엔드포인트 없음)
  - POST /credential → 404 (엔드포인트 없음)
```

### Slice 3 — CredentialDataService update 로직 (10분)

```
CredentialDataService 인터페이스:
  - upsertCredential() 제거 → updateCredential(id, credential) 추가

CredentialDataServiceImpl:
  - updateCredential(): findById → 없으면 404, 있으면 save → void 반환

검증:
  - ./gradlew build 성공
  - ./gradlew test 성공
```

---

## Dependencies

- BL-01~04 완료 필수

---

## Notes

- `GlobalExceptionHandler`는 `ResponseStatusException`만 처리하면 됨 — 그 외 예외는 Spring 기본 처리 유지
- upsert 로직 제거 이유: USP는 사전에 credential 존재 여부를 알고 PUT 호출 → upsert 불필요
- `POST /credential`, `GET /credential` 제거 후 통합 테스트에서 해당 케이스 있으면 함께 수정 필요

---

## Acceptance Criteria

```
[ ] GET /user/없는id → 404 {"error": "user not found"}
[ ] DELETE /user/없는id → 404 {"error": "user not found"}
[ ] PUT /credential/{id} (존재) → 204 No Content
[ ] PUT /credential/없는id → 404 {"error": "credential not found"}
[ ] GET /credential → 405 또는 404 (엔드포인트 제거)
[ ] POST /credential → 405 또는 404 (엔드포인트 제거)
[ ] ./gradlew build 성공
[ ] ./gradlew test 성공
```
