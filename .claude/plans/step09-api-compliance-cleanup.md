# Step 09: USP API 규격 정리

> 백로그: BL-09
> 목표: 에러 포맷 통일 + PUT /credential 204 + 스펙 외 엔드포인트 제거

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `config/GlobalExceptionHandler.java` | **신규 추가** |
| `service/CredentialDataService.java` | 인터페이스 정리 (불필요 메서드 제거, updateCredential 추가) |
| `service/CredentialDataServiceImpl.java` | 구현체 정리 |
| `controller/CredentialDataController.java` | 스펙 외 엔드포인트 제거, PUT → 204 변경 |

---

## 현재 상태 (Before)

### CredentialDataService 인터페이스
```java
List<CredentialData> getAllCredentials();        // 스펙 외 — 제거
Optional<CredentialData> getCredentialById(String id);  // 유지
CredentialData createCredential(CredentialData credentialData);  // 스펙 외 — 제거
CredentialData upsertCredential(String id, CredentialData credential);  // 교체
void deleteCredential(String id);               // 유지
```

### CredentialDataController
```
GET  /credential        → 200 + List  (스펙 외 — 제거)
GET  /credential/{id}   → 200 / empty (유지)
POST /credential        → 201 + body  (스펙 외 — 제거)
PUT  /credential/{id}   → 200 + body  (204로 변경, 404 추가)
DELETE /credential/{id} → 204         (유지)
```

### PUT /credential/{id} 버그
```java
// 버그: upsert 후 체크 → 항상 존재 → 항상 200
CredentialData result = credentialDataService.upsertCredential(id, credential);
boolean existed = credentialDataService.getCredentialById(id).isPresent(); // 항상 true
```

---

## Slice 1 — GlobalExceptionHandler 추가 (10분)

**신규 파일: `config/GlobalExceptionHandler.java`**

```java
package com.keycloak.userstorage.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", message));
    }
}
```

**검증:**
```bash
curl -s http://localhost:8081/user/u-notexist | jq
# → {"error": "user not found"}  (404)

curl -s -X DELETE http://localhost:8081/user/u-notexist | jq
# → {"error": "user not found"}  (404)
```

---

## Slice 2 — CredentialDataService 인터페이스 정리 (5분)

**`service/CredentialDataService.java` 교체:**

```java
package com.keycloak.userstorage.service;

import java.util.Optional;

import com.keycloak.userstorage.model.CredentialData;

public interface CredentialDataService {
    Optional<CredentialData> getCredentialById(String id);
    void updateCredential(String id, CredentialData credential);  // upsertCredential 교체
    void deleteCredential(String id);
    // 제거: getAllCredentials(), createCredential(), upsertCredential()
}
```

---

## Slice 3 — CredentialDataServiceImpl 구현 (10분)

**`service/CredentialDataServiceImpl.java` 교체:**

```java
package com.keycloak.userstorage.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.CredentialData;
import com.keycloak.userstorage.repository.CredentialDataRepository;

@Service
public class CredentialDataServiceImpl implements CredentialDataService {

    private final CredentialDataRepository credentialDataRepository;

    @Autowired
    public CredentialDataServiceImpl(CredentialDataRepository credentialDataRepository) {
        this.credentialDataRepository = credentialDataRepository;
    }

    @Override
    public Optional<CredentialData> getCredentialById(String id) {
        return credentialDataRepository.findById(id);
    }

    @Override
    public void updateCredential(String id, CredentialData credential) {
        CredentialData existing = credentialDataRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "credential not found"));
        credential.setId(existing.getId());
        credential.setCreatedDate(existing.getCreatedDate());
        credential.setUpdatedDate(Timestamp.from(Instant.now()));
        credentialDataRepository.save(credential);
    }

    @Override
    public void deleteCredential(String id) {
        if (!credentialDataRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "credential not found");
        }
        credentialDataRepository.deleteById(id);
    }
}
```

