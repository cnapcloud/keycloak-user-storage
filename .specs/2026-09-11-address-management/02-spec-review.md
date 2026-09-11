# Spec Review: 2026-09-11-address-management

> Owner: `spec-author` (review hat) · Phase 2 · Template: `.claude/templates/spec-review.template.md`

## Verdict
**PASS** — 1차 리뷰에서 지적한 AC-005/AC-010 비-atomic 문제를 AC-013/AC-014 분리로 해소, `/plan` 진행 중 발견된 AC-015(목록 조회 시 사용자 없음 404) 누락도 보완, 나머지 항목 전부 pass/n·a.

## Summary
- acs_total: 15
- acs_failed: 0
- open_questions: 0
- next_command: `/plan`

## Checklist results
> From `.claude/checklists/spec-review.md`.

| # | Item | Result | Rationale |
|---|------|--------|-----------|
| 1 | `## Source`에 출처+snapshot date 기록 | pass | origin, snapshot date 2026-09-11, verbatim 요청 + 추가 지시 기록 |
| 2 | `## Goal` 1문단, 구현 언어 없음 | pass | 사용자 관점 결과만 서술 |
| 3 | `## Non-Goals` 명시 | pass | 5건 명시 |
| 4 | 모든 AC가 EARS-lite 형식 | pass | Ubiquitous(001·002·012)/Event-driven(003·006·007·008·009·011)/Unwanted(004·005·010·013·014·015) — 5형식 범위 내 |
| 5 | 모든 AC가 atomic(조건 1개·결과 1개) | pass | 1차 리뷰의 AC-005(우편번호/도로명주소 OR 결합)를 AC-005(우편번호만)+AC-013(도로명주소만)으로, AC-010(미존재/소유자불일치 OR 결합)을 AC-010(미존재만)+AC-014(소유자불일치만)으로 분리해 각 AC가 단일 원인→단일 결과로 정리됨. AC-015는 단일 조건(목록 조회 시 userId 없음)→단일 결과(404)로 그 자체로 atomic |
| 6 | 모든 AC가 테스트 가능 | pass | 각 AC가 구체적 HTTP 메서드/경로/상태코드/바디로 명시돼 단일 assertion 세트로 매핑 가능 |
| 7 | AC id가 `AC-NNN`, 순차, 재사용 없음 | pass | AC-001~015 순차, 신규 013·014·015는 끝에 추가되어 기존 id 재번호 없음 |
| 8 | AC에 구현 언어(클래스/라이브러리/테이블명) 없음 | pass | HTTP 메서드·경로·상태코드만 사용 |
| 9 | 관련 USP 계약 엔드포인트 상호 참조 | pass | "USP 계약" 문단에 신규 엔드포인트 및 `DELETE /user/{id}`(BL-02) 확장 명시 |
| 10 | 개념 엔티티+관계(cardinality)가 비즈니스 언어로 명시 | pass | `Address`, `User 1..* Address`, ORM/테이블명 없음 |
| 11 | `User`/`USER_ATTRIBUTES`/`CredentialData` 데이터 영향 명시 | pass | `User` 컬럼 변경 없음, cascade 삭제 영향 명시 |
| 12 | `attributes` null-safety/PATCH 시맨틱 반영 | n/a | Address는 `attributes` map 미사용, PATCH 아닌 전체 교체(PUT) |
| 13 | 모든 NFR이 구체적 수치이거나 `Q-NNN` | n/a | 성능/용량류 NFR 없음. 우편번호 5자리는 기능 검증 규칙으로 AC-005에 수치 반영됨 |
| 14 | `## Assumptions`에 사용자/소스가 명시한 것만 | pass | 실질적 가정 없음 |
| 15 | 모든 불확실성이 `Q-NNN` + "왜 중요한지" + 후보안 | pass | `Resolved Questions` Q-001~008 전부 근거 기재, 신규 미해결 Q 없음 |
| 16 | `PASS` 전 `## Open Questions` 비어있거나 전부 deferred | pass | 비어 있음 (`- (없음)`) |

## Required edits (one per `fail`)
- (없음 — 모든 항목 pass/n·a)

## New open questions raised by this review
- (없음)

## Sign-off
- [x] Every checklist item is `pass` or `n/a`.
- [x] `## Open Questions` in `01-spec.md` is empty or all `deferred`-with-rationale.
- [x] Reviewed by user on 2026-09-11.
