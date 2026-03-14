# BL-10: /user/search — attributes 키 검색 지원

> 통합 대상: BL-06

---

## Why

**현재 문제:**
- `GET /user/search?phoneNumber=010-1234` 요청 시 `buildPredicates()`가 `IllegalArgumentException` catch 후 **warn+skip** → 항상 빈 결과
- USP는 attributes 키(`phoneNumber`, `dormantStatus`, `birthday` 등)로 사용자 검색 요구

**원하는 상태:**
- `phoneNumber`, `dormantStatus` 등 attributes 키를 검색 파라미터로 전달하면 `USER_ATTRIBUTES` 테이블 JOIN하여 결과 반환

---

## Scope

**변경할 것:**
- `UserRepositoryImpl.buildPredicates()` — unknown field 감지 시 `USER_ATTRIBUTES` JOIN 조건으로 전환
- `UserRepositoryImpl.countUsersByConditions()` — 동일 JOIN 로직 적용
- `UserRepositoryImpl.searchUserWithAllFields()` — 변경 없음 (`search=*` 케이스)

**변경하지 않을 것:**
- `UserController`, `UserService` 인터페이스 — 파라미터 구조 그대로
- `UserRepository` JPQL 쿼리들 — 변경 없음
- DB 스키마 — `USER_ATTRIBUTES` 이미 존재

---

## UI

해당 없음 (백엔드 전용)

---

## DB

- `USER_ATTRIBUTES` 테이블 이미 존재 (`@ElementCollection`)
- 쿼리 레벨에서 JOIN 추가만 필요:
  ```sql
  JOIN USER_ATTRIBUTES ua ON ua.user_id = u.id
  WHERE ua.attr_key = 'phoneNumber' AND ua.attr_value LIKE '%010%'
  ```

---

## API Contract

```json
{
  "endpoint": "GET /user/search?phoneNumber=010-1234",
  "success_response": {
    "status": 200,
    "body": [
      {
        "id": "u-9f3a2c1b",
        "username": "john",
        "attributes": { "phoneNumber": "010-1234-5678" }
      }
    ]
  }
}
```

---

## 슬라이스

### Slice 1 — buildPredicates JOIN 분기 (20분)

```
UserRepositoryImpl.buildPredicates() 수정:

private List<Predicate> buildPredicates(...) {
    params.forEach((key, value) -> {
        if (SKIP_KEYS.contains(key)) return;
        try {
            root.get(key);
            predicates.add(cb.like(root.<String>get(key), "%" + value + "%"));
        } catch (IllegalArgumentException e) {
            // attributes 키 → JOIN 조건
            Subquery<String> sub = query.subquery(String.class);
            Root<User> subRoot = sub.from(User.class);
            MapJoin<User, String, String> attrJoin = subRoot.joinMap("attributes");
            sub.select(subRoot.get("id"))
               .where(
                   cb.equal(subRoot.get("id"), root.get("id")),
                   cb.equal(attrJoin.key(), key),
                   cb.like(attrJoin.value(), "%" + value + "%")
               );
            predicates.add(cb.exists(sub));
        }
    });
}

검증:
  - GET /user/search?phoneNumber=010 → 해당 users 반환
  - GET /user/search?dormantStatus=ACTIVE → 해당 users 반환
  - GET /user/search?username=john → 기존 동작 유지
```

---

## Dependencies

- BL-01 완료 필수 (`USER_ATTRIBUTES` 테이블 존재)
- BL-09와 독립적 (병렬 진행 가능)

---

## Notes

- `MapJoin`은 `@ElementCollection Map` 타입에 대한 Criteria API JOIN 방식
- `query` 객체(`CriteriaQuery`)가 `buildPredicates`에 필요 → 시그니처에 추가 필요
- attributes 값은 모두 String — `LIKE` 검색으로 충분
- 복합 조건(`phoneNumber=010&dormantStatus=ACTIVE`)도 AND로 자동 적용됨

---

## Acceptance Criteria

```
[ ] GET /user/search?phoneNumber=010-1234 → 해당 사용자 반환
[ ] GET /user/search?dormantStatus=ACTIVE → 해당 사용자 반환
[ ] GET /user/search?username=john → 기존 동작 유지
[ ] GET /user/search?phoneNumber=010&username=john → AND 조건 동작
[ ] GET /user/count?phoneNumber=010 → 정확한 카운트 반환
[ ] ./gradlew build 성공
[ ] ./gradlew test 성공
```
