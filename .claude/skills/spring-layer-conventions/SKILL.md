---
name: spring-layer-conventions
description: Layer boundaries and entity patterns for keycloak-user-storage — Controller → Service (iface+impl) → Repository → Model, the attributes map, id generation, SQL-reserved-word escaping. Use in /plan and /build.
---
# Layer structure & entity patterns

Ported from the former `.claude/conventions/00` + `03`. Full detail in `references/`.

## Layers
```
Controller → Service (interface + impl) → Repository → Model (Entity)
```
- **Controller** — HTTP only, returns `ResponseEntity`, no business logic, calls Service only. `@Transactional` on write endpoints. `controller/`.
- **Service** — business logic, interface + impl split (`UserService` / `UserServiceImpl`), `@Transactional` on impl methods. `service/`.
- **Repository** — `JpaRepository` for CRUD; dynamic queries via `UserQueryRepository` + `UserRepositoryImpl` (Criteria API); JPQL via `@Query`. `repository/`.
- **Model** — JPA `@Entity` + Lombok. `model/`.

## Hard boundaries (also `archunit-rules`)
- Controller must **not** inject a Repository.
- Service should not inject another Service.
- Dynamic field updates use the `attributes` map — **never reflection**.

## Entity essentials
- `User.id` — PK, `String`, format `u-XXXXXXXX`; generated in `createUser()`:
  `user.setId("u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));`
- `User.username` — unique column, not the PK.
- `User.attributes` — `Map<String,String>`, `@ElementCollection(fetch = FetchType.EAGER)`, table `USER_ATTRIBUTES`. **Must** be `= new HashMap<>()`. Never return null (USP NPE).
- PATCH attributes semantics: `null` value → remove key; absent key → keep existing (never full overwrite).
- `CredentialData.id` = the user's `id` (Argon2 hash store).
- SQL reserved words escaped with quotes: `@Table(name = "\"user\"")`, `@Column(name = "\"value\"")`.

## Tech stack
Java 17, Spring Boot 3.3.4, Gradle, H2 in-memory (`hibernate.ddl-auto=update`, no migration files), Lombok, Jackson 2.15.2 (`FAIL_ON_UNKNOWN_PROPERTIES=false`, `NON_NULL`). Internal Maven repo `reposilite.kind.internal` — external network cannot resolve dependencies.
