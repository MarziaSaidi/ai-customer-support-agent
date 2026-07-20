# API Contract

Base URL: `http://localhost:8080/api`

Authentication: `Authorization: Bearer <jwt>` unless marked **Public**.

Status legend: ✅ Implemented · 🔲 Planned · 🚧 Partial

---

## Auth

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| POST | `/auth/register` | Public | ✅ | Register user + create company |
| POST | `/auth/login` | Public | ✅ | Login, returns JWT |
| POST | `/auth/forgot-password` | Public | 🔲 | Send reset email (SendGrid) |
| POST | `/auth/reset-password` | Public | 🔲 | Reset with token |
| POST | `/auth/verify-email` | Public | 🔲 | Verify email with token |
| POST | `/auth/resend-verification` | JWT | 🔲 | Resend verification email |
| GET | `/auth/me` | JWT | ✅ | Current user profile |

### POST `/auth/register`

**Request:**
```json
{
  "email": "admin@acme.com",
  "password": "securepass123",
  "firstName": "Jane",
  "lastName": "Doe",
  "companyName": "Acme Corp"
}
```

**Response (201):**
```json
{
  "token": "eyJ...",
  "email": "admin@acme.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "ADMIN",
  "companyId": 1
}
```

### POST `/auth/login`

**Request:**
```json
{
  "email": "admin@acme.com",
  "password": "securepass123"
}
```

**Response (200):** Same shape as register.

### GET `/auth/me`

**Response (200):**
```json
{
  "id": 1,
  "email": "admin@acme.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "ADMIN",
  "companyId": 1,
  "companyName": "Acme Corp",
  "emailVerified": false
}
```

---

## Companies

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| GET | `/companies/{id}` | JWT | ✅ | Get company profile |
| PUT | `/companies/{id}` | JWT Admin | 🔲 | Update company details |
| POST | `/companies/{id}/settings` | JWT Admin | ✅ | Update AI settings |
| GET | `/companies/{id}/members` | JWT | 🔲 | List team members |
| POST | `/companies/{id}/invites` | JWT Admin | 🔲 | Invite team member |
| DELETE | `/companies/{id}/members/{userId}` | JWT Admin | 🔲 | Remove member |

### POST `/companies/{id}/settings`

**Request:**
```json
{
  "aiSystemPrompt": "You are Acme Corp support. Be friendly and concise."
}
```

---

---

## Knowledge / RAG

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| POST | `/knowledge/ask` | JWT | ✅ | Ask a question using company docs (RAG) |

### POST `/knowledge/ask`

**Request:**
```json
{
  "companyId": 1,
  "question": "Can I return shoes after 30 days?"
}
```

**Response (200):**
```json
{
  "answer": "Based on Return Policy, returns are accepted within 60 days if unused with tags attached.",
  "sources": [
    {
      "documentId": 3,
      "documentTitle": "Return Policy",
      "excerpt": "Returns accepted within 60 days if unused with tags attached.",
      "score": 0.84
    }
  ]
}
```

---

## Documents (Knowledge Base)

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| GET | `/documents` | JWT | ✅ | List company documents |
| POST | `/documents` | JWT Admin | ✅ | Upload document (multipart) |
| POST | `/documents/search` | JWT | ✅ | Semantic search over document chunks |
| GET | `/documents/{id}` | JWT | 🔲 | Get document metadata |
| DELETE | `/documents/{id}` | JWT Admin | ✅ | Soft-delete document |
| POST | `/documents/{id}/reprocess` | JWT Admin | ✅ | Re-queue processing job |

### POST `/documents/search`

**Request:**
```json
{
  "companyId": 1,
  "query": "How do refunds work?",
  "limit": 5
}
```

**Response (200):**
```json
[
  {
    "chunkId": 12,
    "documentId": 3,
    "documentTitle": "Return Policy",
    "content": "Refunds are processed within 5-7 business days...",
    "chunkIndex": 0,
    "score": 0.84
  }
]
```

---

### POST `/documents`

**Request:** `multipart/form-data`
- `file` — PDF, DOCX, MD, TXT
- `title` — string
- `type` — `PDF` | `WORD` | `FAQ` | `WEBSITE` | `MARKDOWN`
- `companyId` — long

**Response (201):**
```json
{
  "id": 1,
  "title": "Return Policy",
  "type": "PDF",
  "processed": false,
  "fileUrl": "/uploads/1/return-policy.pdf"
}
```

---

