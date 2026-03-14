# Step 01: User 모델 재설계

> 백로그: BL-01
> 목표: username PK → 별도 id, attributes Map 도입, enabled/emailVerified 추가

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `model/User.java` | 전면 수정 |
| `repository/UserRepository.java` | PK 타입 변경, findByUsername 추가 |
| `repository/UserQueryRepository.java` | 시그니처 유지 (attributes 검색은 BL-06) |
| `repository/UserRepositoryImpl.java` | builPredicates — attributes 키 무시 처리 |
| `service/UserService.java` | deleteUser 시그니처 추가 |
| `service/UserServiceImpl.java` | createUser UUID 생성, getUserById id 기반 확인 |
| `controller/UserController.java` | GET /{id} 404 처리 |
| `resources/user-data.json` | id 필드 추가, attributes 맵으로 재구성 |

---

## Slice 1 — User 엔티티 수정 (15분)

**`model/User.java` 전면 교체:**

```java
@Data
@Entity
@Table(name = "\"user\"")
public class User {

    @Id
    private String id;                     // "u-" + 8자리 UUID (신규 PK)

    @Column(unique = true, nullable = false)
    private String username;               // PK → unique 컬럼

    private String firstName;
    private String lastName;
    private String email;
    private boolean enabled = true;        // 신규
    private boolean emailVerified = false; // 신규

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime createdDate;     // Timestamp → LocalDateTime

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "USER_ATTRIBUTES",
        joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "attr_key")
    @Column(name = "attr_value")
    private Map<String, String> attributes = new HashMap<>();  // 신규, null 금지

    // 제거: birthday, gender, emailOtp, timeOtp, termsAndConditions, groups, roles
}
```

**검증:**
- `./gradlew build` 컴파일 성공
- H2 Console: `USER` 테이블에 `id`, `enabled`, `email_verified` 컬럼 확인
- H2 Console: `USER_ATTRIBUTES` 테이블 생성 확인

---

## Slice 2 — Repository/Service id 기반으로 전환 (15분)

**`repository/UserRepository.java`:**

```java
// JpaRepository<User, String> 유지 (PK 타입 동일 — String)
public interface UserRepository extends JpaRepository<User, String>, UserQueryRepository {

    Optional<User> findByUsername(String username);  // 신규 — username 단건 조회용

    @Query("SELECT p FROM User p WHERE p.username LIKE %:s% OR p.firstName LIKE %:s% OR p.lastName LIKE %:s%")
    List<User> searchUsers(@Param("s") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM User p WHERE p.username LIKE %:s% OR p.firstName LIKE %:s% OR p.lastName LIKE %:s%")
    Long countUsers(@Param("s") String search);
}
```

**`repository/UserRepositoryImpl.java` — builPredicates 수정:**

```java
// attributes 키는 Criteria 직접 매핑 불가 → 무시하고 경고만 출력 (BL-06에서 처리)
params.forEach((key, value) -> {
    try {
        root.get(key); // 필드 존재 여부 확인
        predicates.add(cb.like(root.get(key), "%" + value + "%"));
    } catch (IllegalArgumentException e) {
        log.warn("Unknown field key '{}', skipping predicate", key);
    }
});
```

**`service/UserService.java` — 시그니처 추가/제거:**

```java
public interface UserService {
    List<User> searchUsers(Map<String, String> reqParams, Integer first, Integer max);
    Long countUsers(Map<String, String> reqParams);
    Integer getAllUserCount();
    Optional<User> getUserById(String id);
    User createUser(User user);
    void patchAttributes(String id, Map<String, String> attrs); // 신규 (BL-03 구현 예정)
    void deleteUser(String id);                                  // 신규 (BL-02 구현 예정)
    // 제거: updateUser, updateGroups, updateRoles
}
```

**`service/UserServiceImpl.java` — createUser UUID 생성:**

```java
@Override
@Transactional
public User createUser(User user) {
    // username 중복 체크 (BL-04에서 409 응답 처리)
    if (userRepository.findByUsername(user.getUsername()).isPresent()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "user already exists");
    }
    user.setId("u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    if (user.getAttributes() == null) {
        user.setAttributes(new HashMap<>());
    }
    return userRepository.save(user);
}

@Override
public void deleteUser(String id) {
    if (!userRepository.existsById(id)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
    }
    userRepository.deleteById(id);
}

@Override
@Transactional
public void patchAttributes(String id, Map<String, String> attrs) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    attrs.forEach((key, value) -> {
        if (value == null) {
            user.getAttributes().remove(key);
        } else {
            user.getAttributes().put(key, value);
        }
    });
    userRepository.save(user);
}
```

**검증:**
- `POST /user` → 응답에 `id` 필드 포함 확인
- `GET /user/{id}` → id로 조회 성공

---

## Slice 3 — Controller 정리 + user-data.json 업데이트 (10분)

**`controller/UserController.java` — GET /{id} 404 JSON 처리:**

```java
@GetMapping("/{id}")
public ResponseEntity<User> getUserById(@PathVariable String id) {
    return userService.getUserById(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
}

@PostMapping
@Transactional
public ResponseEntity<Map<String, String>> createUser(@RequestBody User user) {
    User saved = userService.createUser(user);
    return ResponseEntity.status(201).body(Map.of(
        "id", saved.getId(),
        "username", saved.getUsername(),
        "email", saved.getEmail() != null ? saved.getEmail() : ""
    ));
}

@DeleteMapping("/{id}")
@Transactional
public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}

@PatchMapping("/{id}/attributes")
@Transactional
public ResponseEntity<Void> patchAttributes(@PathVariable String id,
        @RequestBody Map<String, String> attrs) {
    userService.patchAttributes(id, attrs);
    return ResponseEntity.noContent().build();
}

// 제거: PUT /{id}, PUT /{id}/group, PUT /{id}/role
```

**`resources/user-data.json` 포맷 변경 예시:**

```json
[
  {
    "id": "u-9f3a2c1b",
    "username": "john",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "enabled": true,
    "emailVerified": false,
    "createdDate": "2026-01-01T00:00:00",
    "attributes": {
      "dormantStatus": "ACTIVE",
      "phoneNumber": "010-1234-5678",
      "birthday": "1990-01-01",
      "gender": "M"
    }
  }
]
```

**검증:**
- `GET /user/{id}` 응답에 `"attributes": {}` 포함 (null 아님)
- `DELETE /user/{id}` → 204 반환
- `PATCH /user/{id}/attributes` → null 값 키 삭제, 미포함 키 유지 확인

---

## 전체 Acceptance Criteria

```
[ ] ./gradlew build 성공
[ ] H2 Console: USER 테이블에 id, enabled, email_verified 컬럼 존재
[ ] H2 Console: USER_ATTRIBUTES 테이블 존재
[ ] GET /user/{id}: id로 조회, attributes 포함, createdDate = "yyyy-MM-ddTHH:mm:ss"
[ ] GET /user/{id}: 없는 id → 404 {"error": "..."}  (BL-05와 연계)
[ ] GET /user/search: 결과 배열에 attributes 포함
[ ] POST /user: 생성 → {id, username, email} 반환 (201)
[ ] POST /user: 중복 username → 409
[ ] DELETE /user/{id}: 204 반환
[ ] PATCH /user/{id}/attributes: null 키 삭제, 미포함 키 유지
[ ] attributes null 아닌 {} 반환 확인
[ ] user-data.json 신규 포맷으로 업데이트
```

---

## 다음 스텝

BL-01 완료 후 → `step02` ~ `step08` 순차 진행
(BL-07 Credential PUT 수정은 BL-01과 독립이므로 병렬 진행 가능)
