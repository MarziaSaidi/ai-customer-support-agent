# Locked Decisions — SupportIQ

**Product:** SupportIQ — AI-Powered Customer Support Platform

See [PROJECT.md](./PROJECT.md) for full spec and [RESPONSIBILITIES.md](./RESPONSIBILITIES.md) for who owns what.

Decisions confirmed before implementation. Change only with explicit agreement.

| Area | Decision | Notes |
|------|----------|-------|
| Product name | **SupportIQ** | UI/branding; repo folder unchanged |
| Vector DB | PostgreSQL + pgvector | `document_chunks.embedding` |
| Message queue | RabbitMQ | Doc processing, background AI jobs |
| Email | SendGrid | Deferred to Day 12–13 stretch |
| File storage | **Local filesystem** (dev) | S3 interface for production (Day 13) |
| Deployment | Docker Compose → EC2/Render | Day 13 |
| Plan | **14-day roadmap** | See [ROADMAP.md](./ROADMAP.md) |
| Backend | Java 21, Spring Boot 3.4, JWT | |
| Frontend | Next.js, TypeScript, Tailwind, shadcn/ui | |
| Cache | Redis | Sessions, rate limiting |
| AI | OpenAI RAG + function calling | Day 7 + Day 9 |

## Deferred (post-MVP)

- AWS S3 file storage
- AWS EC2 / production deployment
- Kubernetes
- Elasticsearch
- Amazon SES (using SendGrid instead)

## Open questions (need your input)

| Question | Default if no answer |
|----------|----------------------|
| Support Agent vs Admin permissions | Admin: full access; Agent: chat + tickets only |
| Auto-create ticket when AI can't help? | Yes, with `OPEN` status |
| RabbitMQ confirmed? | Yes (assumed) |
