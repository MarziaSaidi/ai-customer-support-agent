# Features & Status

What's implemented today, and what a next iteration would add.

## Implemented

| Area | Details |
|------|---------|
| Authentication | Register/login, stateless JWT, `/auth/me`, protected routes |
| Multi-tenancy | Company workspaces; every query scoped by company; membership checks on all tenant operations |
| Roles | `ADMIN` / `SUPPORT_AGENT` / `CUSTOMER`, enforced on privileged endpoints |
| Team management | List / add / remove members (admin only) |
| Document upload | Multipart upload (PDF/Markdown/text), local storage, list, soft-delete, reprocess |
| Processing pipeline | Text extraction (PDFBox) → chunking → OpenAI embeddings, run in a background thread |
| Retrieval | In-process cosine similarity + keyword blend, scoped per company |
| RAG chat | `POST /knowledge/ask` and the widget chat answer from company docs with source citations |
| AI function calling | `checkOrderStatus`, `createTicket`, `cancelOrder`, `requestRefund`, `searchDocumentation` via a bounded tool loop |
| Ticket system | Create, assign, status/priority updates, internal notes; auto-created on escalation |
| Analytics | Conversation trend, resolution rate, response time, ticket breakdown, top questions |
| Security hardening | Tenant-isolation checks, JWT error handling, default-secret guard, admin `@PreAuthorize`, request timeouts on AI calls, per-IP rate limiting on the public widget |
| Testing & CI | 30 unit + integration tests (JUnit, Mockito, MockMvc); GitHub Actions runs backend tests + frontend lint/build |
| Packaging | Docker Compose (Postgres + backend + frontend) |

## Possible next steps

- Flyway migrations in place of Hibernate `ddl-auto`
- Rate limiting on the authenticated API (the public chat widget is already throttled per IP)
- Retry/backoff around OpenAI calls
- User-facing error states and a data-fetching layer on the frontend
- Move retrieval into a database-side vector index (e.g. pgvector) if the knowledge base grows large
