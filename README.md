# SupportIQ — AI-Powered Customer Support Platform

A multi-tenant SaaS platform that uses **Retrieval-Augmented Generation (RAG)** to help businesses automate customer support — answering questions from documentation, creating tickets, and analyzing conversations.

## Architecture

Intentionally lean — a few well-understood pieces rather than a wide stack of half-used infrastructure.

```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Next.js    │────▶│ Spring Boot │────▶│  PostgreSQL  │
│  Frontend   │     │   Backend   │     └──────────────┘
└─────────────┘     └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
     ┌─────────────────┐        ┌──────────────┐
     │  OpenAI API     │        │  Local files │
     │ (embeddings/chat)│        │  (uploads)   │
     └─────────────────┘        └──────────────┘
```

**RAG pipeline:**
```
PDF/Text upload → Extract → Chunk → Embeddings → Cosine-similarity search → GPT answer
```

## Testing

See [docs/TESTING.md](docs/TESTING.md) for automated test commands, Postman collection, and manual checklist.

```bash
cd backend && ./mvnw test
cd frontend && npm run lint && npm run build
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js, React, TypeScript, Tailwind CSS, shadcn/ui |
| Backend | Java 21, Spring Boot 3, Spring Security, JWT, Maven |
| Database | PostgreSQL (Spring Data JPA) |
| AI | OpenAI API — embeddings, RAG, function calling |
| Deployment | Docker, Docker Compose, GitHub Actions |

## Documentation

| Doc | Description |
|-----|-------------|
| [PROJECT.md](docs/PROJECT.md) | Product definition & features |
| [ROADMAP.md](docs/ROADMAP.md) | Features & status |
| [API.md](docs/API.md) | API contract |
| [DATABASE.md](docs/DATABASE.md) | Database schema |
| [DECISIONS.md](docs/DECISIONS.md) | Design decisions & tradeoffs |
| [TESTING.md](docs/TESTING.md) | Test commands & checklist |

## Security model

Authentication and multi-tenant isolation are enforced on every request:

- **Stateless JWT auth** — `JwtAuthenticationFilter` validates a `Bearer` token per request; invalid, expired, or tampered tokens are rejected with `401/403` (never a `500`). The signing secret must be a strong value — the app **fails fast on startup** outside local dev if `JWT_SECRET` is left as the placeholder.
- **Tenant isolation** — every tenant-scoped service call verifies the caller's membership in the target company (`requireTeamMember` / `requireMembership`), so a user of Company A cannot read or mutate Company B's data even by guessing IDs.
- **Role-based access** — privileged operations (company settings, member management) require `ADMIN` via both `@PreAuthorize("hasRole('ADMIN')")` (coarse, first line) and a tenant-aware `requireAdmin` check in the service (the authoritative gate). Authenticated-but-forbidden returns `403`.
- **Abuse protection** — the public, unauthenticated chat widget is rate-limited per client IP (`RateLimitInterceptor`, default 30 req/min) so an anonymous caller can't flood the AI backend and run up API costs. Excess requests get `429` with a `Retry-After` header.

Cross-tenant and malformed-token behavior is covered by `SecurityIntegrationTest`.

## How retrieval works

Uploaded documents are chunked, embedded with OpenAI, and the vectors are stored on each chunk. At query time the service embeds the question and ranks the company's chunks by **cosine similarity**, blended with a light keyword-overlap score, then feeds the top matches to the model as grounding context (with source citations). Search is always scoped to the caller's company, so tenants never see each other's knowledge base. This runs in-process against PostgreSQL — no separate vector database to operate — which is plenty for this app's scale and easy to reason about. `AiService` grounds answers only in retrieved context and escalates to a human ticket when it can't find an answer, to limit hallucination.

## Roadmap / known gaps

Honest list of what a production hardening pass would add next: Flyway migrations (replacing Hibernate `ddl-auto`), rate limiting on the authenticated API (the public widget is already throttled), retry/backoff around the OpenAI calls, and user-facing error states on the frontend. If the knowledge base grew large, the cosine-similarity scan would move into the database with a dedicated vector index (e.g. pgvector).

## Quick Start

### Prerequisites

- Java 21, Node.js 20+, Docker & Docker Compose

### Run locally (no Docker — quick auth testing)

If Docker is not installed, use the **dev** profile (in-memory H2 database):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Then in a **second terminal**:

```bash
cd frontend
npm install   # first time only
npm run dev
```

Open http://localhost:3000/register

### Run locally (with Docker — full stack)

```bash
cp .env.example .env   # then set OPENAI_API_KEY and a strong JWT_SECRET
docker compose up --build
```

Postgres starts with a healthcheck and the backend waits for it before booting. Frontend on http://localhost:3000, API on http://localhost:8080.

## Deployment

A common free-tier setup is the frontend on **Vercel** and the backend + **managed PostgreSQL** on **Render** or **Railway** (the backend ships with a `Dockerfile`).

### Backend environment variables

| Variable | Required | Notes |
|----------|----------|-------|
| `SPRING_PROFILES_ACTIVE` | ✅ | Set to `docker`. A non-local profile also activates the strong-`JWT_SECRET` startup guard. |
| `DATABASE_URL` | ✅ | JDBC URL of the managed Postgres, e.g. `jdbc:postgresql://host:5432/support_ai` |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | ✅ | Database credentials |
| `JWT_SECRET` | ✅ | 32+ byte random value — `openssl rand -base64 48`. The app refuses to start with the placeholder. |
| `OPENAI_API_KEY` | ✅ | Real embeddings + GPT answers; without it the app falls back to stub answers |
| `CORS_ALLOWED_ORIGINS` | ✅ | Your deployed frontend origin, e.g. `https://your-app.vercel.app` |
| `JWT_EXPIRATION_MS` | — | Token lifetime; defaults to 24h |
| `LOCAL_STORAGE_PATH` | — | Upload directory; defaults to `./uploads` (see note below) |
| `WIDGET_RATE_LIMIT` | — | Public-widget requests/min per IP; defaults to `30` |

