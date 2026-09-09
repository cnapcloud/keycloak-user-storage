# Harness — Gradle wiring (NOT YET APPLIED)

> This document is the **input to a future feature**, not a live config.
> Wire it through its own workflow run: `/spec` → … under
> `.specs/<date>-wire-gradle-harness/`, one layer per task, verifying plugin
> resolution against the internal repo (`reposilite.kind.internal`) each step.
>
> Until then `.claude/scripts/harness.sh` reports these layers as `skipped`,
> which is not a failure.

## Current state (`build.gradle`)
- `plugins`: `org.springframework.boot`, `io.spring.dependency-management`, `java`, `java-library`, `maven-publish`, `jacoco`
- Active harness layers: **unit** (`test`), and **coverage** once the XML report is enabled (one-liner below).

## Layer-by-layer patch

### 0. Enable JaCoCo XML (smallest, do first)
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
Add `src/test/java/com/keycloak/userstorage/ArchitectureTest.java` implementing the rule
set in `.claude/skills/archunit-rules/SKILL.md`. Freeze pre-existing violations with
`FreezingArchRule.freeze(rule)`. Rules run inside the normal `test` task.

### 5. PIT mutation
```groovy
plugins { id 'info.solidsoft.pitest' version '1.15.0' }
pitest {
    targetClasses = ['com.keycloak.userstorage.*']
    targetTests   = ['com.keycloak.userstorage.*']
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
breaking changes. **Verify springdoc resolves from the internal repo first.**

### 7. OWASP Dependency-Check (LAST — offline risk)
```groovy
plugins { id 'org.owasp.dependencycheck' version '10.0.4' }
dependencyCheck {
    formats = ['JSON', 'HTML']
    failBuildOnCVSS = 7.0
    nvd { datafeedUrl = '<internal NVD mirror or cached feed>' }
}
```
Needs an NVD data feed the build host can reach. If there is no internal mirror,
keep this layer `skipped` and track CVEs manually.

## Detection
`.claude/scripts/detect-stack.sh` greps `build.gradle` for each plugin id and reports
which layers are active in `.specs/_stack.json`. After applying a layer, re-run
`/onboard` (or `harness.sh --baseline`) to refresh the baseline.
