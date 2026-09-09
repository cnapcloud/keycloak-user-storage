# Layer Structure

## 계층 구조
```
Controller → Service (interface + impl) → Repository → Model (Entity)
```

## 각 레이어 책임

### Controller
- HTTP 요청/응답 처리, `ResponseEntity` 반환
- 비즈니스 로직 없음 — Service 호출만
- `@Transactional`은 쓰기 작업(`POST`, `PUT`, `PATCH`, `DELETE`)에 선언
- 파일: `controller/`

### Service
- 비즈니스 로직 담당
- 인터페이스 + 구현체 분리 (`UserService` / `UserServiceImpl`)
- `@Transactional`은 구현체 메서드에 선언
- 파일: `service/`

### Repository
- `JpaRepository` 상속으로 기본 CRUD
- 동적 쿼리는 `UserQueryRepository` 인터페이스 + `UserRepositoryImpl` (Criteria API)
- JPQL은 `UserRepository`에 `@Query`로 정의
- 파일: `repository/`

### Model
- JPA `@Entity`, Lombok `@Data` 사용
- `User`: PK = `id` (String, `u-XXXXXXXX` 형식), `username` = unique 컬럼
- `CredentialData`: PK = `id` (String, 사용자 id와 동일), Argon2 해시 저장
- **SQL 예약어 처리** — 테이블/컬럼명이 SQL 예약어이면 큰따옴표로 이스케이프:
  - 테이블: `@Table(name = "\"user\"")`
  - 컬럼: `@Column(name = "\"value\"")`
  - 예약어: `user`, `value`, `model`, `order` 등
- 파일: `model/`

## User 엔티티 핵심 패턴

### id 생성
```java
// Service.createUser()에서 UUID 기반 생성
user.setId("u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
```

### attributes 맵 (Map<String, String>)
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "USER_ATTRIBUTES", joinColumns = @JoinColumn(name = "user_id"))
@MapKeyColumn(name = "attr_key")
@Column(name = "attr_value")
private Map<String, String> attributes = new HashMap<>();
```
- 반드시 `FetchType.EAGER` — LAZY이면 직렬화 시 누락
- 반드시 `= new HashMap<>()` 초기화 — `null` 반환 시 USP에서 NPE 발생
- `null` 값 키: 해당 속성 삭제 의미 (PATCH partial update)

### PATCH partial update 패턴 (Service)
```java
attrs.forEach((key, value) -> {
    if (value == null) {
        user.getAttributes().remove(key);
    } else {
        user.getAttributes().put(key, value);
    }
});
```
- null → 키 삭제
- 미포함 키 → 기존 값 유지 (절대 전체 덮어쓰기 금지)

## 규칙
- Controller는 Repository를 직접 주입받지 않는다
- Service는 다른 Service를 직접 주입받는 것을 지양한다
- 동적 필드 업데이트는 리플렉션 사용 금지 — attributes Map 패턴 사용
