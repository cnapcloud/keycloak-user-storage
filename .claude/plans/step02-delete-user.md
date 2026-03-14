# Step 02: DELETE /user/{id} 엔드포인트 추가

> 백로그: BL-02
> 목표: 사용자 삭제 엔드포인트 추가 — 204 / 404 응답
> **상태: 완료** (BL-01 구현 시 함께 반영)

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `service/UserService.java` | `deleteUser(String id)` 시그니처 추가 |
| `service/UserServiceImpl.java` | 존재 확인 + `deleteById` 구현 |
| `controller/UserController.java` | `@DeleteMapping("/{id}")` 추가 |

---

## Slice 1 — Service 레이어 (10분)

**`service/UserService.java`:**

```java
void deleteUser(String id);
```

**`service/UserServiceImpl.java`:**

```java
@Override
@Transactional
public void deleteUser(String id) {
    if (!userRepository.existsById(id)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
    }
    userRepository.deleteById(id);
}
```

핵심 결정:
- `existsById()` → 없으면 404 (`ResponseStatusException`)
- `deleteById()` → `USER` + `USER_ATTRIBUTES` 행 cascade 삭제

---

## Slice 2 — Controller 레이어 (5분)

**`controller/UserController.java`:**

```java
@DeleteMapping("/{id}")
@Transactional
public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();  // 204
}
```

---

## 검증

```bash
# 성공 케이스 (204)
curl -X DELETE http://localhost:8081/user/u-9f3a2c1b -v
# → HTTP/1.1 204 No Content

# 실패 케이스 (404)
curl -X DELETE http://localhost:8081/user/u-notexist -v
# → HTTP/1.1 404 Not Found
```

---

## 구현 결과 (실제 코드)

### UserService.java

```java
void deleteUser(String id);
```

### UserServiceImpl.java — deleteUser()

```java
@Override
@Transactional
public void deleteUser(String id) {
    if (!userRepository.existsById(id)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
    }
    userRepository.deleteById(id);
}
```

### UserController.java — @DeleteMapping

```java
@DeleteMapping("/{id}")
@Transactional
public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}
```

---

## Acceptance Criteria

```
[x] DELETE /user/{id} → 204 No Content
[x] DELETE /user/{없는id} → 404
[x] ./gradlew build 성공
[x] ./gradlew test 성공
```

---

## 다음 스텝

BL-02 완료 → `step03-patch-user-attributes.md` (BL-03)
