# REST API contract

Base: `/api/v1`. JSON uses camelCase. UUIDs identify resources. All library, provider and artwork routes require a verified session. Owner IDs are never accepted from clients. Foreign resources return 404. No external descriptions appear in persisted DTOs.

## Authentication

| Method | Route | Request / response |
|---|---|---|
| GET | `/auth/csrf` | `{token, headerName}`; initialize before mutations |
| POST | `/auth/register` | `{email,password}` → 202 generic acknowledgement |
| POST | `/auth/resend-verification` | `{email}` → 202 |
| POST | `/auth/verify-email` | `{token}` → 204 |
| POST | `/auth/login` | `{email,password}` → `{email}` and session cookie |
| GET | `/auth/me` | `{email}` or 401 |
| POST | `/auth/logout` | 204, destroys session |
| POST | `/auth/forgot-password` | `{email}` → 202 generic acknowledgement |
| POST | `/auth/reset-password` | `{token,password}` → 204, invalidates all user sessions |

Use the returned CSRF header on every non-GET request. Fetch a fresh CSRF token after login/logout. Cookies must accompany requests; do not store credentials or bearer tokens in browser storage. Verification requires explicit POST after following its email link. Tokens travel in the link fragment and are cleared after successful use.

Password bounds are 12–72 UTF-8 bytes. Email uniqueness uses trimmed lowercase values. Verification expires in 24 hours; reset links expire in 30 minutes. Related unused tokens become invalid when one is consumed. Recovery endpoints avoid confirming account existence. Throttled requests return 429.

## Search and metadata

`GET /books/search?title=&author=&isbn=&page=0` returns `{items: Metadata[], warnings: string[]}`. At least one nonblank query is required; text fields max 300 characters, pages 0–20. ISBN validation includes checksums. Each provider supplies up to ten results per page. One failed provider does not suppress another's results. Empty results remain valid responses.

`GET /books/providers/{provider}/{editionId}` resolves a selected edition. Providers: `googlebooks`, `openlibrary`. Open Library identifiers must be edition IDs (`OL…M`), not work IDs. Search authors/publication dates from work-level results are provisional until edition confirmation.

Metadata:

```ts
type Metadata = {
  title: string;                   // required, max 500
  subtitle: string | null;
  authors: string[];               // required array; may be empty
  isbn10: string | null;
  isbn13: string | null;
  publisher: string | null;
  publicationDate: string | null;  // partial dates/year allowed
  language: string | null;
  pageCount: number | null;        // positive, <=100000
  coverUrl: string | null;         // supported provider hosts only
  provider: string | null;
  editionId: string | null;
  workId: string | null;
};
```

Provider and edition ID are supplied together. Missing metadata remains null, not fabricated. Optional strings can be cleared. ISBN-10 and ISBN-13 must represent the same edition when both are supplied.

Provider-backed saves re-fetch and verify edition identifiers server-side; clients cannot attach arbitrary aliases to shared records. If you need to correct the provider's ISBN or the provider becomes unavailable, convert the confirmation form to manual entry. Personal title/author/publication corrections remain editable. Open Library ISBN search resolves the exact ISBN edition rather than taking a work's first edition.

Descriptions: `GET /books/providers/{provider}/{editionId}/description` before saving, or `GET /library/books/{id}/description` afterward. Response `{text: string|null, source: string, workLevel: boolean}` uses `Cache-Control: no-store`. Text is plain text, labeled with its actual provider; work-level fallback is explicit. Clients must retain it only while the corresponding view is open, never in query caches or persistent storage. Unavailable descriptions do not prevent saving. Failures return 503 and may be retried.

## Library and reading history

`POST /library/books` → 201 `LibraryView`, with Location header:

```json
{
  "requestId": "unique-client-uuid",
  "metadata": {"title":"A book", "authors":[]},
  "reading": {
    "requestId":"unique-reading-uuid",
    "startDate":null,
    "completionDate":null,
    "localDate":"2026-10-17",
    "review":null
  },
  "drawingId":null,
  "uploadId":null,
  "confirmPossibleDuplicate":false
}
```

Omitted optional metadata fields deserialize as null. Supply the browser's local calendar date, never a date derived from UTC truncation. Blank completion uses `localDate`. Completion may not precede start. Reviews max 20,000 characters; null or empty clears an optional review.

Creation is transactional. Reusing an owner's request UUID returns the previous created record; use a new UUID for a genuinely new action. Retrying must reuse the original UUID. Exact edition duplicates return 409 with guidance to add a reread. Manual entries with matching personal titles require `confirmPossibleDuplicate: true`; no title-based automatic merge is performed.

| Method | Route | Behavior |
|---|---|---|
| GET | `/library/books?page=0&size=100` | `{items,hasMore,total}`, completed only, newest-added first with descending ID tiebreaker; max size 100 |
| GET | `/library/books/{id}` | Full LibraryView |
| PATCH | `/library/books/{id}` | `{version,metadata}`; updates only personal editable metadata |
| DELETE | `/library/books/{id}` | 204; deletes personal history/art references, queues file cleanup, retains canonical book |
| POST | `/library/books/{id}/sessions` | Reading request as above → 201 updated LibraryView |
| PATCH | `/library/books/{id}/sessions/{sessionId}` | `{version,startDate,completionDate,review}` → updated LibraryView |
| DELETE | `/library/books/{id}/sessions/{sessionId}` | 204; 409 if final reading session |

Metadata updates submit the complete metadata editor value. Missing optional fields in that value are cleared; this endpoint does not apply arbitrary JSON Merge Patch. Omitted fields outside the `metadata` object are not changed. ISBN/provider identity cannot be changed through personal correction; add the correct edition separately. Reading status is reserved in the schema; v1 creates completed entries only.

`LibraryView`: `{id,version,metadata,readingStatus,dateAdded,spineColor,spineWidth,spineHeight,coverUrl,artwork,sessions}`. Artwork items are `{id,type,url}`; sessions are `{id,version,startDate,completionDate,review}`. Colors and dimensions are stable across refreshes. Versions must match the latest returned version or mutations return 409. Reviews remain separate from descriptions. A reread returns the same entry ID and one spine.

## Artwork

- `POST /artwork?type=UPLOAD|DRAWING`: multipart field `file` → 201 `{id,type,url}`. JPEG/PNG only, <=5 MiB, <=16 megapixels. Files are decoded/re-encoded and stored under generated keys. Staging expires after 24 hours.
- `PUT /library/books/{id}/artwork/{type}`: `{artworkId}` attaches an owned staged image, replacing that type's slot. Returns updated LibraryView.
- `DELETE /library/books/{id}/artwork/{type}`: removes that slot and returns updated LibraryView.
- `GET /artwork/{id}/content`: owner-authorized PNG bytes, no-store. Storage keys and filesystem paths are never exposed.

Precedence: drawing, upload, provider, template. Remove a drawing to reveal an existing upload. Changing personal artwork updates the stored spine color. Filesystem/S3 deletion is eventually consistent through durable cleanup tasks; logical deletion is transactional.

## Errors and service status

Errors use `application/problem+json` with `status`, `detail`, optional field `errors`, and correlation ID for controller validation failures. Standard security-filter errors contain status/detail. No raw provider bodies or private data enter error messages. Relevant codes: 400 validation, 401 unauthenticated, 403 CSRF, 404 missing/foreign record, 409 duplicate/stale state, 413 upload too large, 429 throttled, 503 provider unavailable.

`GET /health` reports application/database availability without private details.