### Frontend environment variable

| Variable | Required | Notes |
|----------|----------|-------|
| `NEXT_PUBLIC_API_URL` | ✅ | Deployed backend base URL **including `/api`**, e.g. `https://your-api.onrender.com/api` |

### Steps

1. Provision a managed PostgreSQL instance; note its JDBC URL and credentials.
2. Deploy the backend from `backend/` (Docker) with the environment variables above.
3. Deploy the frontend from `frontend/` (Vercel auto-detects Next.js) with `NEXT_PUBLIC_API_URL`.
4. Set the backend's `CORS_ALLOWED_ORIGINS` to the deployed frontend origin.
5. Verify: `GET https://<backend-host>/actuator/health` returns `{"status":"UP"}`.

The schema is created automatically on first boot (Hibernate `ddl-auto: update`).

### Before you go live

- **Set an OpenAI spending limit** in the OpenAI dashboard as a backstop — the public widget is already rate-limited, but a hard cap is cheap insurance.
- **Uploaded files use the local filesystem, which is ephemeral** on most PaaS hosts (lost on redeploy). Fine for a demo; attach a persistent volume or object storage for durability.

## Core Features

1. **Company workspace** — multi-tenant accounts, Admin + Support Agent roles
2. **AI knowledge base** — PDF/Markdown upload, chunking, vector search
3. **AI chat** — RAG-powered customer Q&A with source citations
4. **AI actions** — function calling (`checkOrderStatus`, `createTicket`)
5. **Ticket system** — escalation when AI can't resolve
6. **Analytics** — resolution rate, response time, top questions

## Development

```bash
cd backend && ./mvnw test
cd frontend && npm run lint
```

## License

MIT
