# Rule 04: Backlog Creation (UI Intent + DB Schema First)

**Principle**: Before writing a backlog item, collect UI intent and DB schema from the developer.

---

## When to Apply

When the developer says:
- "이런 기능 백로그로 만들어줘"
- "backlog 작성해줘"
- Any request to create a new backlog item

---

## Step 1: Ask Before Writing

Do NOT write the backlog immediately. First ask:

```
[UI]
1. 어느 페이지/컴포넌트에 붙는가?
   (Dashboard ToolCard / Admin 설정 페이지 / 별도 페이지 등)
2. 어떤 형태인가?
   (버튼 추가 / 모달 / 폼 / 별도 섹션 / 새 페이지)
3. 기존 컴포넌트 수정인가, 신규 생성인가?

[DB]
4. 새 테이블이 필요한가, 기존 테이블 수정인가?
5. 다른 기능과 공유하는 데이터인가?
```

If the developer's request already answers some of these, skip those questions.

---

## Step 2: Offer Options When Unclear

If the developer is unsure, offer 2-3 options:

```
UI 형태가 불명확할 때:
  A) ToolCard에 버튼 추가 (기존 화면 수정)
  B) 별도 즐겨찾기 페이지 신규 생성
  → 어느 쪽이 더 맞나요?
```

---

## Step 3: Write Backlog with UI + DB Sections

```
제목: NN. [기능명]: [핵심 행위]

Why:  현재 상태의 문제 → 원하는 상태
Scope:
  - 변경할 것
  - 변경하지 않을 것  ← 명시 필수
UI:
  - 위치: [페이지/컴포넌트]
  - 형태: [버튼/모달/폼/페이지]
  - 수정/신규: [기존 컴포넌트명 또는 신규]
DB:
  - [신규 테이블명] 또는 [기존 테이블 수정]
  - 핵심 필드: [목록]
Dependencies:
  - 인프라 / 선행 Step
Notes:
  - 기존 구현에서 주의할 점 (구현이 가까워지면 채움)
```

---

## What to Read Before Writing

1. `CLAUDE.md` + `MEMORY.md` — 아키텍처, 기존 패턴, Gotchas
2. 유사한 기존 backlog 파일 1-2개 — 형식 참고
3. 기존 컴포넌트/라우트 — 수정 대상 파악

---

## Checklist

```
[ ] UI 위치/형태 확인했는가?
[ ] DB 신규/수정 여부 확인했는가?
[ ] 불명확한 것은 옵션 제시 후 선택받았는가?
[ ] Backlog에 UI + DB 섹션이 있는가?
[ ] "변경하지 않을 것" 명시했는가?
```

---

## See Also

- `01-contract-first.md` — 구현 시작 시 API contract 정의
- `02-vertical-slicing.md` — 구현 시 slice 분할
- `.claude/backlog/` — 실제 backlog 파일들