핵심 결정:
- `updateCredential()`: 없으면 404, 있으면 `updatedDate` 갱신 후 save
- `deleteCredential()`: `existsById()` 선행 확인 후 404 (기존에는 체크 없음)
- `Timestamp` 유지: `CredentialData.createdDate/updatedDate`가 `Timestamp` 타입이므로 그대로 사용

---

## Slice 4 — CredentialDataController 정리 (10분)

**`controller/CredentialDataController.java` 교체:**

```java
package com.keycloak.userstorage.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.keycloak.userstorage.model.CredentialData;
import com.keycloak.userstorage.service.CredentialDataService;

@RestController
@RequestMapping("/credential")
public class CredentialDataController {

    private final CredentialDataService credentialDataService;

    public CredentialDataController(CredentialDataService credentialDataService) {
        this.credentialDataService = credentialDataService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CredentialData> getCredentialById(@PathVariable String id) {
        return credentialDataService.getCredentialById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "credential not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCredential(@PathVariable String id,
            @RequestBody CredentialData credential) {
        credentialDataService.updateCredential(id, credential);
        return ResponseEntity.noContent().build();  // 204
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredential(@PathVariable String id) {
        credentialDataService.deleteCredential(id);
        return ResponseEntity.noContent().build();  // 204
    }

    // 제거: GET / (getAllCredentials), POST / (createCredential)
}
```

변경 사항:
- `GET /credential/{id}` — 404 시 `{"error": "credential not found"}` 응답 (GlobalExceptionHandler 경유)
- `PUT /credential/{id}` — 204 반환, 404 처리
- `DELETE /credential/{id}` — 서비스 레이어에서 404 처리
- `GET /`, `POST /` — 제거

---

## 전체 검증

```bash
# 에러 포맷 통일
curl -s http://localhost:8081/user/u-notexist
# → 404 {"error": "user not found"}

curl -s http://localhost:8081/credential/u-notexist
# → 404 {"error": "credential not found"}

# PUT /credential → 204
curl -s -X PUT http://localhost:8081/credential/u-abc12345 \
  -H "Content-Type: application/json" \
  -d '{"value":"hash","salt":"...","algorithm":"argon2","iterations":3,"type":"password","additionParameters":"{}"}' \
  -v
# → HTTP/1.1 204 No Content

# PUT /credential 없는 id → 404
curl -s -X PUT http://localhost:8081/credential/u-notexist \
  -H "Content-Type: application/json" -d '{}' | jq
# → {"error": "credential not found"}

# 스펙 외 엔드포인트 제거 확인
curl -s http://localhost:8081/credential -v
# → 404 또는 405

curl -s -X POST http://localhost:8081/credential -H "Content-Type: application/json" -d '{}' -v
# → 404 또는 405
```

---

## 통합 테스트 영향

기존 테스트 중 아래 케이스가 있으면 수정 필요:
- `GET /credential` 전체 목록 호출 테스트
- `POST /credential` 생성 테스트
- `PUT /credential/{id}` → 200 응답 기대 테스트

---

## Acceptance Criteria

```
[ ] GET /user/없는id → 404 {"error": "user not found"}
[ ] DELETE /user/없는id → 404 {"error": "user not found"}
[ ] GET /credential/없는id → 404 {"error": "credential not found"}
[ ] PUT /credential/{존재하는id} → 204 No Content
[ ] PUT /credential/없는id → 404 {"error": "credential not found"}
[ ] DELETE /credential/없는id → 404 {"error": "credential not found"}
[ ] GET /credential → 404/405 (엔드포인트 없음)
[ ] POST /credential → 404/405 (엔드포인트 없음)
[ ] ./gradlew build 성공
[ ] ./gradlew test 성공
```

---

## 다음 스텝

BL-09 완료 → `step10-search-attributes.md` (BL-10, 독립 진행 가능)
