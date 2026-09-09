# Implementation Log: <FEATURE-ID>

> Owner: `test-engineer` (red) + `implementer` (green/refactor/simplify) · Phase 4
> One block appended per phase per task. Newest at the bottom.

---

### T-001 — red
- when: <YYYY-MM-DDThh:mm:ssZ>
- test: `com.keycloak.userstorage.<Class>.<method>` — `@Tag("AC-001")`
- command: `./gradlew test --tests 'com.keycloak.userstorage.<Class>.<method>'`
- result: FAIL (expected)
- excerpt:
  ```
  <up to 10 lines of the failure>
  ```

### T-001 — green
- files touched: <paths>
- command: `./gradlew test`
- result: PASS — <n> tests, 0 failures

### T-001 — refactor
- changes: <extract method / rename / move to internal>
- result: PASS — suite green

### T-001 — simplify
- changes: <clarity-over-cleverness moves>
- result: PASS — suite green
- `.tdd-state.json`: T-001 phase = done
