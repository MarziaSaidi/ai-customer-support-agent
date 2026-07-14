# AI Customer Support Agent

A SaaS platform that helps businesses automate customer support using AI. Customers interact through an embeddable chat widget; the AI answers from company documentation and can perform actions like checking orders, processing refunds, or creating support tickets.

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

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3, Spring Security, JWT |
| Frontend | Next.js, React, TypeScript, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL with pgvector |
| Cache | Redis |
| Queue | RabbitMQ |
| AI | OpenAI API + RAG (embeddings + vector search) |
| Storage | Local filesystem (S3 deferred) |
| Email | SendGrid |
| Real-time | Spring WebSocket |
| Monitoring | Prometheus + Grafana |

## Project Structure

```
ai-customer-support-agent/
├── backend/          # Spring Boot API
├── frontend/         # Next.js dashboard + chat widget
├── docker-compose.yml
└── .github/workflows/
```

## Documentation

- [API Contract](docs/API.md) — all endpoints (implemented + planned)
- [Database Schema](docs/DATABASE.md) — tables and relationships
- [Build Roadmap](docs/ROADMAP.md) — day-by-day plan
- [Decisions](docs/DECISIONS.md) — locked stack choices

## Quick Start

### Prerequisites

- Java 21
- Node.js 20+
- Docker & Docker Compose
- Maven 3.9+

### 1. Clone and configure

```bash
cp .env.example .env
# Edit .env with your API keys
```

### 2. Start infrastructure

```bash
docker compose up -d postgres redis rabbitmq
```

### 3. Run backend

```bash
cd backend
./mvnw spring-boot:run
```

### 4. Run frontend

```bash
cd frontend
npm install
npm run dev
```

### 5. Full stack with Docker

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- RabbitMQ Management: http://localhost:15672

## Core Features

- **Authentication** — Sign up, login, password reset, email verification, RBAC (Admin, Support Agent)
- **Company Dashboard** — Team management, documentation upload, conversation history, AI settings
- **Knowledge Base** — PDF, Word, FAQ, website pages, Markdown with vector search
- **AI Chat** — Customer Q&A with escalation to human agents
- **Ticket System** — Create, assign, track, and annotate support tickets
- **AI Actions** — Order status, refunds, cancellations, address updates, password resets
- **Analytics** — Resolution rate, response time, satisfaction, common questions
- **Notifications** — Email and in-app alerts

## API Overview

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/register` | User registration |
| `POST /api/auth/login` | JWT login |
| `GET /api/companies` | Company management |
| `POST /api/documents` | Upload knowledge base docs |
| `POST /api/chat/sessions` | Start chat session |
| `POST /api/chat/messages` | Send message |
| `GET /api/tickets` | List support tickets |
| `GET /api/analytics` | Dashboard metrics |

## Development

```bash
# Backend tests
cd backend && ./mvnw test

# Frontend lint
cd frontend && npm run lint
```

## License

MIT
