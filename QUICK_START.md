# Quick Start Guide

Get the Student Placement Prediction System running locally in a few minutes.

## Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+** (install via your package manager — do not use a bundled copy)
- **MySQL 8.x**
- **Python 3.x** (only to serve the static frontend)

## 1. Database Setup

From the repo root:

```bash
mysql -u root -p < database_schema.sql
```

This creates the `JAVAPROJECT` database with all tables (`students`, `skills`,
`student_skills`, `companies`, `company_skills`, `admin`, `password_reset_tokens`)
and seeds the default admin and common skills.

Default admin login: `admin@placement.com` / `admin123` (stored as a BCrypt hash).

## 2. Configure the Backend (optional)

The backend reads these environment variables (defaults shown):

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/JAVAPROJECT` | JDBC URL |
| `DB_USER` | `root` | DB user |
| `DB_PASS` | `root` | DB password |
| `PORT` | `8080` | Backend listen port |
| `ALLOWED_ORIGINS` | `http://localhost:5500` | Comma-separated CORS allow-list |

```bash
export DB_URL="jdbc:mysql://localhost:3306/JAVAPROJECT"
export DB_USER="root"
export DB_PASS="yourpassword"
export ALLOWED_ORIGINS="http://localhost:5500"
```

## 3. Build & Run the Backend

```bash
cd backend
mvn clean package
java -jar target/placement-backend-1.0.0-jar-with-dependencies.jar
```

The server prints its registered endpoints on startup and listens on
`http://localhost:8080` (or the port you set). Check it with:

```bash
curl http://localhost:8080/api/health
# {"status":"ok"}
```

## 4. Serve the Frontend

```bash
cd ui
python -m http.server 5500
```

Open `http://localhost:5500/index.html`.

## 5. Run the Tests

```bash
cd backend
mvn test
```

Pure unit tests run without a database. Tests that exercise `Database` use an
in-memory H2 database (MySQL compatibility mode), so no MySQL server is needed
for them. A few integration tests that require a live HTTP server + MySQL are
`@Disabled` with instructions in the test class.

## Smoke Test (manual)

1. Open `http://localhost:5500/user_register.html` and register with a valid
   email and a password of at least 8 characters.
2. Open `http://localhost:5500/user_login.html` and log in. An `authToken` is
   stored in `sessionStorage`.
3. On the student dashboard, save a profile. The request includes
   `Authorization: Bearer <token>` and returns `200`.
4. Log in as admin (`admin@placement.com` / `admin123`) to manage companies.

## Notes

- Passwords are hashed with BCrypt (cost 12) and stored in the `password`
  column. There is no plaintext fallback.
- `/api/forgot-password` returns only a generic "if the email exists, a reset
  code has been generated" message; it does **not** expose the token in the
  response. For local development the generated token is logged server-side.
- CORS reflects the `Origin` only for exact matches in `ALLOWED_ORIGINS`.
