# Locked Decisions

Decisions confirmed before implementation. Change only with explicit agreement.

| Area | Decision | Notes |
|------|----------|-------|
| Vector DB | PostgreSQL + pgvector | Single database for relational + vector search |
| Message queue | RabbitMQ | Email, doc processing, background AI tasks |
| Email | SendGrid | Password reset, verification, ticket notifications |
| File storage | **Local filesystem** | Dev/MVP; S3 deferred |
| Deployment | **Local + Docker Compose** | No AWS EC2 for now |
| Scope | **Full spec** | All features in original description |
| Backend | Java 21, Spring Boot 3.4, JWT | |
| Frontend | Next.js, TypeScript, Tailwind, shadcn/ui | |
| Cache | Redis | Sessions, FAQ cache, rate limiting |
| Real-time | Spring WebSocket | Live chat, typing, agent alerts |
| Search | PostgreSQL full-text search | Elasticsearch optional later |

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
