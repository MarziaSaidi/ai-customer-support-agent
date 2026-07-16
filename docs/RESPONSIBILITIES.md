# Responsibilities — Marzia vs AI

## Simple summary

| Task | Marzia | AI (Cursor) |
|------|--------|-------------|
| Product idea | Decide | Help brainstorm |
| Architecture | Design | Review options |
| Database | Design | Suggest improvements |
| Backend | Build & own | Generate boilerplate, review |
| Frontend | Build & own | Help components, debug |
| AI integration | Design usage | Help implementation |
| Debugging | Verify & fix | Analyze errors |
| Testing | Decide quality | Generate tests |
| Deployment | Configure | Troubleshoot |
| Documentation | Own | Improve drafts |

---

## Phase-by-phase

### Planning & architecture — Marzia
- Define product requirements
- Decide core features
- Design system architecture
- Choose tech stack
- Design database schema
- Plan API structure

**Deliverables:** architecture diagram, database design, feature roadmap

**AI helps:** review options, suggest improvements, draft docs

---

### Backend — Marzia builds, AI assists

| Area | Marzia | AI |
|------|--------|-----|
| Authentication | Implement & verify | Security boilerplate, explain concepts |
| Database layer | Entities, relationships, repos, services | Suggest relationships, review queries |
| REST APIs | Own API design & implementation | Controller templates, error patterns |

**APIs to own:** auth, companies, documents, chat, tickets, analytics

---

### Frontend — Marzia builds, AI assists

| Area | Marzia | AI |
|------|--------|-----|
| Dashboard | Company UI, uploads, tickets | Component generation, UI ideas |
| Chat interface | Chat window, history, loading states | React debugging, TypeScript |

---

### AI system — Marzia designs, AI implements

**Marzia decides:**
- What information AI can access
- When AI answers vs creates tickets
- Which backend functions AI can call

**AI helps implement:**
- Document pipeline (PDF → chunks → embeddings)
- RAG pipeline (question → search → LLM → answer)
- Function calling (`checkOrderStatus`, `createTicket`, `searchDocumentation`)

---

### Testing — Marzia leads

- Decide test cases
- Verify behavior manually
- Fix bugs
- Review AI-generated code

**AI:** unit test examples, edge cases, explain failures

---

### Deployment — Marzia configures

- Docker & Docker Compose
- Environment variables
- Cloud deployment (EC2 / Render)
- CI/CD (GitHub Actions)

**AI:** Dockerfiles, troubleshooting, config explanations

---

### Documentation — Marzia owns

- README, architecture, demo video, portfolio copy

**AI:** drafts, clarity improvements

---

## Daily workflow

1. **Marzia** approves the day’s goal from [ROADMAP.md](./ROADMAP.md)
2. **AI** implements assigned tasks
3. **Marzia** manually tests and signs off
4. Move to next day only after approval
