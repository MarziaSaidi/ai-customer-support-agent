# Build Roadmap

Track progress day by day. Update status as phases complete.

## Locked stack

See [DECISIONS.md](./DECISIONS.md).

---

## Progress

| Day | Phase | Status | Deliverable |
|-----|-------|--------|-------------|
| 1 | Plan + contracts | ✅ Done | API docs, DB schema, scaffold fixes |
| 2 | Auth — register/login | ⚪ Pending | JWT auth hardened |
| 3 | Auth — reset/verify | ⚪ Pending | SendGrid emails |
| 4 | RBAC | ⚪ Pending | Role guards |
| 5 | Company onboarding | ⚪ Pending | Company flows |
| 6 | Dashboard shell | ⚪ Pending | Protected layout |
| 7 | Team management | ⚪ Pending | Invites |
| 8 | Doc upload API | ⚪ Pending | Local file storage |
| 9 | Doc processing | ⚪ Pending | RabbitMQ jobs |
| 10 | Doc UI | ⚪ Pending | Upload UI |
| 11 | Embeddings | ⚪ Pending | pgvector |
| 12 | RAG retrieval | ⚪ Pending | Context assembly |
| 13 | Chat backend | ⚪ Pending | Full pipeline |
| 14 | Chat widget | ⚪ Pending | Embeddable widget |
| 15 | Chat polish | ⚪ Pending | Escalation + feedback |
| 16–20 | Tickets + actions | ⚪ Pending | |
| 21–24 | Analytics + notifications | ⚪ Pending | |
| 25–27 | WebSocket | ⚪ Pending | |
| 28–30 | Polish + tests | ⚪ Pending | |

---

## Day 1 — Plan + contracts

### Me (AI)
- [x] API contract document
- [x] Database schema document
- [x] Locked decisions document
- [x] Fix scaffold gaps (local storage, env, README)
- [x] Verify compile/lint

### You
- [ ] Confirm RabbitMQ OK
- [ ] Review API + DB docs
- [ ] SendGrid key ready by Day 3
- [ ] OpenAI key ready by Day 11

---

## Day 2 — Auth register/login

### Me
- Harden auth service
- Add `/auth/me` endpoint
- Frontend auth context + token refresh pattern
- Input validation + error handling

### You
- Test signup and login manually
- Report any UX issues

---

## Responsibility quick reference

| Task type | Owner |
|-----------|-------|
| Write code | AI |
| API keys & accounts | You |
| Manual testing | You |
| Product rules | You |
| Architecture docs | AI |
| Phase approval | You |
