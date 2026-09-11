# Configuring the harness

`.claude/scripts/harness.sh` is this repo's self-validation harness — the gate
runner behind `/validate`. It bundles the project's verification layers (tests,
coverage, static analysis, ...) behind one script.

---

## What the harness runs

`harness.sh` is invoked by `/validate` (the main path), by `/onboard --baseline`,
or manually. On each run it inspects `build.gradle`, executes the layers that are
configured there in order, and parses each layer's report into a per-gate
`{status, ...}` in `build/harness-summary.json`, which `/validate` then turns into
its verdict.

The layers, in run order:

- **unit** — run the JUnit test suite.
- **coverage** — JaCoCo line/branch coverage from the test run.
- **checkstyle** — code-style rules on source and tests.
- **spotbugs** — static bug pattern analysis on compiled classes.
- **archunit** — architecture/layering rules, checked as JUnit tests.
- **mutation** — PIT mutation testing (kill rate of the test suite).
- **openapi** — regenerate the OpenAPI spec and diff it for breaking changes.
- **owasp** — OWASP Dependency-Check scan for known-vulnerable dependencies.

A layer whose plugin is not in `build.gradle` is reported as `"skipped"`, which is
**not a failure** — `/validate` enforces only the configured layers.

### Verdict

`/validate` reads the per-gate `status` from `build/harness-summary.json` (not the
gradle exit code) and compares each against `.specs/_baseline.json`:

| condition | verdict |
|---|---|
| every active gate `pass`, no regression vs baseline | PASS |
| gates pass but below an absolute target, no regression, waivers cite an ADR | WARN |
| any active gate `fail`, a regression vs baseline, or an AC with no test | FAIL |

`skipped` is neither pass nor fail. A regression = that gate worse than the baseline
value, so baseline failures pass through until `--baseline` is re-run to ratchet them
out. Full rules: `.claude/skills/harness-report-parsing/SKILL.md`.

---

## Adding a layer

Each section below adds one layer, and they are independent — apply them in any
order (patch 0 is the smallest, so start there). The plugins and dependencies must
be resolvable from a public or internal repository the build host can reach. After
applying a layer, re-run `/onboard` (or `harness.sh --baseline`) so `.specs/_stack.json` and
`.specs/_baseline.json` pick up the new gate.

### 0. Enable JaCoCo XML
```groovy
jacocoTestReport {
    dependsOn test
    reports { xml.required = true; html.required = true }
}
test.finalizedBy jacocoTestReport
```
Unblocks `check-new-code-coverage.sh` and the `coverage` gate in `harness.sh`.

### 1. JaCoCo verification (brownfield ratchet)
```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { counter = 'LINE';   minimum = 0.00 }   // set from .specs/_baseline.json at onboard
            limit { counter = 'BRANCH'; minimum = 0.00 }
        }
    }
}
check.dependsOn jacocoTestCoverageVerification
```

### 2. Checkstyle
```groovy
plugins { id 'checkstyle' }
checkstyle {
    toolVersion = '10.17.0'
    configFile = file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0            // ratchet: raise the bar, never lower
}
```
Add `config/checkstyle/checkstyle.xml` (start from Google or Sun checks, trimmed).
Report consumed: `build/reports/checkstyle/*.xml`.

### 3. SpotBugs
```groovy
plugins { id 'com.github.spotbugs' version '6.0.18' }
spotbugs {
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter = file("config/spotbugs/exclude.xml")
}
tasks.named('spotbugsMain') { reports { xml.required = true } }
```
Add `config/spotbugs/exclude.xml` (exclude generated / DTO false positives).
Report consumed: `build/reports/spotbugs/*.xml`.

### 4. ArchUnit
```groovy
dependencies {
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
}
```
Add `src/test/java/<root-package-path>/ArchitectureTest.java` (this project:
`com/keycloak/userstorage`) implementing the rule set in
`.claude/skills/archunit-rules/SKILL.md`. Freeze pre-existing violations with
`FreezingArchRule.freeze(rule)`. Rules run inside the normal `test` task.

### 5. PIT mutation
```groovy
plugins { id 'info.solidsoft.pitest' version '1.15.0' }
pitest {
    targetClasses = ['<root-package>.*']   // this project: 'com.keycloak.userstorage.*'
    targetTests   = ['<root-package>.*']   // this project: 'com.keycloak.userstorage.*'
    threads = 4
    mutationThreshold = 0        // ratchet up from baseline; absolute target 75
    outputFormats = ['XML', 'HTML']
    timestampedReports = false
    historyInputLocation  = file("build/pitHistory.txt")
    historyOutputLocation = file("build/pitHistory.txt")
}
```
Report consumed: `build/reports/pitest/mutations.xml`.

### 6. OpenAPI (springdoc)
```groovy
plugins { id 'org.springdoc.openapi-gradle-plugin' version '1.9.0' }
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0'
}
openApi {
    outputDir = file("src/main/resources/openapi")
    outputFileName = "openapi.yaml"
}
```
`harness.sh` then diffs the generated `openapi.yaml` against `origin/main` and flags
breaking changes.

### 7. OWASP Dependency-Check (LAST — offline risk)
```groovy
plugins { id 'org.owasp.dependencycheck' version '10.0.4' }
dependencyCheck {
    formats = ['JSON', 'HTML']
    failBuildOnCVSS = 7.0
    nvd { datafeedUrl = '<internal NVD mirror or cached feed>' }
}
```
The analysis needs an NVD data feed the build host can reach. If no reachable feed,
keep this layer `skipped` and track CVEs manually.

---

## This project's current build configuration

- Applied `plugins`: `org.springframework.boot`, `io.spring.dependency-management`,
  `java`, `java-library`, `maven-publish`, `jacoco`.
- The only enforced gate is `unit`. Every other layer is `skipped`.
- The `jacoco` plugin is applied, so the `jacocoTestReport` task exists and runs —
  but without `reports.xml.required = true` it emits only HTML. The harness looks
  for the XML report, does not find it, and marks coverage `skipped`. Patch 0
  above turns it on.

---

