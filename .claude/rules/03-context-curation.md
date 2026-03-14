# Rule 03: Context Curation (< 100KB)

**Principle**: Give AI only the files it needs, under 100KB total.

---

## Why Limit Context?

More files does not mean better results:

| Context size | Outcome |
|-------------|---------|
| 100KB focused | AI understands quickly |
| 500KB broad | AI gets confused |
| 1MB+ entire repo | AI produces hallucinations |

---

## What to Include

Must have:
1. Contract JSON (`01-contract-first.md`)
2. Reference code — 1-2 files with similar implementation
3. Type definitions — only what this slice uses

Do not include:
- Entire folder structures
- Config files (tsconfig, vite.config, etc.)
- node_modules or dist
- Test files (unless writing tests)
- Design documents other than the contract

---

## Example: Keycloak Adapter Slice

Files to prepare:

```
.claude/0-plan/step8-contract.json       (2 KB) — contract
backend/src/adapters/jenkins.ts          (15 KB) — reference
backend/src/adapters/types.ts            (5 KB)  — types
backend/src/api/routes/admin/tools.ts    (10 KB) — route pattern

Total: ~32 KB

Exclude:
  - backend/ (entire folder)
  - frontend/ (not needed)
  - All test files
  - prisma/ (reference schema.prisma only if needed)
```

Prompt to AI:
> "Here's the contract and jenkins.ts as reference. Implement keycloak.ts following the same pattern."

---

## Checklist

Before asking AI to implement:

```
[ ] Contract is prepared
[ ] Reference code selected (1-2 files)
[ ] Reference code provided in full (not summarized)
[ ] Types included (only what's needed)
[ ] Total file size < 100KB
[ ] AI can implement from this alone
```

---

## Common Mistake

Too much context:
```
"Here's the entire backend/ (500KB), all types/ (50KB),
 and the design doc (200KB). Now implement the adapter."
```
Result: Multiple revisions, slow, confused.

Right amount of context:
```
"Here's the contract.
 Here's jenkins.ts as reference.
 Implement keycloak.ts the same way."
```
Result: Fast, minimal revisions.

---

## See Also

- `01-contract-first.md` - What to include in contract
- `02-vertical-slicing.md` - What each slice needs
- `.claude/conventions/` - Patterns reference
