# .claude/ — Spec-Driven Development harness

이 서비스는 **harness 기반 spec-중심 워크플로우**로 개발한다.
전체 방법론: [`docs/methodology.md`](docs/methodology.md).

## 진입점

```
/onboard → /spec → /spec-review → /plan → (/build T-NNN → commit) ×N → /validate → /review → commit
```

새 작업을 시작할 때:
1. `.specs/README.md`에서 다음 기능(백로그 행)을 고른다.
2. `/spec "<설명 또는 백로그 행>"` — `01-spec.md` 초안. 모르는 값은 `Q-NNN`으로 남기고 답을 받는다.
3. `/spec-review` — 체크리스트 통과(`PASS`)해야 다음.
4. `/plan` — `03-design.md` + `04-tasks.md` + `adr/` + `.tdd-state.json`.
5. `/build T-001` … 각 태스크를 red→green→refactor→simplify로. **태스크마다 커밋.**
6. `/validate` → `/review` → 사람이 `git commit`.

## 디렉터리

| 경로 | 내용 |
|---|---|
| `commands/` | 슬래시 커맨드 진입점 — 얇음. frontmatter(`agent:`) + `## Purpose / Inputs / Outputs / Next` 섹션 + owning agent 포인터. 절차·거부조건·완료조건은 agent에 있다. `/build`만 fat(+`## Orchestration`), `/status`·`/help`는 agent 없이 자체 완결 |
| `agents/` | 실행자 — 자립적. Role + Process(hat별) + Skills + Hard rules + Handoff. `spec-author`, `architect`, `implementer`, `test-engineer`, `validator`. 커맨드 없이 직접 호출돼도 동작 |
| `skills/` | 방법론 + 이 프로젝트 컨벤션 + harness 파싱 규칙 |
| `templates/` | `.specs/<feature>/` 산출물 스켈레톤 |
| `checklists/` | phase-exit 게이트 체크리스트 |
| `hooks/` | 강제 장치 (아래) |
| `scripts/` | `harness.sh`, `detect-stack.sh`, `check-new-code-coverage.sh`, `traceability.sh` |
| `docs/` | `methodology.md`, `spec-format.md`, `harness-gradle.md` |

### command과 agent의 분리

- **command** = 슬래시 등록 + hat 선택 + **`## Purpose / Inputs / Outputs / Next` 계약**. 사용자가 먼저 보는 계약서. 절차는 재서술하지 않고 owning agent를 가리킨다.
- **agent** = 절차의 유일 정본 (Process / Refuse if / Done when / Skills / Hard rules / Handoff). `<feature-id>` 생략 시 해석 등 의미론도 agent가 소유 — 커맨드는 요약만. `spec-author`·`architect`·`validator`는 각각 두 hat을 담고, 커맨드가 hat을 지정한다.
- `/build`만 예외 — `test-engineer`(red) → `implementer`(green/refactor/simplify) 두 agent를 지휘하는 로직이 커맨드에 있어 `commands/build.md`는 fat 유지 (계약 4줄 + `## Orchestration`).
- 이모지 금지 (`CLAUDE.md` 코드 불변 규칙). verdict는 `PASS`/`WARN`/`FAIL` 텍스트.

## 강제 장치 (hooks — `settings.json`에 등록됨)

- **`block-impl-without-failing-test.sh`** — 실패 테스트(`.tdd-state.json` phase=red + `red_failure_excerpt`) 없이 `src/main/**` 수정 불가.
- **`enforce-files-in-scope.sh`** — 활성 태스크의 `files_in_scope` 밖 `src/**` 편집 불가.
- **`block-progress-on-open-questions.sh`** — 활성 feature에 미해결 `Q-NNN`이 있으면 `src/**` 편집 불가.
- **`forbid-skip-flags.sh`** — `./gradlew -x test`, `-Dpitest.skip`, `--no-verify` 등 차단.
- **`route-natural-language-aliases.sh`** — "스펙 짜줘" 같은 표현을 슬래시 커맨드로 안내(차단 아님).

## 전제 조건

- `bash`, `git`, `jq`, `python3` 가 PATH에 있어야 한다 (hooks + scripts).
- Gradle wrapper (`./gradlew`), Java 17.

## Harness 현황

`harness.sh`는 지금 **unit** 게이트만 활성(+`jacocoTestReport` XML 켜면 coverage).
checkstyle/spotbugs/archunit/mutation/openapi/owasp는 `skipped` — 설정 패치는
[`docs/harness-gradle.md`](docs/harness-gradle.md)에 있고, 필요할 때 개발자가 직접
적용한다. `skipped`는 실패가 아니다.

## 이관 이력

구 `.claude/{rules,conventions,backlogs,plans}/`는 이 구조로 흡수되었다:
- `rules/` → `docs/methodology.md` + `skills/{vertical-slicing,context-curation,openapi-contract-first}/`
- `conventions/` → `skills/{spring-layer-conventions,spring-error-handling,spring-logging,usp-integration-testing}/`
- `backlogs/` + `plans/` → `.specs/README.md` "완료 이력" 표 (상세는 git history)
