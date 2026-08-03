# Student Placement Prediction System

A full-stack web application for predicting student placement eligibility based on CGPA and skill levels. Built with Java (SparkJava) backend and vanilla HTML/CSS/JavaScript frontend.

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
- **Java 17+** with **SparkJava** framework
- **MySQL 8.x** database
- **Maven** for dependency management
- **BCrypt** for password hashing
- **Gson** for JSON serialization

### Frontend
- **HTML5, CSS3, Vanilla JavaScript** (no frameworks)
- **Fetch API** for backend communication
- **LocalStorage** for session management

### Database
- Students, Skills, Student_Skills, Companies, Company_Skills, Admin tables
- Password reset tokens table for forgot password functionality

## Project Structure

```
Student-Placement-Prediction/
├── backend/                 # Java SparkJava backend
│   ├── src/
│   │   ├── main/java/com/placement/
│   │   │   ├── App.java           # Main entry point & API routes
│   │   │   ├── Database.java      # Database connection & queries
│   │   │   └── models/            # Data models (Student, Company, Skill, etc.)
│   │   └── test/                  # Comprehensive test suite (25+ test files)
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
├── database_schema.sql            # Complete database schema
├── migrations/                    # Database migration scripts
├── Dockerfile                     # Docker configuration
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

Default admin credentials: `admin@placement.com` / `admin123`

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

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/register` | Student registration |
| POST | `/api/login` | Login (student/admin) |
| POST | `/api/forgot-password` | Request password reset |
| POST | `/api/reset-password` | Reset password with token |

### Student Profile
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/student/profile` | Save/update student profile |
| GET | `/api/student/profile?email=...` | Get student profile |

### Companies (Admin)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/companies` | List all companies |
| POST | `/api/companies` | Add new company |
| PUT | `/api/companies/{id}` | Update company |
| DELETE | `/api/companies/{id}` | Delete company |

### Eligibility
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/eligible?cgpa=8.5&skills=Java:3,Python:2` | Get eligible companies |

**Matching Logic:**
- CGPA ≥ 9.0: All companies shown
- CGPA ≥ 7.0: Companies where student meets all required skill levels
- Skill levels: 1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐

## Testing

```bash
cd backend
mvn test
```

Comprehensive test suite includes:
- Unit tests for all models and services
- Integration tests for database operations
- Authentication & authorization tests
- Edge case & boundary value tests
- Concurrency & error handling tests

## Docker Deployment

```bash
docker build -t placement-system .
docker run -p 8080:8080 -e DB_URL=... -e DB_USER=... -e DB_PASS=... placement-system
```

## Documentation

- [QUICK_START.md](QUICK_START.md) - Quick start guide
- [FORGOT_PASSWORD_IMPLEMENTATION.md](FORGOT_PASSWORD_IMPLEMENTATION.md) - Password reset feature details
- [TEST_EXECUTION_SUMMARY.md](TEST_EXECUTION_SUMMARY.md) - Test results summary
- [backend/README.md](backend/README.md) - Backend-specific documentation

## License

This project is for educational purposes.