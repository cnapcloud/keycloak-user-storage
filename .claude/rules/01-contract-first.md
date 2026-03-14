# Rule 01: Contract-First (API Before Code)

**Principle**: Define API contract (JSON) BEFORE writing code.

---

## What is a Contract?

Minimal JSON showing:
1. Endpoint (URL + method)
2. Request example
3. Success response example
4. Error response example

No more than 50 lines.

---

## Template

```json
{
  "endpoint": "PUT /api/admin/tools/:toolId/test-connection",
  "request": {
    "example": {
      "username": "admin@example.com",
      "password": "secretpass",
      "url": "https://service.example.com"
    }
  },
  "success_response": {
    "example": {
      "success": true,
      "adapter": {
        "message": "connection.test_passed",
        "details": {
          "groups": ["admin", "developers"]
        }
      }
    }
  },
  "error_response": {
    "example": {
      "success": false,
      "error": "Invalid credentials",
      "code": "INVALID_CREDENTIALS"
    }
  }
}
```

---

## Why Before Code?

```
Without contract:
  AI: "Should I use PUT or POST? What's the response format?"
  User: "Hmm, maybe..."
  AI: Codes PUT
  User: "Actually, I wanted POST and different format"
  Result: Rework, 2 hour waste

With contract:
  User: "Here's what I want"
  AI: "Got it, implementing..."
  Result: 1 hour saved
```

---

## Where to Save

`.claude/0-plan/step[N]-contract.json`

---

## Don't Over-Spec

Too much (avoid):
```json
{
  "endpoint": "...",
  "http_status_codes": [200, 201, 400, 401, 403, 404, 409, 500],
  "authentication": { "...detailed OAuth flow..." },
  "rate_limiting": { "..." }
}
```

Just right:
```json
{
  "endpoint": "...",
  "request": { "example": {} },
  "success_response": { "example": {} },
  "error_response": { "example": {} }
}
```

---

## Checklist

Before implementation starts:

```
[ ] Endpoint URL is clear (HTTP method + path)
[ ] Request example shows all required fields
[ ] Success response example is realistic
[ ] Error response example is realistic
[ ] Contract is < 50 lines
[ ] Readable in under 2 minutes
```

---

## See Also

- `02-vertical-slicing.md` - Break into slices after contract
- `03-context-curation.md` - Select files for implementation
- `.claude/conventions/` - Response format reference
