# AGENTS.md — Student Placement Prediction System

Repository notes for AI agents working on this project.

## Project layout
- `backend/` — Java 17 + Maven HTTP server (`com.placement.App` + `com.placement.Database`).
- `ui/` — Static HTML/JS frontend (Bootstrap). Talks to the backend over `http://localhost:8080/api`.
- `database_schema.sql` — canonical MySQL schema (the source of truth for tables).
- `migrations/` — additive migration files (notes say changes are folded into the schema).
- `test_validation_fixes.py` — manual smoke-test script against a running server (port 8080).

## Build & test
- Build: `cd backend && mvn clean package` → produces `target/placement-backend-1.0.0-jar-with-dependencies.jar`.
- Tests: `cd backend && mvn test` (282 tests, 0 failures expected; 14 skipped are HTTP integration tests needing a live server + MySQL).
- Run server: `PORT=8080 java -jar backend/target/placement-backend-1.0.0-jar-with-dependencies.jar` (needs MySQL at `jdbc:mysql://localhost:3306/JAVAPROJECT`, or set `DB_URL`/`DB_USER`/`DB_PASS`).

## Key conventions / gotchas
- **Password storage**: BCrypt (cost 12) hash stored in the `password` column of `students` and `admin`. There is NO plaintext fallback. The legacy `password_hash` column does NOT exist in production — any test referencing `password_hash` is drift and must use `password`.
- **Admin seed**: `admin@placement.com` / `admin123`, stored as a BCrypt hash in `database_schema.sql`.
- **`Database(Connection)` test injection**: `open()` wraps the injected connection in a `NonClosingConnection` so `try-with-resources` in individual methods does NOT close the shared test connection. Do not pass a raw `Connection` to per-call helpers and rely on it staying open.
- **DB validation noise**: the no-arg `Database()` constructor does NOT probe the DB; `validateConnection()` is called from `main()` only, so unit tests stay quiet.
- **Case-insensitive email**: all student lookups use `LOWER(email) = LOWER(?)`.
- **Security**: `/api/forgot-password` never returns the reset token in the HTTP response (it's only logged server-side for local dev). `Authorization: Bearer <token>` is required for protected endpoints via `requireAuth`.
- **Eligibility**: `getEligibleCompanies(cgpa, skills)` filters by BOTH required CGPA (`c.required_cgpa <= cgpa`) AND every required skill level. The `/api/eligibility/check` and `/api/eligible` branches must agree.
- **Frontend↔backend API paths** are aligned; companies PUT/DELETE use `/api/companies/{id}`.
- **Gitignore**: `backend/target/` is ignored except the deployment fat-jar; `.agents_tmp/` is ignored.

## Test classes
- Real unit/integration (H2 in-memory, MODE=MySQL): `RegistrationTest`, `ProfileManagementTest`, `EndToEndWorkflowTest`, `DatabaseIntegrationTest`, `AuthenticationUnitTest`, `AuthorizationTest`, `EligibilityCheckTest`, `AppTest` (static helpers), model tests.
- `AuthenticationTest` — HTTP integration tests, `@Disabled` (need a running server + MySQL).
