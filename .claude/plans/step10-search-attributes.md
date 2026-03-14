# Step 10: /user/search — attributes 키 검색 지원

> 백로그: BL-10
> 목표: phoneNumber, dormantStatus 등 attributes 키로 사용자 검색 지원

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `repository/UserRepositoryImpl.java` | `buildPredicates()` — unknown field 시 EXISTS 서브쿼리로 분기 |

---

## 현재 상태 (Before)

```java
// unknown field → warn + skip (검색 안 됨)
try {
    root.get(key);
    predicates.add(cb.like(...));
} catch (IllegalArgumentException e) {
    log.warn("Unknown field key '{}', skipping predicate", key);
}
```

---

## 변경 후 (After)

```java
// unknown field → USER_ATTRIBUTES JOIN EXISTS 서브쿼리
try {
    root.get(key);
    predicates.add(cb.like(root.<String>get(key), "%" + value + "%"));
} catch (IllegalArgumentException e) {
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
```

핵심: `buildPredicates`에 `CriteriaQuery<?>` 파라미터 추가 → 서브쿼리 생성 가능

---

## SQL 생성 예시

```sql
-- GET /user/search?gender=F
SELECT u.* FROM "user" u
WHERE EXISTS (
    SELECT sub.id FROM "user" sub
    JOIN user_attributes ua ON ua.user_id = sub.id
    WHERE sub.id = u.id
      AND ua.attr_key = 'gender'
      AND ua.attr_value LIKE '%F%'
)

-- GET /user/search?username=john&gender=M  (AND 복합 조건)
SELECT u.* FROM "user" u
WHERE u.username LIKE '%john%'
  AND EXISTS (
      SELECT sub.id FROM "user" sub
      JOIN user_attributes ua ON ua.user_id = sub.id
      WHERE sub.id = u.id
        AND ua.attr_key = 'gender'
        AND ua.attr_value LIKE '%M%'
  )
```

---

## Acceptance Criteria

```
[ ] GET /user/search?dormantStatus=ACTIVE → 17명 반환
[ ] GET /user/search?gender=F → 8명 반환
[ ] GET /user/search?gender=M → 9명 반환
[ ] GET /user/search?username=admin&gender=M → 1명 (admin, M)
[ ] GET /user/count?gender=F → 8 반환
[ ] GET /user/search?username=john → 기존 동작 유지 (1명)
[ ] ./gradlew test 성공
```
