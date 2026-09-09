---
name: context-curation
description: Give each phase only the files it needs (< 100 KB) — contract, 1–2 reference files, relevant types. Use when an agent assembles reading context for /plan or /build.
---
# Context curation

More files ≠ better output. Focused context → fast, accurate work.

| Context size | Outcome |
|---|---|
| ~100 KB focused | agent understands quickly |
| ~500 KB broad | agent gets confused |
| whole repo | hallucinations |

## Include
1. The relevant `.specs/<feature>/` artifact (`01-spec.md` for /plan; `03-design.md` + `04-tasks.md` for /build).
2. 1–2 **reference files** with a similar existing implementation — in full, not summarised. Examples in this repo:
   - dynamic query → `repository/UserRepositoryImpl.java`
   - endpoint + error handling → `controller/UserController.java`
   - integration test style → `src/test/java/com/keycloak/userstorage/UserStorageIntegrationTest.java`
3. Only the type definitions the current task touches (`model/User.java`, `model/CredentialData.java`).

## Exclude
- Whole folder trees, `build/`, `bin/`, `.gradle/`.
- Build config (`build.gradle`) unless the task changes it.
- Unrelated tests.
- Design docs other than the active spec.

## Command "Reads" sections are the contract
Each `.claude/commands/*.md` lists exactly what that phase reads. Honour it — don't pull in more.
