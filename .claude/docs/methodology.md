# Methodology — Spec-Driven Development for keycloak-user-storage

Loiane Groner의 [specs-driven-development-spring-angular](https://github.com/loiane/specs-driven-development-spring-angular)
방법론을 이 백엔드 서비스(Gradle · Java 17 · Spring Boot 3.3.4 · H2)에 맞게 이식한 것.
UI/프론트엔드 영역은 제외.

## 핵심 원칙

1. **Spec 먼저.** 모든 기능은 `.specs/<feature-id>/` 아래 번호가 매겨진 산출물 체인을 거친다.
2. **No invention.** 요청·대화·코드에 없는 값(필드명, 기본값, 에러 시맨틱, SLA, 상태코드…)은
   임의로 정하지 않는다. `Q-NNN`으로 기록하고 사용자에게 묻는다.
3. **Phase-exit 게이트.** 미해결 `Q-NNN`이 있으면 다음 단계로 못 넘어간다 (`block-progress-on-open-questions` hook).
4. **TDD by construction.** `/build`는 실패하는 테스트를 먼저 쓰고
   `.tdd-state.json`에 `red_failure_excerpt`를 기록해야 `src/main/**` 수정이 허용된다
   (`block-impl-without-failing-test` hook).
5. **Self-validating harness.** `/validate`는 `.claude/scripts/harness.sh`가 만든 리포트를 읽고
   결정론적 verdict를 낸다. 추측 금지.
6. **커밋은 사람이.** 어떤 agent도 자동으로 `git commit` 하지 않는다. 사용자가 명시적으로 요청하면 그때만 수행한다 (`git push`는 여전히 금지).

## 7단계 (+ Phase 0)

```
/onboard → /spec → /spec-review → /plan → /build T-NNN → /validate → /review → commit
  (0)       (1)        (2)          (3)     (4, TDD 반복)     (5)         (6)
```

| Phase | Command | Owner agent | 산출물 |
|---|---|---|---|
| 0 bootstrap | `/onboard` | `architect` (onboarding hat) | `.specs/_stack.json`, `_baseline.json`, `_onboarding.md` |
| 1 specify | `/spec` | `spec-author` (author hat) | `01-spec.md` |
| 2 review | `/spec-review` | `spec-author` (review hat) | `02-spec-review.md` |
| 3 plan | `/plan` | `architect` (design hat) | `03-design.md`, `04-tasks.md`, `adr/`, `.tdd-state.json` |
| 4 build | `/build T-NNN` | `test-engineer` (red) + `implementer` (green/refactor/simplify) | code + tests, `05-implementation-log.md` |
| 5 validate | `/validate` | `validator` | `07-validation-report.md`, `07a-traceability.md` |
| 6 review | `/review` | `validator` (review hat) | `08-code-review.md` |

(`06-test-plan.md`은 이 lean 편성에서 생략 — 번호는 호환을 위해 비워둠. 커버리지/traceability 갭은 별도 문서 없이 처리한다: `/validate`가 `07-validation-report.md`에 `Gap-NNN`으로 식별 → `/plan`이 이를 덮는 gap task를 `04-tasks.md`에 추가 → `/build`가 정규 red→green 루프로 닫는다. 대응 AC가 없는 orphan 라인은 `Q-NNN`으로 스펙에 반송.)

**command와 agent의 분리.** `commands/*.md`는 얇다 — frontmatter(`agent:`) + Phase + Purpose + Inputs만.
절차 · 거부조건 · 완료조건은 owning agent(`agents/*.md`)에 있고, agent는 커맨드 없이 직접 호출돼도 자립 실행된다.
`spec-author` · `architect` · `validator`는 각각 두 hat을 담고 커맨드가 hat을 지정한다.
`/build`만 예외 — 두 agent를 지휘하므로 메인 세션이 오케스트레이션하고 `commands/build.md`가 그 순서를 담는다.
`/status` · `/help`는 owning agent 없이 자체 완결.

## `.specs/` 레이아웃

```
.specs/
├── README.md            # 기능 인덱스 + 완료 이력(BL-01~10)
├── _onboarding.md        _stack.json        _baseline.json
└── YYYY-MM-DD-<feature-id>/
    ├── 01-spec.md   02-spec-review.md   03-design.md   04-tasks.md
    ├── 05-implementation-log.md   07-validation-report.md   07a-traceability.md   08-code-review.md
    ├── .tdd-state.json
    └── adr/ADR-NNN-*.md
```

## `.tdd-state.json`

`/plan`이 생성, `/build`가 phase를 전이시킨다. `phase`: `pending → red → green → refactor → simplify → done`.
`active_task`는 태스크 사이에서 `null`. hook 3종이 이 파일을 읽어 `src/main/**` 편집을 강제한다.

## 강제 장치 (hooks — `.claude/settings.json`)

| Hook | 이벤트 | 차단 조건 |
|---|---|---|
| `block-impl-without-failing-test.sh` | Edit/Write | 활성 `.tdd-state.json` 없이 / phase≠red계열 / `red_failure_excerpt` 비어있음 / scope 밖 파일 → `src/main/**` 수정 차단 |
| `enforce-files-in-scope.sh` | Edit/Write | 활성 태스크 진행 중 `src/**` 편집이 `files_in_scope` 밖 |
| `block-progress-on-open-questions.sh` | Edit/Write | 활성 feature의 `01/03/04` 문서에 미해결 `Q-NNN` 있는데 `src/**` 편집 |
| `forbid-skip-flags.sh` | Bash | `./gradlew -x test`, `-Dpitest.skip`, `--no-verify` 등 |
| `route-natural-language-aliases.sh` | UserPromptSubmit | (차단 아님) "스펙 짜줘" → `/spec` 힌트 |

## 이 프로젝트 고유 규칙 (다른 Spring 프로젝트 의견을 들여오지 말 것)

- `ResponseEntity` 반환은 이 프로젝트에서 정상. Loiane의 "avoid ResponseEntity" 규칙은 적용 안 함.
- 에러: `ResponseStatusException`, DELETE/PATCH 성공은 204, 중복은 409, 봉투 `{"error": "..."}`.
- `User.attributes`는 절대 null 아님 (`new HashMap<>()`), `FetchType.EAGER`.
- 날짜: `LocalDateTime` + `CustomLocalDateTimeSerializer` (`yyyy-MM-dd'T'HH:mm:ss`).
- 동적 쿼리: Criteria API (`UserRepositoryImpl`).
- H2 in-memory, `ddl-auto=update` — 마이그레이션 파일 없음.
