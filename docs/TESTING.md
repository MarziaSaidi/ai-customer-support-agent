# SupportIQ — Testing Guide

Manual checklist and automated test instructions for Day 12.

---

## Run automated tests

### Backend (JUnit + Mockito)

```bash
cd backend
./mvnw test
```

**Coverage includes:**
- Controller integration tests (auth, chat, documents, tickets, analytics)
- Service integration tests (RAG, vector search, document processing, AI functions)
- Mockito unit tests (`AiFunctionServiceUnitTest`, `TicketServiceUnitTest`)
- Security tests (`SecurityIntegrationTest`)

### Frontend

```bash
cd frontend
npm run lint
npm run build
```

### CI

GitHub Actions runs backend tests + frontend lint/build on every push/PR to `main` (see `.github/workflows/ci.yml`).

---

## Postman collection

Import these files into Postman:

| File | Purpose |
|------|---------|
| `postman/SupportIQ.postman_collection.json` | All API requests |
| `postman/SupportIQ.local.postman_environment.json` | Local dev variables |

**Setup:**
1. Start backend: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
2. Import collection + environment
3. Run **Auth → Register** or **Auth → Login**
4. Token and `companyId` are saved automatically to collection variables
5. Run other folders in order

---

## Manual test checklist (Marzia)

### Auth
- [ ] Register a new company account
- [ ] Log out and log back in
- [ ] Confirm `/auth/me` returns profile + company name
- [ ] Wrong password returns error

### Documents & RAG
- [ ] Upload `samples/return-policy.pdf` from `/dashboard/documents`
- [ ] Wait for status **Processed**
- [ ] Search "How do refunds work?" — return policy chunk appears
- [ ] Ask AI "Can I return shoes after 30 days?" — answer cites Return Policy

### Chat widget
- [ ] Open `/widget` while logged in
- [ ] Ask "Where is my order #48291?" — shipped + tracking
- [ ] Ask "I need to speak to a human agent" — ticket created

### Conversations (agent inbox)
- [ ] Open `/dashboard/conversations`
- [ ] Select a chat, reply as agent, mark resolved

### Tickets
- [ ] Open `/dashboard/tickets`
- [ ] Create ticket manually
- [ ] Assign to yourself, change status, add internal note

### Analytics
- [ ] Open `/dashboard/analytics`
- [ ] Confirm 7-day trend, ticket breakdown, top questions after widget chats

### Security edge cases
- [ ] Support Agent cannot change company settings (Team page ok, Settings blocked)
- [ ] Support Agent cannot upload documents (view only)
- [ ] API calls without JWT return 401/403

---

## Sample test data

After registration, demo order **#48291** is seeded automatically (shipped, with tracking).

Sample files in `/samples`:
- `return-policy.pdf` / `return-policy.md`
- `shipping-faq.txt`

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| 401 on dashboard API calls | Log in again — H2 dev DB resets on backend restart |
| Upload 500 error | Use `dev` profile; confirm H2 dialect in `application-dev.yml` |
| Widget shows "Log in" | Must be logged in — widget uses your `companyId` |
| Empty analytics | Send a few widget messages first |
