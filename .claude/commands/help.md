---
description: Read-only — print the command catalog and phase order. See .claude/commands/help.md.
argument-hint: "[command-name]"
---
# /help — meta (read-only)

## Purpose
커맨드 카탈로그와 권장 phase 순서를 출력한다. 인자를 주면 그 커맨드 하나를 자세히 설명한다.
owning agent 없이 자체완결로 동작한다.

## Inputs
- `[command-name]` — 선택, 슬래시 없이

## Outputs
- 없음 — 화면 출력만

## Reads
- `.claude/commands/`
- `.claude/commands/<command-name>.md` if a name was supplied
- `.claude/docs/methodology.md`

## Process
No argument → print this table and the natural-language alias list:

| Command | Phase | Produces |
|---|---|---|
| `/onboard` | 0 | `.specs/_onboarding.md`, `_stack.json`, `_baseline.json` |
| `/spec` | 1 | `01-spec.md` |
| `/spec-review` | 2 | `02-spec-review.md` |
| `/plan` | 3 | `03-design.md`, `04-tasks.md`, `adr/`, `.tdd-state.json` |
| `/build T-NNN` | 4 | code + tests, `05-implementation-log.md` |
| `/validate` | 5 | `07-validation-report.md`, `07a-traceability.md` |
| `/review` | 6 | `08-code-review.md` |
| `/status` | meta | pipeline table (read-only) |
| `/help` | meta | this catalog |

Order: `/onboard → /spec → /spec-review → /plan → /build T-001 … → /validate → /review → commit`.

With an argument → print that command's **Purpose / Inputs / Outputs / Next** sections, then its owning agent's Process / Refuse if / Done when / Hard rules / Handoff from `.claude/agents/<name>.md` (the thin command file points there; `/status` and `/help` have no agent).

## Refuse if
Never.

## Done when
Help text is rendered.
