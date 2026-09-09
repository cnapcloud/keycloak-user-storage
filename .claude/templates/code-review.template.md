# Code Review: <FEATURE-ID>

> Owner: `validator` (review hat) · Phase 6 · Template: `.claude/templates/code-review.template.md`
> Diff: `git diff origin/main...HEAD` · Validation verdict: <PASS/WARN>

## Final verdict
**<Approve | Approve with waivers | Request changes>**

## Findings
| ID | Severity | File:line | Finding | Suggested change |
|----|----------|-----------|---------|------------------|
| F-001 | must-fix / should-fix / nit / praise | <path:line> | <what> | <how> |

## Rubric section results
> From `.claude/skills/code-review-rubric/SKILL.md`.

| Section | Result | Notes |
|---------|--------|-------|
| 1 Traceability | ok / findings | |
| 2 Layer boundaries | | |
| 3 Spring idioms | | |
| 4 Error handling | | |
| 5 Data access | | |
| 6 Dates & serialisation | | |
| 7 Test quality | | |
| 8 Clarity | | |
| 9 Migration / contract | | |

## Summary
- must-fix: <n> · should-fix: <n> · nit: <n> · praise: <n>
- Waivers: <none | F-NNN → ADR-NNN>

## Next action
- PASS / WARN → suggested commit message:
  ```
  <type>: <summary>

  Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
  ```
  User runs `git commit` — the agent never commits.
- FAIL → address must-fix findings, then re-run `/validate` and `/review`.
