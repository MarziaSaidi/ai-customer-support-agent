# SupportIQ — Project Definition

**Product name:** SupportIQ — AI-Powered Customer Support Platform

**One-line description:** A multi-tenant SaaS platform that uses Retrieval-Augmented Generation (RAG) to help businesses automate customer support by answering questions from their documentation, creating tickets, and analyzing customer conversations.

## Why this project

Combines AI, backend engineering, full-stack development, database design, cloud deployment, and a real business use case — the kind of product a startup would actually build.

---

## Main features

### 1. Company workspace
Businesses create accounts with team roles and a document library.

```
Company: Nike Support
Team:    Admin, Support Agent
Docs:    Return Policy.pdf, Shipping FAQ.pdf, Warranty.pdf
```

### 2. AI knowledge base (RAG)
Admin uploads PDF, Markdown, or website FAQ. Pipeline:

```
Document → Extract text → Chunk → Embeddings → Store vectors → AI search
```

Example:
- **Customer:** Can I return shoes after 30 days?
- **AI:** According to Nike's return policy, shoes can be returned within 60 days if unused.
- **Source:** Return Policy.pdf page 3

### 3. AI chat assistant
```
User question → Spring Boot → Vector search → OpenAI → Answer
```

### 4. AI actions (function calling)
AI calls backend functions: `getOrderStatus()`, `createTicket()`, `cancelRequest()`.

### 5. Ticket system
When AI cannot answer → create ticket with title, priority, status. Agent resolves.

### 6. AI analytics dashboard
- Total conversations
- AI resolution rate
- Average response time
- Top questions

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js, React, TypeScript, Tailwind CSS, shadcn/ui |
| Backend | Java 21, Spring Boot 3, Spring Security, JWT, Spring Data JPA, Hibernate, Maven |
| Database | PostgreSQL |
| AI | OpenAI API, embeddings, RAG, function calling |
| Storage | Local filesystem |
| Deployment | Docker, Docker Compose, GitHub Actions |

---

## Target database (canonical)

| Table | Purpose |
|-------|---------|
| `users` | id, email, password |
| `companies` | id, name |
| `company_users` | user_id, company_id, role |
| `documents` | id, company_id, filename |
| `document_chunks` | id, document_id, content, embedding |
| `conversations` | id, customer_id |
| `messages` | id, conversation_id, role, content |
| `tickets` | id, conversation_id, status, priority |

See [DATABASE.md](./DATABASE.md) for full schema and migration notes from current code.

---

## Repo vs product name

| Item | Value |
|------|-------|
| Product / UI name | **SupportIQ** |
| Local folder | `ai-customer-support-agent` |
| GitHub repo | `MarziaSaidi/ai-customer-support-agent` |

Renaming the repo to `supportiq` is optional later.
