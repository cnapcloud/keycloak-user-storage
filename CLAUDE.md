# keycloak-user-storage

Keycloak User Storage Provider — Keycloak이 사용자 인증 시 호출하는 REST API 백엔드 (Spring Boot + H2)

---

## Quick Start

```bash
# Build
./gradlew build

# Run locally (port 8081)
./gradlew bootRun

# Run with dev profile (port 8080)
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Docker build & push
make build
make docker-build
make docker-push
```

**Service URLs:**
- API: `http://localhost:8081`
- H2 Console: `http://localhost:8081/h2-console` (JDBC: `jdbc:h2:mem:testdb`, user: `sa`, pw: `password`)
- Actuator: `http://localhost:8081/actuator`

---

## Conventions & Rules (코딩 전 참조)

> 작업 유형에 맞는 파일을 먼저 읽고 시작할 것

### 코딩 컨벤션 — 어떻게 짤 것인가

| 작업 | 참조 파일 |
|---|---|
| 기술 스택 확인 | [`.claude/conventions/00-tech-stack.md`](.claude/conventions/00-tech-stack.md) |
| 예외/에러 처리 | [`.claude/conventions/01-error-handling.md`](.claude/conventions/01-error-handling.md) |
| 로그 추가 | [`.claude/conventions/02-logging.md`](.claude/conventions/02-logging.md) |
| 레이어 구조 변경 | [`.claude/conventions/03-layer-structure.md`](.claude/conventions/03-layer-structure.md) |

### 워크플로우 규칙 — 어떻게 일할 것인가

> 전체 워크플로우 요약: [`.claude/rules/README.md`](.claude/rules/README.md)

| 상황 | 규칙 | 참조 파일 |
|---|---|---|
| 새 API 기능 시작 | API 계약 먼저 정의, 코드 작성 전 공유 | [`.claude/rules/01-contract-first.md`](.claude/rules/01-contract-first.md) |
| 구현 범위 분할 | 15분 단위 수직 슬라이스로 쪼개서 진행 | [`.claude/rules/02-vertical-slicing.md`](.claude/rules/02-vertical-slicing.md) |
| AI에게 컨텍스트 제공 | 필요한 파일만 (총 100KB 이하) | [`.claude/rules/03-context-curation.md`](.claude/rules/03-context-curation.md) |
| 백로그 작성 요청 | UI 위치/형태 + DB 변경 먼저 확인 후 작성 | [`.claude/rules/04-backlog-creation.md`](.claude/rules/04-backlog-creation.md) |

---

## Rules (Read This)

1. **레이어 경계 지킴** — Controller는 Service만, Service는 Repository만 직접 호출
2. **DB는 H2 in-memory** — DDL은 Hibernate `update`로 자동 반영, 스키마 파일 별도 없음
3. **날짜 포맷 통일** — `LocalDateTime`은 반드시 `CustomLocalDateTimeSerializer` 통해 `yyyy-MM-dd'T'HH:mm:ss` (ISO 8601) 형식 직렬화
4. **attributes 맵 null 금지** — `User.attributes`는 반드시 `new HashMap<>()` 초기화, null 반환 시 USP에서 NPE 발생
5. **Lombok 사용** — 반복 보일러플레이트(`@Getter`, `@Builder`, `@NoArgsConstructor` 등) 는 Lombok으로 처리, 수동 getter/setter 금지

---

## Project Status

**완료:**
- User CRUD REST API (`/user`) — USP 규격 기반 재편 (BL-01)
- CredentialData CRUD REST API (`/credential`)
- 동적 사용자 검색 (JPA Criteria API)
- DELETE /user/{id}, PATCH /user/{id}/attributes 구현
- Docker 이미지 빌드 및 Harbor 레지스트리 배포 파이프라인
- 테스트 데이터 (`user-data.json`, `credential-data.json`)
- 통합 테스트 (`UserStorageIntegrationTest` — 19개 케이스)

**진행 중:** USP API 재편 (BL-02~08 대기 — `.claude/backlog/00-backlog-list.md` 참조)

---

## Before Coding

1. `application.yml` 확인 — DB, 포트, 로깅 설정 파악
2. 변경할 레이어의 인터페이스(`UserService`, `CredentialDataService` 등) 먼저 확인
3. attributes 업데이트는 `PATCH /user/{id}/attributes` — partial update 의미 (null=삭제, 미포함=유지)

---

## Common Commands

