# Design Decisions — SupportIQ

Key technical choices and the reasoning behind them. See [PROJECT.md](./PROJECT.md) for the feature spec.

## Stack

| Area | Choice | Why |
|------|--------|-----|
| Backend | Java 21, Spring Boot 3.4, Spring Data JPA | Typed, well-supported, common in industry backends |
| Auth | Stateless JWT + Spring Security | No server session state; simple to scale horizontally |
| Database | PostgreSQL 16 | One reliable datastore for all relational data |
| AI | OpenAI API (embeddings + chat + function calling) | Managed models; no infra to run |
| Retrieval | In-process cosine similarity over stored embeddings | Simple and sufficient at this scale; no separate vector DB to operate |
| Document processing | Synchronous, in a background thread (`@Async`) | Upload returns fast without standing up a message broker |
| File storage | Local filesystem | Straightforward for a single-node app |
| Frontend | Next.js, TypeScript, Tailwind, shadcn/ui | Modern React with a typed API layer |
| Deploy | Docker Compose | Reproducible local/prod-like environment |

## Deliberately kept out

These were considered and cut to keep the system small enough to run and reason about end to end:

| Considered | Cut in favor of | Would revisit when |
|------------|-----------------|--------------------|
| pgvector / dedicated vector DB | In-process cosine similarity | The knowledge base grows large enough that a DB-side vector index pays off |
| RabbitMQ | Synchronous background processing | Processing becomes slow or needs retries/fan-out |
| Redis | (not needed yet) | Adding caching or distributed rate limiting |
| AWS S3 | Local filesystem | Running multi-node or needing durable object storage |
| WebSockets | Request/response polling | Real-time agent presence/typing becomes a priority |

The principle: don't run infrastructure the app doesn't actually need. Each of the above is a clean extension point rather than a dependency carried from day one.

## Product notes

| Item | Value |
|------|-------|
| Product / UI name | SupportIQ |
| GitHub repo | `MarziaSaidi/ai-customer-support-agent` |
| Roles | `ADMIN` (full access), `SUPPORT_AGENT` (chat + tickets), `CUSTOMER` |
| AI can't answer? | Auto-creates an `OPEN` ticket and escalates to a human |
