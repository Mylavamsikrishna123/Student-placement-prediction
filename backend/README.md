# Student Placement Prediction System - Backend

Java backend built on the JDK's built-in `com.sun.net.httpserver.HttpServer` (no web framework) with JDBC against MySQL. A small custom JSON parser/serializer lives in `App.java` — there is no SparkJava or Gson dependency.

## Prerequisites
- Java 17+ (JDK)
- Maven 3.8+
- MySQL 8.x (H2 is used automatically for the test suite)

## Database Setup

1. Create the database using the provided schema (run from the repo root):

```bash
mysql -u root -p < database_schema.sql
```

The schema creates the `JAVAPROJECT` database and all tables the application references:
- `students` - Student information (BCrypt hash stored in `password`)
- `skills` - Master list of skills
- `student_skills` - Student skill levels (1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐)
- `companies` - Company information
- `company_skills` - Company required skills with levels
- `admin` - Admin credentials (default seeded: `admin@placement.com` / `admin123`, password stored as a BCrypt hash)
- `password_reset_tokens` - One-time reset tokens for the forgot/reset password flow

> Existing deployments should also apply the scripts in `migrations/` (e.g. making `student_id_number`/`degree` nullable). A fresh `database_schema.sql` already reflects those changes.

## Configure Database Connection

Set environment variables (defaults are `jdbc:mysql://localhost:3306/JAVAPROJECT`, user `root`, pass `root`):

**PowerShell:**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/JAVAPROJECT"
$env:DB_USER="root"
$env:DB_PASS="yourpassword"
```

**Linux/Mac:**
```bash
export DB_URL="jdbc:mysql://localhost:3306/JAVAPROJECT"
export DB_USER="root"
export DB_PASS="yourpassword"
```

Other env vars: `PORT` (backend port, default 8080), `ALLOWED_ORIGINS` (comma-separated CORS allow-list, default `http://localhost:5500`).

## Build & Run

```bash
cd backend
mvn clean package
java -jar target/placement-backend-1.0.0-jar-with-dependencies.jar
```

Server listens on `http://localhost:8080` and prints the list of registered endpoints on startup.

## API Endpoints

All endpoints are under `/api`. Authenticated requests must send `Authorization: Bearer <token>`.

### Authentication
- **GET** `/api/health` - Readiness check
- **POST** `/api/register` - Student registration `{ email, password }`
- **POST** `/api/login` - Login `{ email, password, role }` → returns `{ token, email, role }`
- **POST** `/api/forgot-password` `{ email }` - Returns a generic "if email exists, reset code sent" message
- **POST** `/api/verify-reset-token` `{ token }` - Validates a reset token
- **POST** `/api/reset-password` `{ token, newPassword }` - Resets the password

### Student Profile
- **GET** `/api/student/profile?email=...` - Get student profile
- **POST** `/api/student/profile` - Save/update profile (requires Bearer token; students can only edit their own profile)
  ```json
  { "email", "name", "idNumber", "department", "degree", "collegeName", "phone", "cgpa", "skills": {"Java": 3, "Python": 2}, "certifications", "backlogs" }
  ```

### Companies
- **GET** `/api/companies` - List all companies
- **POST** `/api/companies` - Add company `{ name, link, requiredCgpa, skills }`
- **PUT** `/api/companies/{id}` - Update company (admin token)
- **DELETE** `/api/companies/{id}` - Delete company (admin token)

### Eligibility
- **GET** `/api/eligible?cgpa=8.5&skills=Java:3,Python:2` - Get eligible companies
- **POST** `/api/eligibility/check` `{ email }` - Compute & persist eligibility for a student
- **GET** `/api/eligibility/results?studentId=...` - Get persisted eligibility results

**Matching Rules** (`/api/eligible`):
- If CGPA ≥ 9.0: Show all companies
- If CGPA ≥ 7.0: Show companies where student meets all required skill levels
- Skill levels: 1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐

## Testing

```bash
cd backend
mvn test
```

JUnit 5 tests. Pure unit tests run without a database; `Database`-exercising tests use in-memory H2 (MySQL mode) via the `Database(Connection)` test constructor. Tests requiring a live HTTP server + MySQL are `@Disabled` with instructions.

## Frontend

The frontend HTML files are in `../ui`. Serve them with:

```bash
cd ../ui
python -m http.server 5500
```

Then open `http://localhost:5500/index.html`. The frontend calls the API at `http://localhost:8080`.
