# Student Placement Prediction System

A full-stack web application for predicting student placement eligibility based on CGPA and skill levels. The backend is a lightweight Java 17 service built on the JDK's built-in `com.sun.net.httpserver.HttpServer` (no web framework), with a hand-rolled JSON helper and JDBC against MySQL. The frontend is vanilla HTML/CSS/JavaScript.

## Features

- **Student Registration & Authentication** - Secure registration and login with password hashing
- **Admin Dashboard** - Manage companies, view student data, and monitor system
- **Profile Management** - Students can update their profiles and skill levels
- **Company Management** - Admins can add/edit companies with required skill levels
- **Eligibility Prediction** - Automatic matching of students to eligible companies based on CGPA and skills
- **Forgot Password / Reset Password** - Secure password recovery via email tokens
- **Responsive UI** - Clean, modern interface that works on all devices

## Tech Stack

### Backend
- **Java 17+** using the built-in `com.sun.net.httpserver.HttpServer` (no web framework such as SparkJava)
- **MySQL 8.x** database via JDBC (`mysql-connector-java`)
- **Maven** for dependency management and packaging
- **BCrypt** (`jbcrypt`) for password hashing — hashes are stored in the `password` column
- A small custom JSON parser/serializer in `App.java` (no Gson dependency)

### Frontend
- **HTML5, CSS3, Vanilla JavaScript** (no frameworks)
- **Bootstrap** via CDN
- **Fetch API** for backend communication
- `sessionStorage` for auth-token / session management

### Database
- `students`, `skills`, `student_skills`, `companies`, `company_skills`, `admin` tables
- `password_reset_tokens` table for the forgot/reset password flow
- BCrypt password hashes live in the `password` column of `students`/`admin`

## Project Structure

```
Student-Placement-Prediction/
├── backend/                 # Java HttpServer backend
│   ├── src/
│   │   ├── main/java/com/placement/
│   │   │   ├── App.java           # Main entry point, HTTP routes, JSON helpers
│   │   │   ├── Database.java      # JDBC data access layer (BCrypt, transactions)
│   │   │   └── models/            # Data models (Student, Company, Skill)
│   │   └── test/                  # JUnit 5 test suite (H2 for integration tests)
│   ├── pom.xml                    # Maven configuration
│   └── README.md                  # Backend-specific documentation
├── ui/                          # Frontend HTML/CSS/JS
│   ├── index.html                 # Landing page
│   ├── user_login.html            # Student login
│   ├── user_register.html         # Student registration
│   ├── student_dashboard.html     # Student dashboard
│   ├── admin_login.html           # Admin login
│   ├── admin_dashboard.html       # Admin dashboard
│   ├── forgot_password.html       # Password recovery request
│   ├── reset_password.html        # Password reset form
│   └── style.css                  # Shared styles
├── database_schema.sql            # Complete database schema (incl. password_reset_tokens)
├── migrations/                    # Database migration scripts
├── Dockerfile                     # Multi-stage Docker build
├── start.bat                      # Windows startup script
└── QUICK_START.md                 # Quick start guide
```

## Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+**
- **MySQL 8.x**
- **Python 3.x** (for serving frontend) or any static file server

## Quick Start

### 1. Database Setup

```bash
# Create database and tables
mysql -u root -p < database_schema.sql
```

Default admin credentials: `admin@placement.com` / `admin123` (the admin password is stored as a BCrypt hash in `database_schema.sql`).

### 2. Configure Database Connection

Set environment variables:

**Windows (PowerShell):**
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

### 3. Build & Run Backend

```bash
cd backend
mvn clean package
java -jar target/placement-backend-1.0.0-jar-with-dependencies.jar
```

Backend runs on `http://localhost:8080`

### 4. Serve Frontend

```bash
# From project root
python -m http.server 5500
```

Frontend available at `http://localhost:5500`

Open `http://localhost:5500/index.html` in your browser.

## API Endpoints

The server listens on `http://localhost:8080` (override with the `PORT` env var). All responses are JSON.

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health/readiness check |

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/register` | Student registration `{ email, password }` |
| POST | `/api/login` | Login `{ email, password, role }` → returns Bearer token |
| POST | `/api/forgot-password` | Request a password reset token `{ email }` |
| POST | `/api/verify-reset-token` | Verify a reset token `{ token }` |
| POST | `/api/reset-password` | Reset password with token `{ token, newPassword }` |

### Student Profile
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/student/profile?email=...` | Get student profile |
| POST | `/api/student/profile` | Save/update profile (requires `Authorization: Bearer <token>`) |

### Companies
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/companies` | List all companies |
| POST | `/api/companies` | Add company |
| PUT | `/api/companies/{id}` | Update company (admin token) |
| DELETE | `/api/companies/{id}` | Delete company (admin token) |

### Eligibility
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/eligible?cgpa=8.5&skills=Java:3,Python:2` | Get eligible companies |
| POST | `/api/eligibility/check` | Compute & persist eligibility for a student `{ email }` |
| GET | `/api/eligibility/results?studentId=...` | Get persisted eligibility results |

**Matching Logic** (`/api/eligible`):
- CGPA ≥ 9.0: All companies shown
- CGPA ≥ 7.0: Companies where the student meets all required skill levels
- Skill levels: 1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐

> Note: `/api/eligibility/check` and `/api/eligibility/results` are the registered contexts. The server's startup banner lists the real endpoints.

## Testing

```bash
cd backend
mvn test
```

The test suite uses JUnit 5. Pure unit tests (model tests, the `App` static-helper tests) run without any database. Tests that exercise `Database` use an in-memory **H2** database in MySQL compatibility mode via the `Database(Connection)` test constructor, so no MySQL server is required for them. A small number of integration tests that need a live HTTP server + MySQL are `@Disabled` with an honest reason; see each test class for instructions on running them.

## Docker Deployment

```bash
docker build -t placement-system .
docker run -p 8080:8080 -e DB_URL=... -e DB_USER=... -e DB_PASS=... placement-system
```

## Documentation

- [QUICK_START.md](QUICK_START.md) - Quick start guide
- [backend/README.md](backend/README.md) - Backend-specific documentation

## License

This project is for educational purposes.