```bash
# Build
./gradlew build
./gradlew bootRun

# Docker
make docker-build                # 단일 아키텍처
make docker-build-multi          # amd64 + arm64
make docker-push                 # harbor.kind.internal/library/keycloak-user-storage:<git-sha>

# H2 Console (k8s 환경)
# readme.txt 참고 — kubectl port-forward 필요
```

---

## File Structure

```
keycloak-user-storage/
├── src/main/java/com/keycloak/userstorage/
│   ├── UserApplication.java          # 진입점
│   ├── controller/
│   │   ├── UserController.java       # /user 엔드포인트
│   │   └── CredentialDataController.java  # /credential 엔드포인트
│   ├── service/
│   │   ├── UserService.java / UserServiceImpl.java
│   │   └── CredentialDataService.java / CredentialDataServiceImpl.java
│   ├── repository/
│   │   ├── UserRepository.java           # JpaRepository + JPQL
│   │   ├── UserQueryRepository.java      # 동적 쿼리 인터페이스
│   │   ├── UserRepositoryImpl.java       # Criteria API 구현
│   │   └── CredentialDataRepository.java
│   ├── model/
│   │   ├── User.java             # PK: id (u-XXXXXXXX), username unique, attributes Map<String,String>
│   │   └── CredentialData.java   # Argon2 자격증명 저장 (PK=사용자 id)
│   ├── config/
│   │   ├── JacksonConfig.java
│   │   └── CustomLocalDateTimeSerializer.java
│   └── utils/
│       └── SnakeToCamelConverter.java
├── src/main/resources/
│   ├── application.yml           # 기본 설정 (port 8081, H2)
│   ├── application-dev.yml       # dev 프로파일 (port 8080)
│   ├── user-data.json            # 초기 테스트 사용자 17명 (attributes 맵 포맷)
│   └── credential-data.json      # 초기 자격증명 템플릿
├── Dockerfile
├── Makefile                      # build/docker 타겟
├── build.gradle
└── readme.txt                    # H2 console 접근법, SQL 예시
```

---

## Key API Endpoints

```
GET    /user/search              # 검색 (username, firstName, lastName, email, first, max)
GET    /user/count/all           # 전체 사용자 수 → 정수 반환
GET    /user/count               # 조건별 사용자 수 → 정수 반환
GET    /user/{id}                # id로 단건 조회 (200/404)
POST   /user                     # 사용자 생성 → {id, username, email} 반환 (201/409)
DELETE /user/{id}                # 사용자 삭제 (204/404)
PATCH  /user/{id}/attributes     # attributes 부분 업데이트 (204/404)

GET    /credential/{id}          # 자격증명 조회 (200/404)
PUT    /credential/{id}          # 자격증명 업데이트 (204/404)
DELETE /credential/{id}          # 자격증명 삭제 (204/404)
```

---

## Known Gotchas

- **H2 in-memory**: 앱 재시작 시 데이터 초기화됨. 영속 데이터 필요 시 `jdbc:h2:file:` URL로 변경
- **내부 Maven 저장소**: `http://reposilite.kind.internal/releases` — 외부 네트워크에서 빌드 시 의존성 해결 실패 가능
- **이미지 태그**: `make docker-push`는 현재 git commit의 short SHA(7자)를 태그로 사용
- **`User.id`가 PK**: `u-XXXXXXXX` 형식 UUID, `createUser()`에서 자동 생성. `username`은 unique 컬럼
- **`attributes`는 별도 테이블**: `@ElementCollection`으로 `USER_ATTRIBUTES` 테이블 자동 생성. `FetchType.EAGER` 필수
- **attributes null 금지**: `User.attributes`는 항상 `HashMap` 초기화 — null 반환 시 USP에서 NPE

---

## Acceptance Criteria (All Before PR)

```
[ ] ./gradlew build 성공 (컴파일 에러 없음)
[ ] ./gradlew test 성공 (19개 통합 테스트 전부 PASSED)
[ ] H2 Console에서 USER, USER_ATTRIBUTES 테이블 스키마 정상 확인
[ ] 추가/변경한 엔드포인트 수동 테스트 (curl or Postman)
[ ] Docker 이미지 빌드 성공 (make docker-build)
```

---

## When Starting New Work

1. `git pull` — 최신 main 동기화
2. 관련 Controller/Service/Repository 파일 먼저 읽기
3. 인터페이스 수정 → 구현체 수정 → Controller 순서로 작업
4. `./gradlew bootRun`으로 로컬 검증 후 PR

---

**Last Updated**: 2026-03-09
**Main Branch**: main
**Registry**: `harbor.kind.internal/library/keycloak-user-storage`
