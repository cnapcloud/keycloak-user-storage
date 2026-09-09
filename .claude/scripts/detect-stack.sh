#!/usr/bin/env bash
# detect-stack.sh
# Emit a _stack.json describing this repo's build/runtime/harness posture.
# Usage: .claude/scripts/detect-stack.sh [build.gradle path]  > .specs/_stack.json

set -euo pipefail

GRADLE_FILE="${1:-build.gradle}"
[ -f "$GRADLE_FILE" ] || { echo "detect-stack: $GRADLE_FILE not found" >&2; exit 2; }

grade() { grep -qE "$1" "$GRADLE_FILE" && echo true || echo false; }

build_tool="unknown"
[ -f build.gradle ] && build_tool="gradle"
[ -f pom.xml ] && build_tool="maven"

java_ver="$(grep -oE 'VERSION_[0-9]+' "$GRADLE_FILE" | head -n1 | grep -oE '[0-9]+' || echo 'unknown')"
boot_ver="$(grep -oE "org.springframework.boot' version '[0-9.]+" "$GRADLE_FILE" | grep -oE '[0-9.]+' | tail -n1 || echo 'unknown')"

db="unknown"
grep -qE 'com\.h2database:h2' "$GRADLE_FILE" && db="h2"
grep -qE 'org\.postgresql:postgresql' "$GRADLE_FILE" && db="postgresql"

migration="none"
{ grep -qE 'flyway' "$GRADLE_FILE" && grep -qE 'liquibase' "$GRADLE_FILE"; } && migration="both"
{ grep -qE 'flyway' "$GRADLE_FILE" && ! grep -qE 'liquibase' "$GRADLE_FILE"; } && migration="flyway"
{ grep -qE 'liquibase' "$GRADLE_FILE" && ! grep -qE 'flyway' "$GRADLE_FILE"; } && migration="liquibase"

cat <<EOF
{
  "build": "$build_tool",
  "java": "$java_ver",
  "spring_boot": "$boot_ver",
  "db": "$db",
  "migration": "$migration",
  "test_framework": "junit-platform",
  "harness_layers": {
    "unit":       $(grade "useJUnitPlatform|spring-boot-starter-test"),
    "coverage":   $(grade "id \"jacoco\"|id 'jacoco'"),
    "mutation":   $(grade "info\\.solidsoft\\.pitest"),
    "checkstyle": $(grade "id \"checkstyle\"|id 'checkstyle'"),
    "spotbugs":   $(grade "com\\.github\\.spotbugs"),
    "archunit":   $(grade "com\\.tngtech\\.archunit"),
    "owasp":      $(grade "org\\.owasp\\.dependencycheck"),
    "openapi":    $(grade "springdoc|openapi")
  }
}
EOF
