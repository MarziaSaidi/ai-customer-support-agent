# SupportIQ — AI-Powered Customer Support Platform

A multi-tenant SaaS platform that uses **Retrieval-Augmented Generation (RAG)** to help businesses automate customer support — answering questions from documentation, creating tickets, and analyzing conversations.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│  Next.js    │────▶│ Spring Boot │────▶│  PostgreSQL +    │
│  Frontend   │     │   Backend   │     │  pgvector        │
└─────────────┘     └──────┬──────┘     └──────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         ┌────────┐  ┌──────────┐  ┌─────────────┐
         │ Redis  │  │ RabbitMQ │  │ Local files │
         └────────┘  └──────────┘  └─────────────┘
```

**RAG pipeline (Day 5–7):**
```
PDF Upload → Extract → Chunk → Embeddings → Vector store → GPT answer
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
| Database | PostgreSQL + pgvector |
| AI | OpenAI API, embeddings, RAG, function calling |
| Queue | RabbitMQ |
| Deployment | Docker, Docker Compose, GitHub Actions |

## Documentation

| Doc | Description |
|-----|-------------|
| [PROJECT.md](docs/PROJECT.md) | Product definition & features |
| [ROADMAP.md](docs/ROADMAP.md) | **14-day development plan** |
| [RESPONSIBILITIES.md](docs/RESPONSIBILITIES.md) | Marzia vs AI ownership |
| [API.md](docs/API.md) | API contract |
| [DATABASE.md](docs/DATABASE.md) | Schema + migration notes |
| [DECISIONS.md](docs/DECISIONS.md) | Locked stack choices |

## Progress

| Day | Status |
|-----|--------|
| 1 — Project setup | ✅ Done |
| 2 — Authentication | ✅ Done |
| 3 — Company dashboard | ⚪ Next |

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
