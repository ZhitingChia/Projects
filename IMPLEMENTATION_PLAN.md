# Life Tracker — Knowledge Library

## Approved scope and architecture

Public email/password registration, verification and password reset; private multi-user libraries; edition-based books, rereads, reviews, no-cache external descriptions, interactive shelves, uploads and drawings. Deliver locally with an AWS guide; no AWS provisioning. Future enhancements from section 17 of Specifications.md are deferred.

React/TypeScript/Vite/Tailwind frontend; Java 21/Spring Boot 4.1 modular monolith; PostgreSQL 17/JPA/Flyway; Spring Security and JDBC sessions; Mailpit/SMTP; local and S3 storage adapters. Same-origin deployment, CSRF protection, owner-scoped access. Provider adapters for Google Books and Open Library. CSS/DOM shelf rendering rather than WebGL. No Redis or microservices.

Structure: frontend/src/{app,features,shared}; backend/src/main/java/com/lifetracker/{identity,knowledge,shared}; docs; e2e. Layers within knowledge separate controllers, services, entities, DTOs, repositories, providers and storage.

## Data/API decisions

UUID entities: users, hashed account_tokens, books, book_identifiers, book_sources, library_entries (unique user/book, metadata overrides and stable spine geometry), reading_sessions, artwork, durable storage_cleanup tasks; framework session tables. UTC timestamps and calendar-date reading fields. No description column. Exact identifiers deduplicate editions; uncertain manual records remain distinct. Personal corrections never alter shared canonical metadata. Final reading session deletion requires deleting the entry. One spine per edition regardless of rereads.

REST prefix /api/v1. Auth: csrf, register, verify-email, resend-verification, login, logout, me, forgot-password, reset-password. Books: search, provider edition details and transient description. Library: paginated list/create, detail/patch/delete, session create/patch/delete, drawing/upload attachment/removal, transient description. Artwork: multipart staging and authorized content. RFC9457 errors; version conflicts and duplicate creation return 409. Create requests carry client request UUIDs. Date default is the browser's explicit local calendar date.

Passwords: BCrypt with delegating encoder, 12–72 UTF-8 bytes. Verification tokens 24 hours, reset tokens 30 minutes, hashed and single-use. Generic account-recovery responses. Session rotation, CSRF and ownership checks. Images JPEG/PNG, <=5 MiB and 16 MP, decoded/re-encoded. Artwork staging expires in 24 hours; durable cleanup handles nontransactional storage operations.

## Phase checklist

- [x] 0 Foundation: scaffold, locked dependencies, Maven wrapper, PostgreSQL/Mailpit Compose, Flyway, CI, configuration. Gate passed: packaged stack runs and database-backed health responds.
- [x] 1 Identity: registration, verification, session login/logout, reset, CSRF, throttling, isolation. Gate passed: two users cannot access each other's records; tokens cannot replay; expired tokens fail and reset invalidates sessions.
- [x] 2 Book management: provider search and edition selection, manual entry, metadata overrides, reviews/dates/rereads, deletion, transient descriptions. Gate passed: persistence, duplicates, rollback and recovery tests pass.
- [x] 3 Interactive library: workstation, responsive shelves, stable spines, overflow, details, accessible list, reduced motion. Gate passed: refresh/reflow/deletion work; rereads add no spine; keyboard activation and mobile layout verified.
- [x] 4 Covers: upload/drawing staging, precedence, replacement, cleanup. Gate passed: artwork persists, foreign access fails, and deletion cleanup removes files; S3 adapter tested against a local HTTP stub.
- [x] 5 Release: E2E/security/accessibility/mobile checks, documentation, AWS guide. Gate passed locally: automated suites pass, WSL instructions and API contracts are delivered. AWS provisioning remains deferred.

Dependencies are sequential; add tests in each phase. Keep runnable after each gate. Do not mark unverified work complete. Track evidence and limitations below.

## Testing

