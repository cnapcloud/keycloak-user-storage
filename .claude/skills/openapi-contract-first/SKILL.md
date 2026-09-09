---
name: openapi-contract-first
description: Define the API contract (request + success + error examples, status codes) before writing code. Lives as an OpenAPI sketch in 03-design.md. Use in /plan.
---
# Contract-first (API before code)

Define the contract **before** implementation. The contract is request + response examples and status codes — nothing more. It lives in `03-design.md` under `## API Contract`.

## Minimal contract (per endpoint, < 50 lines)
```json
{
  "endpoint": "PATCH /user/{id}/attributes",
  "request": { "example": { "phoneNumber": "010-1234-5678", "lastLoginDate": null } },
  "success_response": { "status": 204, "body": null },
  "error_response":   { "status": 404, "example": { "error": "user not found" } }
}
```

## This project's conventions (do not re-decide these)
- GET → 200 / 404. POST → 201 (+ `{id, username, email}`) / 409.
- DELETE, PUT, PATCH success → **204 No Content**, no body.
- Error envelope: `{"error": "..."}`. Raised via `ResponseStatusException` (no global `@ControllerAdvice` yet).
- Dates: `yyyy-MM-dd'T'HH:mm:ss` via `CustomLocalDateTimeSerializer`.
- `attributes` is always a JSON object, never null.

## Don't over-spec
No auth flows, rate limiting, or an exhaustive status-code matrix in the contract. Just the shapes the feature actually introduces.

## When a full OpenAPI file exists later
Once `springdoc` is wired (see `.claude/docs/harness-gradle.md`), `/validate` diffs the generated `openapi.yaml` against `origin/main` and flags breaking changes. Until then the sketch in `03-design.md` is the contract.
