# Database Schema — SupportIQ

PostgreSQL 16 with **pgvector**. Hibernate `ddl-auto: update` for local dev.

## Target schema (canonical — per PROJECT.md)

```
users ──┬── company_users ── companies
        │
companies ── documents ── document_chunks (embedding vector)
         └── conversations ── messages
                          └── tickets
```

| Table | Key columns |
|-------|-------------|
| `users` | id, email, password_hash |
| `companies` | id, name |
| `company_users` | user_id, company_id, role |
| `documents` | id, company_id, filename |
| `document_chunks` | id, document_id, content, embedding | ✅ Day 5 |
| `conversations` | id, company_id, customer_email |
| `messages` | id, conversation_id, role, content |
| `tickets` | id, conversation_id, status, priority |

## Current implementation vs target

| Target | Current code | Migration |
|--------|--------------|-----------|
| `company_users` | `users.company_id` + `users.role` | **Day 3** — add join table |
| `document_chunks` | `ai_embeddings` | ✅ **Day 5** — renamed |
| `conversations` | `chat_sessions` | **Day 8** — rename entity/table |
| `documents.filename` | `documents.title` + `file_url` | **Day 4** — align fields |

Extra tables in current code (kept for full spec): `orders`, `refunds`, `notifications`, `feedback`.

---

## Entity relationship (current code)

```
companies ──┬── users
            ├── documents ── ai_embeddings
            ├── chat_sessions ── messages
            │                 └── feedback
            ├── tickets
            └── orders ── refunds

users ── notifications
```

## Tables

### `companies`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| name | VARCHAR NOT NULL | |
| slug | VARCHAR UNIQUE NOT NULL | URL-safe identifier |
| website | VARCHAR | |
| logo_url | VARCHAR | |
| ai_system_prompt | TEXT | Custom AI instructions |
| active | BOOLEAN DEFAULT true | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `users`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| email | VARCHAR UNIQUE NOT NULL | |
| password_hash | VARCHAR NOT NULL | BCrypt |
| first_name | VARCHAR NOT NULL | |
| last_name | VARCHAR NOT NULL | |
| role | ENUM | `ADMIN`, `SUPPORT_AGENT`, `CUSTOMER` |
| company_id | BIGINT FK → companies | |
| email_verified | BOOLEAN DEFAULT false | |
| active | BOOLEAN DEFAULT true | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `documents`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| title | VARCHAR NOT NULL | |
| type | ENUM | `PDF`, `WORD`, `FAQ`, `WEBSITE`, `MARKDOWN` |
| file_url | VARCHAR | Local path or URL |
| content | TEXT | Extracted text |
| processed | BOOLEAN DEFAULT false | |
| active | BOOLEAN DEFAULT true | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `ai_embeddings`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| document_id | BIGINT FK NOT NULL | |
| chunk_text | TEXT NOT NULL | |
| chunk_index | INT NOT NULL | |
| embedding | vector(1536) | OpenAI text-embedding-3-small |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

**Index (planned):** `CREATE INDEX ON ai_embeddings USING ivfflat (embedding vector_cosine_ops);`

### `chat_sessions`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| customer_email | VARCHAR | |
| customer_name | VARCHAR | |
| status | ENUM | `ACTIVE`, `ESCALATED`, `RESOLVED`, `CLOSED` |
| assigned_agent_id | BIGINT FK → users | |
| resolved | BOOLEAN DEFAULT false | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `messages`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| session_id | BIGINT FK NOT NULL | |
| role | ENUM | `CUSTOMER`, `AI`, `AGENT`, `SYSTEM` |
| content | TEXT NOT NULL | |
| metadata | VARCHAR | JSON for actions/sources |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `tickets`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| session_id | BIGINT FK | Linked chat session |
| subject | VARCHAR NOT NULL | |
| description | TEXT | |
| status | ENUM | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| priority | ENUM | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| assigned_to_id | BIGINT FK → users | |
| internal_notes | TEXT | Agent-only |
| customer_email | VARCHAR | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `orders`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| order_number | VARCHAR UNIQUE NOT NULL | |
| customer_email | VARCHAR NOT NULL | |
| status | ENUM | `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED` |
| total_amount | DECIMAL NOT NULL | |
| shipping_address | VARCHAR | |
| tracking_number | VARCHAR | |
| shipped_at | TIMESTAMPTZ | |
| expected_delivery_at | TIMESTAMPTZ | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `refunds`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| order_id | BIGINT FK NOT NULL | |
| amount | DECIMAL NOT NULL | |
| reason | TEXT | |
| status | ENUM | `REQUESTED`, `APPROVED`, `PROCESSING`, `COMPLETED`, `REJECTED` |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `notifications`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| user_id | BIGINT FK NOT NULL | |
| type | ENUM | `TICKET_UPDATE`, `NEW_MESSAGE`, `TEAM_INVITE`, `DOCUMENT_PROCESSED`, `SYSTEM` |
| title | VARCHAR NOT NULL | |
| message | TEXT NOT NULL | |
| read | BOOLEAN DEFAULT false | |
| reference_id | VARCHAR | Ticket/session ID |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

### `feedback`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| session_id | BIGINT FK NOT NULL | |
| rating | INT NOT NULL | 1–5 |
| comment | TEXT | |
| resolved | BOOLEAN NOT NULL | |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

## Planned tables (Day 3+)

### `password_reset_tokens`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| user_id | BIGINT FK NOT NULL | |
| token | VARCHAR UNIQUE NOT NULL | |
| expires_at | TIMESTAMPTZ NOT NULL | |
| used | BOOLEAN DEFAULT false | |

### `email_verification_tokens`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| user_id | BIGINT FK NOT NULL | |
| token | VARCHAR UNIQUE NOT NULL | |
| expires_at | TIMESTAMPTZ NOT NULL | |
| verified_at | TIMESTAMPTZ | |

### `team_invites` (Day 7)

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| email | VARCHAR NOT NULL | |
| role | ENUM | |
| token | VARCHAR UNIQUE | |
| accepted | BOOLEAN DEFAULT false | |
| expires_at | TIMESTAMPTZ | |

## Redis keys (not in PostgreSQL)

| Key pattern | Purpose | TTL |
|-------------|---------|-----|
| `session:{id}` | Chat session cache | 24h |
| `faq:{companyId}` | Cached FAQ responses | 1h |
| `ratelimit:{ip}` | API rate limiting | 1m |

## Init script

`backend/src/main/resources/db/init.sql` enables pgvector:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```
