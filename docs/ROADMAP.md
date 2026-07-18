# 14-Day Development Plan

Aligned with [PROJECT.md](./PROJECT.md). Progress tracked below.

**Responsibilities:** see [RESPONSIBILITIES.md](./RESPONSIBILITIES.md) — Marzia owns architecture, building, testing, deployment; AI assists.

---

## Progress tracker

| Day | Focus | Status | Owner test? |
|-----|-------|--------|-------------|
| 1 | Project setup | ✅ Done | — |
| 2 | Authentication | ✅ Done | [x] Marzia |
| 3 | Company dashboard | ✅ Done | [x] Marzia |
| 4 | Document upload | ✅ Done | [x] Marzia |
| 5 | Document processing pipeline | ✅ Done | |
| 6 | Vector search (pgvector) | ✅ Done | |
| 7 | Connect AI (RAG) | ✅ Done | |
| 8 | Chat interface | ⚪ Next | |
| 9 | AI function calling | ⚪ Pending | |
| 10 | Ticket system | ⚪ Pending | |
| 11 | Analytics dashboard | ⚪ Pending | |
| 12 | Testing | ⚪ Pending | |
| 13 | Deployment | ⚪ Pending | |
| 14 | Portfolio polish | ⚪ Pending | |

---

## Day 1 — Project setup ✅

**Build:** Spring Boot, Next.js, PostgreSQL, Docker

**Learn:** Spring structure, REST API basics

**Done:**
- Monorepo scaffold
- Docker Compose (Postgres + pgvector, Redis, RabbitMQ)
- API & database contract docs

---

## Day 2 — Authentication ✅

**Build:** Register, login, JWT, protected routes

**Tables:** Users, roles, companies (initial — migrate to `company_users` on Day 3)

**Done:**
- JWT auth, `/auth/me`, RBAC roles on user
- Frontend auth context, protected/guest routes
- Auth tests

**Marzia:** manually test signup/login

---

## Day 3 — Company dashboard ✅

**Build:** Dashboard UI, team members, `company_users` table

### Me (AI)
- [x] `company_users` entity + membership on register
- [x] Team APIs (list, add, remove) with admin guards
- [x] Company settings API (PUT)
- [x] Dashboard shell with sidebar (Overview, Team, Settings)
- [x] Company controller tests

### You (Marzia)
- [x] Test dashboard navigation
- [x] Test Settings page (change company name / AI prompt)
- [x] Register a second user, add them as Support Agent on Team page
- [x] Confirm Support Agent cannot save settings

---

## Day 4 — Document upload ✅

**Build:** Upload PDF, store file locally, save metadata in DB

### Me (AI)
- [x] Multipart upload API (`POST /api/documents`)
- [x] Local file storage under `uploads/{companyId}/`
- [x] List and soft-delete documents
- [x] Documents dashboard page
- [x] Upload tests

### You (Marzia)
- [ ] Upload a sample PDF from `/dashboard/documents`
- [ ] Confirm it appears in the list with status "Pending"
- [ ] Confirm Support Agent can view but not upload
- [ ] Test delete (admin)

---

## Day 5 — Document processing pipeline ✅

**Build:**
```
PDF → Text extraction → Chunking → Embeddings
```
Use RabbitMQ for async jobs. Renamed `ai_embeddings` → `document_chunks`.

### Me (AI)
- [x] `DocumentChunk` entity (replaces `ai_embeddings`)
- [x] Text extraction (PDF via PDFBox, Markdown/TXT plain read)
- [x] Text chunking service (~800 chars with overlap)
- [x] OpenAI embedding service (stub fallback when no API key)
- [x] `DocumentProcessingService` orchestrator
- [x] RabbitMQ async dispatch + sync fallback for dev/test
- [x] Auto-process on upload; `POST /documents/{id}/reprocess`
- [x] Frontend polls until status changes to Processed
- [x] Pipeline tests

### You (Marzia)
- [ ] Restart backend, upload `samples/return-policy.pdf`
- [ ] Wait a few seconds — status should change from Pending → Processed
- [ ] Re-upload `samples/shipping-faq.txt` and confirm same behavior

---

## Day 6 — Vector search ✅

**Build:** pgvector similarity search with in-memory fallback for dev

**Test:** Question *"How do refunds work?"* → finds return policy chunk

### Me (AI)
- [x] `VectorSearchService` with pgvector + in-memory cosine search
- [x] `POST /api/documents/search` endpoint
- [x] Improved stub embeddings for local dev without OpenAI key
- [x] Knowledge search test UI on documents page
- [x] Vector search tests

### You (Marzia)
- [ ] Upload and process a return policy document
- [ ] Search "How do refunds work?" on the documents page
- [ ] Confirm the return policy chunk appears in results

---

## Day 7 — Connect AI (RAG) ✅

**Build:**
```
User question → Retrieve documents → GPT with context → Answer + source citation
```

### Me (AI)
- [x] `RagService` with vector retrieval + OpenAI chat completion
- [x] Fallback answers from top chunks when no OpenAI key
- [x] `POST /api/knowledge/ask` endpoint with source citations
- [x] Widget chat uses RAG via updated `AiService`
- [x] Ask AI preview UI on documents page
- [x] RAG integration tests

### You (Marzia)
- [ ] Upload return policy, then ask "Can I return shoes after 30 days?"
- [ ] Confirm answer references Return Policy with sources listed
- [ ] Optional: add `OPENAI_API_KEY` to `.env` for GPT-generated answers

---

## Day 8 — Chat interface

**Build:** Chat UI, message history, conversation storage

Rename `chat_sessions` → `conversations` per target schema.

---

## Day 9 — AI function calling

**Functions:**
- `checkOrderStatus()`
- `createTicket()`
- `cancelRequest()` / `searchDocumentation()`

---

## Day 10 — Ticket system

**Build:** Create, assign, update status, internal notes

---

## Day 11 — Analytics

**Charts:** conversations, resolved %, common questions, response time

---

## Day 12 — Testing

**Backend:** JUnit, Mockito  
**API:** Postman collection  
**Marzia:** define test cases, verify edge cases

---

## Day 13 — Deployment

**Deploy:** backend, frontend, database via Docker  
**Target:** local Docker first; EC2 or Render when ready

---

## Day 14 — Portfolio polish

**README:** architecture diagram, screenshots, demo video link, API docs

**Marzia:** record demo, write portfolio description

---

## What changed from the old 30-day plan

| Old plan | New 14-day plan |
|----------|-----------------|
| Day 3: email reset/verify | Deferred (add during Day 12–13 if needed) |
| Day 3–5: auth extras | Compressed into Day 2 ✅ |
| 30 days total | 14 days to portfolio-ready MVP |
| Product name SupportAI | **SupportIQ** |

Email verification (SendGrid) can be added in Day 12–13 or as a stretch goal — not blocking RAG MVP.