Vitest/RTL: forms, dialogs, selection, local dates, shelf packing, cover precedence, retry state. JUnit/Mockito: validation, tokens, normalization, storage rules. PostgreSQL/Testcontainers: migrations, transactions, uniqueness, sessions, ownership and cleanup. WireMock: provider timeouts, malformed results, missing metadata and partial outages. Playwright: Mailpit registration/verification, login/reset, add/refresh/edit/reread/upload/draw/delete and cross-user isolation. Mock external providers in automated suites. Test keyboard navigation, reduced motion and screen-reader names. Load 1,000 records through pages of 100.

## AWS path and trade-offs

Guide: single EC2 application behind HTTPS reverse proxy, private RDS PostgreSQL, private S3, SES SMTP and Secrets Manager. No actual deployment or infrastructure provisioning. S3 authorization remains server-side. SES sender verification/production access is a deployment prerequisite. Session auth avoids browser tokens; modular monolith favors reliable transactions. Filesystem cleanup is eventually consistent. Description unavailability during outages is intentional because descriptions cannot be cached.

## Deferred / limitations

Statistics, achievements, organization tools, notes, import/export, other modules, MFA, account export/deletion and non-completed workflows are deferred. Model preserves all three reading statuses. Provider terms and cover rights must be checked before public operation. AWS cost/availability are not validated locally.

## Progress evidence

- Environment: WSL Ubuntu 26.04, native Docker Engine 29.9.1 and Compose. Windows browser reaches the WSL application through localhost. Builds/tests run with Linux Java 21/Maven and Node 24 containers. No host Java/Node installation is required for packaged delivery.
- Backend: `docker compose -f compose.yaml -f compose.tools.yaml run --rm backend-tools mvn -q verify` passed **10 tests**, none skipped: 3 PostgreSQL/HTTP integration scenarios, 2 provider scenarios, 2 storage scenarios and 3 validation scenarios.
- Large-library integration: 1,000 persisted books return pages of 100 with at most 10 JPA statements per request; newly added books remain visible on page one. Related records are batch fetched.
- Frontend: TypeScript and production Vite build passed; **9 Vitest/RTL tests** passed. Locked npm dependencies audited with **zero reported vulnerabilities**. Drawing code is a separate lazy-loaded bundle.
- Browser: `docker compose -f compose.yaml -f compose.e2e.yaml run --rm e2e` passed **5 tests**: full Chromium account/book/artwork/recovery workflow, desktop/mobile Chromium checks, Firefox smoke and WebKit smoke. Axe WCAG A/AA checks passed for registration, empty library and book details. Mobile library has no horizontal page overflow.
- Confirmed registration/verification, login/logout, review paragraph persistence and clearing, local date defaults, rereads, artwork priority/replacement, refresh persistence, private-resource isolation, CSRF rejection, duplicate prevention, stale versions, transaction rollback and durable cleanup.
- Deliverables: README with WSL execution and backup instructions, REST documentation, OpenAPI contract, Docker/tool/E2E Compose files and GitHub Actions workflow. The workflow itself has not run on GitHub; its build/test commands were executed locally.
- Maven wrapper version verified as 3.9.11. Spring Boot manages JUnit Jupiter/Testcontainers versions; Boot 4.1 uses the JUnit 6 generation of the same Jupiter test APIs. Mockito uses an explicit test JVM agent to avoid WSL self-attachment issues.
- Small implementation adjustment: books sort by newest date-added first, with descending ID tiebreaker. This keeps a newly saved book immediately visible when the library exceeds one page. Geometry/color stay stable. Provider-backed identifiers are re-verified server-side; uncertain/corrected identities can be converted to manual entries without losing metadata.
- Live provider probes: Open Library returned HTTP 200; unauthenticated Google Books returned HTTP 429. Automated tests verify partial-provider failure and manual entry. Configure a Google Books key for your own quota; provider availability is external.
- Validation is local. AWS deployment, real SES delivery, production load/abuse controls and a full manual screen-reader audit are not certified by these checks. Future enhancements remain deferred as approved.
