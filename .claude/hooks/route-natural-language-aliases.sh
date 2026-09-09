#!/usr/bin/env bash
# route-natural-language-aliases.sh
# Claude Code UserPromptSubmit hook.
#
# Translates casual phrasing (Korean + English) into an explicit slash command
# by emitting an additionalContext hint. Does not rewrite the user's prompt.

set -euo pipefail

input="$(cat)"
if command -v jq >/dev/null 2>&1; then
  prompt="$(echo "$input" | jq -r '.prompt // empty')"
else
  prompt="$(echo "$input" | sed -n 's/.*"prompt"[[:space:]]*:[[:space:]]*"\(.*\)"[[:space:]]*}.*/\1/p')"
fi
[ -z "$prompt" ] && exit 0

lc="$(echo "$prompt" | tr '[:upper:]' '[:lower:]')"

emit() {
  printf '{"hookSpecificOutput":{"hookEventName":"UserPromptSubmit","additionalContext":"Natural-language alias detected: consider routing to %s (see .claude/commands/%s.md)."}}\n' "$1" "${1#/}"
}

case "$lc" in
  *"온보딩"*|*"onboard this repo"*|*"onboard this project"*|*"베이스라인 캡처"*)
    emit "/onboard" ;;
  *"스펙 짜"*|*"스펙 작성"*|*"스펙 만들"*|*"요구사항 정리"*|*"spec this"*|*"write a spec"*|*"turn this ticket into requirements"*)
    emit "/spec" ;;
  *"스펙 리뷰"*|*"스펙 검토"*|*"review the spec"*)
    emit "/spec-review" ;;
  *"설계해"*|*"design this"*|*"plan this"*|*"태스크로 쪼개"*|*"break this into tasks"*|*"구현 계획"*)
    emit "/plan" ;;
  *"빌드해"*|*"구현해"*|*"implement t-"*|*"build t-"*)
    emit "/build" ;;
  *"검증해"*|*"validate"*|*"run the harness"*|*"하네스 돌려"*)
    emit "/validate" ;;
  *"코드 리뷰"*|*"review the code"*|*"pre-commit review"*|*"커밋 전 리뷰"*)
    emit "/review" ;;
  *"현황"*|*"진행 상황"*|*"status"*|*"where do things stand"*)
    emit "/status" ;;
  *)
    exit 0 ;;
esac
