# Logging

## 설정 (`application.yml`)
```yaml
logging:
  level:
    root: INFO
    org.hibernate.SQL: DEBUG           # 실행된 SQL 출력
    org.hibernate.type.descriptor.sql: TRACE  # 바인딩 파라미터 출력
```

## 사용 방법
- Lombok `@Slf4j` 어노테이션으로 `log` 필드 자동 생성
- Repository 구현체에서 invalid key 처리 시 `log.warn(...)` 사용 중

```java
@Slf4j
public class UserRepositoryImpl {
    // log.warn, log.info, log.debug 사용
}
```

## 원칙
- 비즈니스 로직 에러/경고는 `log.warn` 또는 `log.error`
- 디버그용 상세 정보는 `log.debug` (운영 환경에서 노출 최소화)
- `System.out.println` 사용 금지 — 반드시 `log.*` 사용
