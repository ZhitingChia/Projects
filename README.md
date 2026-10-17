# Life Tracker — Knowledge Library

A private reading library with a cozy interactive bookshelf, edition search, reading history, reviews, rereads, uploaded covers and a drawing canvas. Public registration requires email verification; password recovery is included.

## Run in WSL Ubuntu 26.04

Docker Engine and Docker Compose are the only required local tools for the packaged application. From an Ubuntu terminal:

```bash
cd /mnt/d/Dev/Projects
cp .env.example .env
docker compose up --build -d
```

Open **http://localhost:8080** in your Windows browser. Register an account, open **http://localhost:8025** (Mailpit), follow its verification link, then sign in. Mailpit captures development emails; it does not send them externally. Google Books and Open Library are real providers in the normal stack. A Google Books API key is optional for development and can be supplied through `.env`.

If Docker reports `permission denied`, use `sudo docker compose ...` or deliberately configure Docker group access using your normal system administration workflow. Docker group membership grants root-equivalent access. The implementation was validated with WSL's native Docker Engine, not Windows Docker Desktop.

```bash
docker compose logs -f app
docker compose stop          # keep data
docker compose up -d        # resume
```

PostgreSQL and artwork use named volumes. `docker compose down` retains them; **`down -v` deletes data**. Development ports bind only to loopback. Do not expose this local configuration directly to the internet.

## Develop and test

Linux toolchain containers avoid mixing Windows and Linux Node dependencies:

```bash
docker compose -f compose.yaml -f compose.tools.yaml run --rm frontend-tools
docker compose -f compose.yaml -f compose.tools.yaml run --rm backend-tools
```

The frontend command installs locked dependencies, type-checks, builds and runs Vitest. Backend verification runs unit/provider tests and PostgreSQL integration tests through Testcontainers. Its tool container mounts the Docker socket solely to run those test containers; only run trusted project tests this way. Integration tests are skipped if Docker is unavailable, which does not constitute full validation.

For hot reload, install Linux Java 21 and Node 24 in WSL (do not reuse Windows `node_modules`), start dependencies with `docker compose up -d postgres mailpit`, then run in separate WSL terminals:

```bash
cd backend && bash mvnw spring-boot:run
cd frontend && npm ci && npm run dev
```

Open http://localhost:5173. The Vite proxy forwards `/api` to port 8080. The development backend defaults email links to port 5173; the packaged Compose application defaults them to 8080.

Compose reads `.env` automatically. A backend started directly with Maven uses exported environment variables; if you change the database credentials in `.env`, export matching `DATABASE_USER` and `DATABASE_PASSWORD` before starting it. Use Linux dependencies throughout a WSL development session.

Run browser tests using an **isolated Compose project**, with separate database volumes and automated provider fixtures:

```bash
docker compose -p life-tracker-e2e -f compose.yaml -f compose.e2e.yaml up --build -d app
docker compose -p life-tracker-e2e -f compose.yaml -f compose.e2e.yaml run --rm e2e
docker compose -p life-tracker-e2e -f compose.yaml -f compose.e2e.yaml down
```

Stop the normal stack first to release ports 8080, 5432, 1025 and 8025. Browser tests create disposable test accounts and books, and use Mailpit plus WireMock for deterministic provider behavior. Reports appear under `frontend/playwright-report` and `frontend/test-results`. Restart the normal stack after testing; do not use the provider fixtures for normal operation.

## Design and configuration

- `frontend/`: React, TypeScript, Tailwind, accessible dialogs, CSS shelves; drawing code is lazy-loaded.
- `backend/`: Java 21, Spring Boot, Spring Security, PostgreSQL/JPA and Flyway. Feature packages separate identity from knowledge.
- [Implementation plan](IMPLEMENTATION_PLAN.md): approved scope, decisions and progress evidence.
- [REST contract](docs/API.md): routes, request shapes and concurrency rules.
- [OpenAPI](docs/openapi.yaml): machine-readable API contract.
- [AWS guide](docs/AWS_DEPLOYMENT.md): deployment path; cloud provisioning is deferred.

Environment variables: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `APP_BASE_URL`, `SERVER_ADDRESS`, `PORT`, `COOKIE_SECURE`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_TLS`, `MAIL_FROM`, `GOOGLE_BOOKS_KEY`, `ARTWORK_PATH`, `STORAGE_TYPE`, `S3_BUCKET`, `AWS_REGION`. Do not commit real secrets. The JDBC URL format is `jdbc:postgresql://host:5432/database`.

Authentication uses HttpOnly session cookies, CSRF tokens and JDBC session persistence. Password-reset and verification links are single-use. Every personal record is owner-scoped. Shared canonical metadata remains after personal library deletion. External descriptions are fetched on demand and excluded from database, browser/query caches and logs. Reviews are independent persisted plain text.

Different editions are separate books. Exact ISBN/provider IDs prevent duplicates; uncertain manual records require a confirmation when a matching title exists. One edition has one spine and any number of completed reading sessions. Deleting the final session requires deleting the library entry. Personal metadata corrections do not change shared canonical metadata; changing an ISBN requires adding the correct edition.

Cover priority: drawing → uploaded image → provider → template. JPEG/PNG input is limited to 5 MiB and 16 megapixels, decoded/re-encoded into PNG, and stored privately. Unattached uploads expire after 24 hours; cleanup retries periodically. The canvas has a bounded undo history and saves flattened images, not editable vector strokes.

## Backup and restore

Back up PostgreSQL and artwork together during a write-free maintenance window. For the local stack:

```bash
docker compose stop app
docker compose exec -T postgres pg_dump -U lifetracker -Fc lifetracker > library.dump
docker compose run --rm --no-deps --entrypoint tar app -C /app/data/artwork -czf - . > artwork.tar.gz
docker compose start app
```

To restore into a **new empty database/volume**, stop the app, restore using `pg_restore --no-owner --no-privileges` inside the PostgreSQL container, and extract the artwork archive to its mounted artwork directory as UID 10001. Start the app and verify account login, book counts and artwork retrieval. Protect backups as personal data. Never restore over live data without a separate verified backup.

## Release boundaries

Future statistics, achievements, notes, import/export, other Life Tracker modules, MFA, account deletion/export and non-completed workflows remain deferred. AWS deployment has not been provisioned. Review external-provider terms, email deliverability and applicable privacy requirements before public hosting.
