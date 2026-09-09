# keycloak-user-storage

Keycloak User Storage Provider가 사용자 인증 시 호출하는 REST API 백엔드. Spring Boot 3.3.4 + H2 in-memory + JPA.
USP 규격: `keycloak-extension-spi/docs/06-usp-integration-guide.md`

---

## 개발 워크플로우

모든 기능 작업은 spec-driven harness를 거친다. 새 기능은 `/spec`부터 시작한다.

```
/onboard → /spec → /spec-review → /plan → /build T-NNN → /validate → /review → (사람이) commit
```

- 워크플로우 진입점 · 강제 hook 목록: [`.claude/README.md`](.claude/README.md)
- 7-phase 방법론 상세: [`.claude/docs/methodology.md`](.claude/docs/methodology.md)
- 기능 인덱스 · 완료 이력: [`.specs/README.md`](.specs/README.md)
- 리포지토리 분류 · harness 기준선: [`.specs/_onboarding.md`](.specs/_onboarding.md)

코딩 컨벤션은 `.claude/skills/`에 있고 각 command가 자동 로드한다. 여기 다시 적지 않는다.

## 강제 규칙 (hook이 위반을 차단)

- **No invention** — 티켓/대화/코드에 없는 값은 임의로 정하지 않는다. `Q-NNN`으로 기록하고 사용자에게 묻는다.
- **Phase-exit 게이트** — 미해결 `Q-NNN`이 있으면 `src/**` 편집 불가.
- **TDD by construction** — 실패 테스트를 먼저 쓰고 `.tdd-state.json`에 기록해야 `src/main/**` 수정 허용.
- **한 태스크씩**, 태스크마다 커밋. agent는 자동 커밋하지 않는다 — 사용자가 명시적으로 요청할 때만 `git commit`. `git push`는 금지.
- **Brownfield ratchet** — 게이트는 `.specs/_baseline.json` 대비 회귀만 차단. 신규/변경 라인은 커버리지 ≥95%.

## 코드 불변 규칙

- 레이어 경계: Controller→Service→Repository 만 직접 호출
- `User.attributes`는 절대 null 아님 (`new HashMap<>()` 초기화) — null 시 USP NPE
- `LocalDateTime`은 `CustomLocalDateTimeSerializer`로 `yyyy-MM-dd'T'HH:mm:ss` 직렬화
- 보일러플레이트는 Lombok. 수동 getter/setter 금지
- DB는 H2 in-memory, Hibernate `update` DDL, 스키마/마이그레이션 파일 없음
- **이모지 사용 금지** — 코드/주석/커밋/문서/로그 어디에도

---

## 실행

```bash
./gradlew build
./gradlew bootRun                     # port 8081 (dev 프로파일: 8080)
.claude/scripts/harness.sh            # 자체 검증 harness
make docker-push                      # harbor.kind.internal/library/keycloak-user-storage:<git-sha>
```

- API `http://localhost:8081` · H2 Console `/h2-console` (`jdbc:h2:mem:testdb`, `sa` / `password`)
- 내부 Maven 저장소 `http://reposilite.kind.internal/releases` — 외부 네트워크에서 빌드 실패 가능
- 현재 `main` 기준선에 선행 테스트 실패 3건 (`otpMethod` 시드 drift). `.specs/_baseline.json`에 캡처됨

---

**Last Updated**: 2026-09-08 (spec-driven harness 도입) · **Main Branch**: main
