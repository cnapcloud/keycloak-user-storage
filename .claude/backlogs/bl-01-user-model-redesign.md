# BL-01: User 모델 재설계 — id 분리 + attributes 맵 + enabled/emailVerified

---

## Why

**현재 문제:**
- `User.username`이 PK → USP는 별도 `id`를 키로 사용 (`GET /user/{id}`, `DELETE /user/{id}` 등)
- 도메인 속성(birthday, gender, phoneNumber 등)이 개별 컬럼 → USP 규격은 `Map<String, String> attributes`로 통합
- `enabled`, `emailVerified` 필드 없음 → 필수 필드 누락 (`enabled=false`이면 로그인 차단)
- `groups`, `roles` 필드가 모델에 있으나 USP 스펙에 없음

**원하는 상태:**
USP 연계 가이드 §4 데이터 스키마에 맞는 User 엔티티와 응답 구조

---

## Scope

**변경할 것:**
- `User` 엔티티에 `id` 필드 추가 (UUID 기반 PK, 예: `u-{uuid8}`)
- `username`을 PK에서 일반 컬럼으로 변경 (unique 제약은 유지)
- `enabled` (boolean), `emailVerified` (boolean) 필드 추가
- `attributes` 필드 추가 (`@ElementCollection Map<String, String>`)
- 기존 도메인 컬럼(birthday, gender, emailOtp, timeOtp, termsAndConditions)을 attributes로 이전
- `User` 응답 직렬화 시 `attributes` 맵이 포함되어야 함
- `UserService`, `UserRepository` 인터페이스 및 구현체에서 `id` 기반 조회로 전환

**변경하지 않을 것:**
- `CredentialData` 모델 (이미 `id` 필드 있음)
- `GET /user/search`, `GET /user/count`, `GET /user/count/all` 엔드포인트 경로
- H2 in-memory DB 사용 (DDL은 Hibernate `update`로 자동 반영)

---

## UI

해당 없음 (백엔드 전용)

---

## DB

**USER 테이블 변경:**

| 필드 | 변경 | 설명 |
|------|------|------|
| `id` | **신규 추가 (PK)** | UUID 기반, 예: `u-9f3a2c1b` |
| `username` | PK → 일반 컬럼 (unique) | 로그인 ID |
| `first_name` | 유지 | |
| `last_name` | 유지 | |
| `email` | 유지 | |
| `enabled` | **신규 추가** | boolean, default true |
| `email_verified` | **신규 추가** | boolean, default false |
| `created_date` | `Timestamp` → `LocalDateTime` | 직렬화 포맷 통일 |
| `birthday` | **컬럼 제거** → attributes 이전 | |
| `gender` | **컬럼 제거** → attributes 이전 | |
| `email_otp` | **컬럼 제거** → attributes 이전 (key: `otpMethod`) | |
| `time_otp` | **컬럼 제거** | USP 스펙에 없음, 제거 |
| `terms_and_conditions` | **컬럼 제거** → attributes 이전 (key: `termsAccepted`) | |
| `groups` | **제거** (`USER_GROUPS` 테이블) | USP 스펙에 없음 |
| `roles` | **제거** (`USER_ROLES` 테이블) | USP 스펙에 없음 |

**USER_ATTRIBUTES 테이블 (신규 @ElementCollection):**
- `user_id` (FK → USER.id)
- `attr_key` (String)
- `attr_value` (String)

---

## API Contract

### GET /user/{id} 응답 (변경 후)

```json
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
    "lastLoginDate": "2026-03-09T12:00:00",
    "phoneNumber": "010-1234-5678",
    "birthday": "1990-01-01",
    "gender": "M"
  }
}
```

- `attributes`는 절대 `null` 반환 금지 → 없으면 `{}`

### POST /user 요청 (변경 후)

```json
{
  "username": "jane",
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "enabled": true,
  "emailVerified": false,
  "attributes": {
    "phoneNumber": "010-9876-5432",
    "birthday": "1995-06-15",
    "gender": "F"
  }
}
```

---

## 슬라이스 (구현 순서)

### Slice 1 — User 엔티티 변경 (15분)

```
DB:
  - User.java: id (UUID PK) 추가, username unique, enabled/emailVerified 추가
  - attributes Map<String,String> @ElementCollection 추가
  - birthday, gender, emailOtp, timeOtp, termsAndConditions, groups, roles 필드 제거

Acceptance:
  - [ ] ./gradlew build 컴파일 성공
  - [ ] H2 Console에서 USER, USER_ATTRIBUTES 테이블 확인
```

### Slice 2 — Repository/Service id 기반 조회로 전환 (15분)

```
Service:
  - getUserById(id): id 기반 조회로 변경 (기존 username 기반에서)
  - createUser(): id UUID 자동 생성 로직 추가
  - UserRepository: findById(id), findByUsername 분리

Acceptance:
  - [ ] POST /user → id 포함 응답 확인
  - [ ] GET /user/{id} → id로 조회 성공
```

### Slice 3 — attributes 직렬화 검증 + 기존 데이터 마이그레이션 (10분)

```
Config:
  - attributes가 null이면 빈 Map 반환하도록 보장 (JacksonConfig or Entity 초기화)
  - user-data.json 초기 데이터를 새 포맷으로 업데이트

Acceptance:
  - [ ] GET /user/{id} 응답에 "attributes": {} 포함 (null 아님)
  - [ ] user-data.json 로드 시 id 자동 생성 확인
```

---

## Dependencies

- BL-02, BL-03, BL-04, BL-05, BL-06은 이 백로그 완료 후 시작
- BL-07 (Credential), BL-08 (엔드포인트 제거)는 독립적으로 진행 가능

---

## Notes

- `id` 생성 규칙: `"u-" + UUID.randomUUID().toString().replace("-","").substring(0,8)` (예: `u-9f3a2c1b`)
- H2 in-memory 특성상 앱 재시작 시 데이터 초기화됨 → `user-data.json` 포맷도 함께 업데이트 필요
- `@ElementCollection` Map은 `FetchType.EAGER` 로 설정해야 응답 직렬화 시 누락 없음
- Hibernate `update` DDL이므로 기존 컬럼 drop은 자동으로 안 됨 → H2 재시작으로 초기화 (개발 환경 무관)
- `CustomLocalDateTimeSerializer` 활용: `createdDate`를 `LocalDateTime`으로 변경하면 자동 적용

---

## Acceptance Criteria

```
[ ] ./gradlew build 성공
[ ] GET /user/{id}: id로 조회, attributes 포함, createdDate 포맷 yyyy-MM-ddTHH:mm:ss
[ ] GET /user/search: 결과 배열에 attributes 포함
[ ] POST /user: 생성 시 id 자동 부여
[ ] attributes가 null 아닌 {} 반환 확인
[ ] H2 Console: USER 테이블에 id, enabled, email_verified 컬럼 존재
[ ] H2 Console: USER_ATTRIBUTES 테이블 존재
[ ] user-data.json 포맷 신규 스키마로 업데이트
```
