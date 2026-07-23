# Database Schema — SupportIQ

PostgreSQL 16, accessed through Spring Data JPA / Hibernate. Schema is generated from the JPA entities (`ddl-auto: update` in dev). Embeddings are stored on each chunk as a float array and ranked in the application layer with cosine similarity.

## Entity relationships

```
companies ──┬── users
            ├── company_users ── users        (team membership + role)
            ├── documents ── document_chunks   (content + embedding)
            ├── conversations ── messages
            │                 └── feedback
            ├── tickets
            └── orders ── refunds

users ── notifications
```

Every tenant-owned row hangs off `companies`, and every query is scoped by `company_id` so tenants stay isolated.

## Tables

### `companies`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| name | VARCHAR NOT NULL | |
| slug | VARCHAR UNIQUE NOT NULL | URL-safe identifier |
| website | VARCHAR | |
| ai_system_prompt | TEXT | Custom AI instructions |
| active | BOOLEAN DEFAULT true | |
| created_at / updated_at | TIMESTAMPTZ | |

### `users`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| email | VARCHAR UNIQUE NOT NULL | |
| password_hash | VARCHAR NOT NULL | BCrypt |
| first_name / last_name | VARCHAR NOT NULL | |
| role | ENUM | `ADMIN`, `SUPPORT_AGENT`, `CUSTOMER` |
| company_id | BIGINT FK → companies | |
| email_verified | BOOLEAN DEFAULT false | |
| active | BOOLEAN DEFAULT true | |
| created_at / updated_at | TIMESTAMPTZ | |

### `company_users`
Join table for team membership; a user's effective role in a company lives here.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| user_id | BIGINT FK NOT NULL | |
| company_id | BIGINT FK NOT NULL | |
| role | ENUM | `ADMIN`, `SUPPORT_AGENT`, `CUSTOMER` |
| created_at / updated_at | TIMESTAMPTZ | |

### `documents`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| title | VARCHAR NOT NULL | |
| type | ENUM | `PDF`, `WORD`, `FAQ`, `WEBSITE`, `MARKDOWN` |
| file_url | VARCHAR | Local path |
| content | TEXT | Extracted text |
| processed | BOOLEAN DEFAULT false | |
| active | BOOLEAN DEFAULT true | |
| created_at / updated_at | TIMESTAMPTZ | |

### `document_chunks`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| document_id | BIGINT FK NOT NULL | |
| content | TEXT NOT NULL | Chunk text |
| chunk_index | INT NOT NULL | Position within the document |
| embedding | REAL[] | OpenAI `text-embedding-3-small` (1536-dim); cosine similarity computed in-app |
| created_at / updated_at | TIMESTAMPTZ | |

### `conversations`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| customer_email / customer_name | VARCHAR | |
| status | ENUM | `ACTIVE`, `ESCALATED`, `RESOLVED`, `CLOSED` |
| resolved | BOOLEAN DEFAULT false | |
| created_at / updated_at | TIMESTAMPTZ | |

### `messages`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| conversation_id | BIGINT FK NOT NULL | |
| role | ENUM | `CUSTOMER`, `AI`, `AGENT`, `SYSTEM` |
| content | TEXT NOT NULL | |
| created_at / updated_at | TIMESTAMPTZ | |

### `tickets`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| company_id | BIGINT FK NOT NULL | |
| conversation_id | BIGINT FK | Linked conversation |
| subject | VARCHAR NOT NULL | |
| description | TEXT | |
| status | ENUM | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| priority | ENUM | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| assigned_to_id | BIGINT FK → users | |
| internal_notes | TEXT | Agent-only |
| customer_email | VARCHAR | |
| created_at / updated_at | TIMESTAMPTZ | |

### `orders` / `refunds`
Back the AI order/refund tools. `orders`: order_number (unique), customer_email, status (`PENDING`…`CANCELLED`), total_amount, tracking_number, expected_delivery_at. `refunds`: order_id FK, amount, reason, status (`REQUESTED`…`REJECTED`).

### `notifications` / `feedback`
`notifications`: user_id FK, type, title, message, read flag. `feedback`: conversation_id FK, rating (1–5), comment, resolved flag.
