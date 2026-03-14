# .claude/rules/ - AI Development Workflow

**Purpose**: Constraints for how AI works on this project.

These are workflow rules for planning and implementation — not coding style rules.

---

## Rules

### 1. Contract-First
- Define API contract (JSON) before writing code
- Contract = Request + Response examples only
- Share with user before implementation

See: `01-contract-first.md`

### 2. Vertical Slicing
- Break work into 15-min end-to-end slices
- Each slice: DB + API + UI (complete and testable)
- Test each slice before starting the next

See: `02-vertical-slicing.md`

### 3. Context Curation
- Give AI only the files it needs (< 100KB total)
- Do not dump the entire repo
- Include: contract, reference code, relevant type defs

See: `03-context-curation.md`

### 4. Question-First
- Ask all clarifying questions upfront
- Do not: Question → partial answer → implement → ask again
- Do: Question → full answer → implement (once)

### 5. Memory-Driven
- Save learnings to MEMORY.md after each step
- Reuse patterns from past work
- Document gotchas for future sessions

---

## Workflow: Starting a New Step

```
1. User asks "Step X: [description]"

2. AI asks clarifying questions
   - Acceptance criteria?
   - Scope (backend only? full stack?)
   - Dependencies?

3. AI proposes plan
   - Contract (if API involved)
   - Vertical slices (15-min chunks)
   - Reference files to use

4. User approves

5. AI implements slice by slice
   - Build + test each slice
   - Update MEMORY.md with learnings

6. Done
   - All slices complete
   - Acceptance criteria verified
   - MEMORY.md updated
```

---

## Rule Reference by Situation

| Situation | Rule |
|-----------|------|
| Starting new feature | Contract-First + Vertical Slicing |
| Proposing implementation | Context Curation |
| Requirements unclear | Question-First |
| Implementing | Vertical Slicing |
| Feature complete | Memory-Driven |

---

## Key Principle

> "Full clarification upfront, then fast execution."
>
> Not: Plan → Code → Ask → Rework
>
> But: Ask → Clarify → Plan → Code (once)

---

## See Also

- `.claude/conventions/` - How we write code
- `CLAUDE.md` (root) - Project quick start
- `memory/MEMORY.md` - Proven patterns and gotchas
