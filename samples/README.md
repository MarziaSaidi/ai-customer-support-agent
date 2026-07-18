# Sample documents for testing uploads

Use these files to test **Day 4 — Document upload** at `/dashboard/documents`.

| File | Type | Use for |
|------|------|---------|
| `return-policy.pdf` | PDF | Main upload test |
| `return-policy.md` | Markdown | Markdown upload test |
| `shipping-faq.txt` | Text / FAQ | FAQ upload test |

## How to upload

1. Start backend: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
2. Start frontend: `npm run dev`
3. Open http://localhost:3000/dashboard/documents
4. Choose a file, set title (e.g. "Return Policy"), click **Upload**

## Test questions (after Day 7 RAG)

- "Can I return shoes after 30 days?"
- "How long does shipping take?"
