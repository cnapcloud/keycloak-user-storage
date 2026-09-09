# Onboarding — keycloak-user-storage

**Phase 0 산출.** `/onboard` (agent: `architect`, onboarding hat).
갱신: `.claude/scripts/detect-stack.sh` + `.claude/scripts/harness.sh --baseline` 재실행.

_최초 캡처: 2026-09-08 · git `4c36ad4` · 브랜치 `chore/spec-driven-harness`_

---

## 분류

**Brownfield.** `src/main/java` 19개 파일 (Controller 2 · Service 4 · Repository 4 · Model 3 · Config 4 · utils 1 · 진입점 1),
`src/test/java` 1개 (`UserStorageIntegrationTest` — `@SpringBootTest(RANDOM_PORT)`, 38 케이스).

단일 모듈 Gradle. 프로덕션 코드는 성숙 상태 (BL-01~10 완료). harness/spec 체인만 신규 도입.

---

## 스택 (`_stack.json`)

| | |
|---|---|
| build | Gradle (`./gradlew`) |
| java | 17 |
| Spring Boot | 3.3.4 |
| DB | H2 in-memory, `ddl-auto=update` — **마이그레이션 파일 없음** |
| test | JUnit Platform |

---

## 기준선 게이트 (`_baseline.json`)

| 레이어 | 상태 | 비고 |
|---|---|---|
| unit | **fail** | 38 테스트 · **3 실패** — 아래 "선행 실패" |
| coverage | skipped | `build.gradle` `jacocoTestReport { reports.xml.required = true }` 미설정 |
| archunit | skipped | 플러그인/규칙 미배선 |
| checkstyle | skipped | 플러그인 미배선 |
| spotbugs | skipped | 플러그인 미배선 |
| mutation | skipped | pitest 미배선 |

> **브라운필드 래칫**: 이후 모든 `/validate`는 이 표 대비 **회귀만** 차단한다.
> 현재 3개 실패는 "day-one 차단"이 아니라 기준선. 신규/변경 코드는 풀 기준
> (변경 라인 커버리지 ≥95%) 적용 — `.claude/skills/jacoco-coverage-policy/SKILL.md`.

### 선행 실패 3건 (harness 도입과 무관)

`UserStorageIntegrationTest`:

- `search_byAttributeKey_otpMethod_returnsAllUsers` — `otpMethod=SKIP` 16 기대 / 실제 14
- `count_byAttributeKey_otpMethod_returns17` — 동일 원인
- `search_combinedFieldAndAttribute_returnsIntersection` — 교집합 1 기대 / 실제 0

원인: 커밋 `8bdd8d6` (시드 사용자 `otpMethod` SKIP→SMS 2건)이 테스트 단언 미갱신.
→ 권장 첫 후속 `/spec`: **`fix-otp-seed-test-drift`** (시드 데이터 ↔ 단언 정합, `_baseline.json` ratchet down).

---

## 미배선 레이어 (Findings)

`_stack.json.harness_layers`에서 `false` — 배선 방법은
[`../.claude/docs/harness-gradle.md`](../.claude/docs/harness-gradle.md):

| 레이어 | 필요 | 차단 요소 |
|---|---|---|
| coverage | `jacocoTestReport { reports.xml.required = true }` | 없음 — 즉시 가능 |
| checkstyle | `checkstyle` 플러그인 + `config/checkstyle/checkstyle.xml` | 내부 저장소 플러그인 미러 확인 |
| spotbugs | `com.github.spotbugs` 플러그인 + `config/spotbugs/exclude.xml` | 〃 |
| archunit | `com.tngtech.archunit:archunit-junit5` + `ArchitectureTest.java` | `.claude/skills/archunit-rules/SKILL.md` 규칙 확정 |
| mutation | `info.solidsoft.pitest` 플러그인 + `targetClasses` | 내부 저장소 플러그인 미러 확인 |
| openapi | `org.springdoc.openapi-gradle-plugin` + `springdoc-openapi-starter-webmvc-api` | 〃 |

→ 후속 feature: `.specs/`에 `wire-gradle-harness` 행 (README 참조).

---

## 권장 시작

1. **`/spec "시드 otpMethod 테스트 정합"`** — 선행 실패 3건 해소 (가장 작은 수직 슬라이스, harness end-to-end 검증 겸용).
2. 이후 신규 기능은 `.specs/README.md` "진행 중 / 예정" 표에 행 추가 후 `/spec`.

**다음 명령:** `/spec`