## Chat

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| POST | `/chat/widget/sessions` | Public | ✅ | Start widget session |
| POST | `/chat/widget/sessions/{id}/messages` | Public | ✅ | Send customer message |
| GET | `/chat/sessions` | JWT | ✅ | List company conversation summaries |
| GET | `/chat/sessions/{id}` | JWT | ✅ | Get conversation + messages |
| POST | `/chat/sessions/{id}/messages` | JWT Agent | ✅ | Agent reply |
| POST | `/chat/sessions/{id}/escalate` | Public/JWT | ✅ | Escalate to human |
| POST | `/chat/sessions/{id}/feedback` | Public | 🔲 | Submit rating |
| POST | `/chat/sessions/{id}/resolve` | JWT Agent | ✅ | Mark resolved |

### POST `/chat/widget/sessions`

**Query:** `companyId`, `customerEmail?`, `customerName?`

**Response (201):**
```json
{
  "id": 42,
  "status": "ACTIVE",
  "customerEmail": "customer@example.com",
  "customerName": "John",
  "resolved": false,
  "createdAt": "2026-07-14T18:00:00Z",
  "messages": []
}
```

### POST `/chat/widget/sessions/{id}/messages`

**Request:**
```json
{
  "content": "Where is my order #48291?"
}
```

**Response (200):** Session with updated `messages` array.

---

## Tickets

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| GET | `/tickets` | JWT | ✅ | List tickets (`?companyId=`) |
| POST | `/tickets` | JWT | ✅ | Create ticket |
| GET | `/tickets/{id}` | JWT | ✅ | Get ticket detail |
| PATCH | `/tickets/{id}` | JWT Agent | ✅ | Update status/priority/assignee |
| POST | `/tickets/{id}/notes` | JWT Agent | ✅ | Add internal note |

### POST `/tickets`

**Query:** `companyId`

**Request:**
```json
{
  "subject": "Refund request",
  "description": "Customer wants refund for order #48291",
  "priority": "MEDIUM",
  "customerEmail": "customer@example.com"
}
```

---

## Orders & Refunds (AI Actions)

These actions are invoked by the AI via function calling (Day 9). Direct REST endpoints remain planned.

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| GET | `/orders` | JWT | 🔲 | List orders |
| GET | `/orders/{orderNumber}` | JWT/AI | 🔲 | Order status lookup |
| PATCH | `/orders/{orderNumber}` | JWT/AI | 🔲 | Cancel or update address |
| POST | `/orders/{orderNumber}/refunds` | JWT/AI | 🔲 | Request refund |
| GET | `/refunds` | JWT | 🔲 | List refunds |

**AI tools (via chat):** ✅ `checkOrderStatus`, `createTicket`, `cancelOrder`, `requestRefund`, `searchDocumentation`

---

## Analytics

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| GET | `/analytics` | JWT | ✅ | Dashboard metrics with trends (`?companyId=`) |
| GET | `/analytics/questions` | JWT | ✅ | Most common customer questions |

### GET `/analytics`

**Response (200):**
```json
{
  "totalConversations": 150,
  "resolvedConversations": 112,
  "openTickets": 12,
  "resolvedTickets": 88,
  "aiResolutionRate": 74.7,
  "averageResponseTimeMs": 850,
  "customerSatisfaction": 4.2,
  "conversationTrend": [
    { "date": "2026-07-14", "count": 5 },
    { "date": "2026-07-15", "count": 8 }
  ],
  "topQuestions": [
    { "question": "Where is my order #48291?", "count": 12 }
  ],
  "ticketStatusBreakdown": {
    "open": 4,
    "inProgress": 2,
    "resolved": 10,
    "closed": 1
  }
}
```

---

## Notifications

| Method | Path | Auth | Status | Description |
|--------|------|------|--------|-------------|
| GET | `/notifications` | JWT | 🔲 | List user notifications |
| PATCH | `/notifications/{id}/read` | JWT | 🔲 | Mark as read |
| PATCH | `/notifications/read-all` | JWT | 🔲 | Mark all read |

---

## WebSocket (STOMP)

Endpoint: `ws://localhost:8080/ws` (SockJS fallback)

| Subscribe | Purpose | Status |
|-----------|---------|--------|
| `/topic/chat/{sessionId}` | Live messages | 🔲 |
| `/topic/typing/{sessionId}` | Typing indicators | 🔲 |
| `/queue/notifications/{userId}` | Agent alerts | 🔲 |

| Send | Purpose | Status |
|------|---------|--------|
| `/app/chat/{sessionId}/message` | Send message | 🔲 |
| `/app/chat/{sessionId}/typing` | Typing event | 🔲 |

---

## Error format

All errors return:

```json
{
  "timestamp": "2026-07-14T18:00:00Z",
  "status": 400,
  "message": "Human-readable error"
}
```

Validation errors include an `errors` object with field names.

---

## Rate limiting (planned)

| Endpoint group | Limit |
|----------------|-------|
| `/auth/*` | 10 req/min per IP |
| `/chat/widget/*` | 30 req/min per IP |
| Authenticated API | 100 req/min per user |
