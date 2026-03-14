# Rule 02: Vertical Slicing (15-min Chunks)

**Principle**: Break work into 15-minute end-to-end slices (DB + API + UI).

---

## What is Vertical Slicing?

Instead of:
```
Day 1: Build entire backend
Day 2: Build entire frontend
Day 3: Test together
```

Do:
```
Slice 1 (15 min): Database table + API GET + UI display
Slice 2 (15 min): API PUT + UI form + validation
Slice 3 (15 min): Delete feature + confirm dialog
```

Each slice is complete and testable independently.

---

## Slice Size Guide

| Size | Assessment |
|------|------------|
| 5 min | Too small — merge with next slice |
| 15 min | Ideal |
| 1 hour | Too big — split further |

---

## Slice Template

```
Slice N: [Short description]

Database:
  - Table: [name], fields: [list]

API:
  - Endpoint: [method] [path]
  - Contract: [success/error format]

Frontend:
  - Component: [name]
  - Hook: [name if needed]

Acceptance:
  - [ ] DB migration runs
  - [ ] curl test passes (200 OK)
  - [ ] UI renders correctly
  - [ ] Data persists after page refresh

Estimate: 15 min
```

---

## Example: Favorites Feature

**Slice 1: Add/remove favorite**

```
Database:
  - Table: user_favorites (user_id, tool_id, created_at)

API:
  - PUT /api/favorites/:toolId
  - Response: { success, toggled: boolean }

Frontend:
  - Component: ToolCard star icon
  - State: isFavorite (from API)

Acceptance:
  - [ ] curl -X PUT http://localhost:3000/api/favorites/jenkins
  - [ ] Response: { success: true, toggled: true }
  - [ ] Star icon toggles on click
  - [ ] DB shows new row

Estimate: 15 min
```

**Slice 2: Filter view**

```
Frontend:
  - Component: Favorites toggle button
  - Logic: Filter tool list by favorites

API:
  - GET /api/favorites

Acceptance:
  - [ ] Toggle button visible
  - [ ] Clicking shows/hides non-favorite tools
  - [ ] Filters correctly after page refresh

Estimate: 15 min
```

---

## Red Flags — Slice Too Big

- Slice description includes "entire feature X"
- Estimate exceeds 1 hour
- Contains "test this later"
- UI depends on backend being finished first

If any apply — split into smaller slices.

---

## Execution Order

1. Slice 1: Code DB + API → curl test → add UI → commit
2. Slice 2: Code next endpoint → extend UI → test → commit
3. Slice 3: Final feature → polish → full flow test → commit

Each slice is deployable on its own.

---

## Checklist

Before starting a slice:

```
[ ] Description is 1-2 sentences
[ ] DB changes are clear
[ ] API contract is defined
[ ] Frontend component is identified
[ ] Acceptance criteria are testable
[ ] Estimate is 15 min
```

---

## See Also

- `01-contract-first.md` - Define API before slicing
- `03-context-curation.md` - What files to use per slice
- `.claude/conventions/04-layer-structure.md` - DB → API → UI order
