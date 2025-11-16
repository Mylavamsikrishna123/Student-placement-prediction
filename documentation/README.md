# 🎓 Student Placement Prediction System

A complete web application for managing student placements and matching students with companies based on CGPA and technical skills.

**Status:** ✅ Production Ready | **Built:** Java + JavaScript | **Database:** MySQL

---

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Usage](#-usage)
- [API Documentation](#-api-documentation)
- [Deployment](#-deployment)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)

---

## ⚡ Quick Start

### Prerequisites
- **Java 17+** - [Download](https://www.oracle.com/java/technologies/downloads/)
- **MySQL 8.0+** - [Download](https://dev.mysql.com/downloads/mysql/)
- **Python 3.8+** (optional) - [Download](https://www.python.org/downloads/)

### 1️⃣ Setup Database

```bash
# Create database
mysql -u root -p
CREATE DATABASE JAVAPROJECT;
exit;

# Import schema
mysql -u root -p JAVAPROJECT < database_schema.sql
```

### 2️⃣ Start Backend

```bash
# Windows
run-backend.bat

# Linux/Mac
export DB_URL=jdbc:mysql://localhost:3306/JAVAPROJECT
export DB_USER=root
export DB_PASS=root
java -jar backend/target/placement-backend-1.0.0-jar-with-dependencies.jar
```

### 3️⃣ Start Frontend

```bash
# Windows
run-frontend.bat

# Linux/Mac
python -m http.server 5500
```

### 4️⃣ Access Application

- 🌐 User Portal: http://localhost:5500
- 👨‍💼 Admin Portal: http://localhost:5500/admin_login.html
- ⚙️ Backend API: http://localhost:8080

---

## ✨ Features

### 👨‍🎓 Student Dashboard
- ✅ User registration and authentication
- ✅ Profile management (CGPA, skills, personal info)
- ✅ Real-time placement eligibility checking
- ✅ View matching companies
- ✅ Apply to companies with automatic eligibility validation

### 👨‍💼 Admin Panel
- ✅ Secure admin login
- ✅ Add/Edit/Delete companies
- ✅ Set company requirements (CGPA, skills)
- ✅ Manage skill requirements
- ✅ View all applications

### 🤖 Smart Matching Algorithm
- **CGPA ≥ 9.0**: Show all companies
- **CGPA ≥ 7.0**: Show companies where student meets ALL required skill levels
- **CGPA < 7.0**: Show only eligible companies based on skill match
- Skills must match or exceed company requirements

---

## 🛠️ Technology Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | HTML5, CSS3, JavaScript (Vanilla) |
| **Backend** | Java 17, SparkJava Framework |
| **Database** | MySQL 8.x with JDBC |
| **Build** | Maven 3.9+ |
| **Deployment** | Railway (Backend), Netlify (Frontend) |

---

## 📁 Project Structure

```
📦 Student Placement System
│
├── 📄 Frontend Files
│   ├── index.html                 # 🏠 Landing page
│   ├── user_register.html         # 📝 Student registration
│   ├── user_login.html            # 🔐 Student login
│   ├── student_dashboard.html     # 📊 Student profile & eligibility
│   ├── admin_login.html           # 🔐 Admin login
│   ├── admin_dashboard.html       # ⚙️ Admin company management
│   └── style.css                  # 🎨 Global styles
│
├── 📦 Backend (Java)
│   ├── src/main/java/com/placement/
│   │   ├── App.java               # 🚀 REST API & endpoints
│   │   └── Db.java                # 💾 Database operations
│   ├── pom.xml                    # 📋 Maven configuration
│   ├── target/
│   │   └── *.jar                  # 🎁 Compiled backend
│   └── README.md                  # 📖 Backend docs
│
├── 🗄️ Database
│   └── database_schema.sql        # 📋 Schema definition
│
├── ⚙️ Configuration
│   ├── .env.example               # 🔧 Environment template
│   ├── .gitignore                 # 🚫 Git ignore rules
│   ├── run-backend.bat            # ▶️ Start backend (Windows)
│   └── run-frontend.bat           # ▶️ Start frontend (Windows)
│
└── 📖 Documentation
    ├── README.md                  # 📋 This file
    ├── SETUP.md                   # 🚀 Detailed setup guide
    └── CLEANUP_REPORT.md          # 🧹 Project cleanup info
```

---

## 🎯 Usage

### For Students

1. **Register** → `user_register.html`
2. **Login** → `user_login.html`
3. **Complete Profile** → Add CGPA and skills
4. **View Eligibility** → See matching companies
5. **Apply** → Click "Apply" button

### For Admin

1. **Login** → `admin_login.html` (credentials in system)
2. **Add Company** → Fill company details and required skills
3. **Edit Company** → Update requirements anytime
4. **Delete Company** → Remove outdated listings
5. **Manage** → View all registered students and applications

---

## 📡 API Documentation

### Companies Endpoint
```
GET  /api/companies           - List all companies
POST /api/companies           - Add new company
PUT  /api/companies/:id       - Update company
DELETE /api/companies/:id     - Delete company
```

### Users Endpoint
```
POST /api/users/login         - User authentication
POST /api/users/register      - User registration
POST /api/admin/login         - Admin authentication
```

### Applications Endpoint
```
GET  /api/applications        - List applications
POST /api/applications        - Submit application
PUT  /api/applications/:id    - Update application
```

### Request Body Examples

**Add Company:**
```json
{
  "companyName": "Google",
  "applicationLink": "https://google.com/careers",
  "requiredCgpa": 9.0,
  "skills": [
    {"skillName": "Java", "level": "Advanced"},
    {"skillName": "Problem Solving", "level": "Advanced"}
  ]
}
```

**Student Login:**
```json
{
  "email": "student@college.com",
  "password": "password123"
}
```

---

## ☁️ Deployment

### Deploy Backend to Railway

1. Push code to GitHub
2. Connect GitHub repo to [Railway](https://railway.app)
3. Set environment variables:
   ```
   DB_URL=jdbc:mysql://your-db:3306/JAVAPROJECT
   DB_USER=your-user
   DB_PASS=your-pass
   ```
4. Deploy!

### Deploy Frontend to Netlify

1. Push code to GitHub
2. Connect GitHub repo to [Netlify](https://netlify.com)
3. Build command: (leave empty)
4. Publish directory: `.` (root)
5. Deploy!

See `SETUP.md` for detailed instructions.

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| **Backend won't start** | Ensure MySQL is running and database exists |
| **"Port 8080 already in use"** | Kill existing process or change port in App.java |
| **Frontend shows blank** | Hard refresh (Ctrl+Shift+R) and check browser console |
| **Login fails** | Verify database schema is loaded correctly |
| **"Database not found"** | Run: `mysql -u root -p JAVAPROJECT < database_schema.sql` |

See `SETUP.md` for more troubleshooting.

---

## 📈 Performance

- **Database Queries**: Optimized with proper indexing
- **CORS**: Configured for frontend/backend communication
- **Caching**: Browser caching enabled for static assets
- **Build**: Pre-compiled JAR for instant startup

---

## 🔐 Security

- ✅ Password stored (should be hashed in production)
- ✅ Admin credentials hidden from UI
- ✅ CORS restricted to localhost:5500
- ✅ SQL injection prevention with prepared statements
- ✅ Environment variables for sensitive data

**Production Recommendations:**
- Use HTTPS instead of HTTP
- Add password hashing (bcrypt)
- Implement JWT tokens
- Add rate limiting
- Use environment variables for all secrets

---

## 📝 Database Schema

**Tables:**
- `users` - Student accounts
- `companies` - Company listings
- `applications` - Student applications
- `admin` - Admin credentials (default loaded)

**Key Fields:**
- `users.cgpa` - Student CGPA (0-10)
- `companies.required_cgpa` - Required CGPA
- `users.skills` - JSON array of skills
- `companies.skills` - JSON array of required skills

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 📞 Support

- 📖 Read `SETUP.md` for setup guidance
- 🔍 Check `CLEANUP_REPORT.md` for project structure
- 💬 Create an issue for bugs or feature requests
- 📧 Contact: Feel free to reach out

---

## ✅ Project Status

- ✅ User registration & authentication
- ✅ Student profile management
- ✅ Company management (admin)
- ✅ Placement eligibility algorithm
- ✅ Application system
- ✅ Database schema
- ✅ REST API
- ✅ Frontend UI
- ✅ CORS configuration
- ✅ Production deployment ready

---

**Built with ❤️ for Student Placements**

**[Setup Guide](SETUP.md) | [Backend Docs](backend/README.md) | [Cleanup Report](CLEANUP_REPORT.md)**
│   ├── pom.xml                # Maven dependencies
│   └── README.md              # Backend documentation
└── README.md                  # This file
```

## Setup Instructions

### 1. Database Setup

Create the MySQL database:

```bash
mysql -u root -p < database_schema.sql
```

Or run the SQL file in your MySQL client.

### 2. Backend Setup

Navigate to the backend directory:

```bash
cd backend
```

Set database environment variables (PowerShell):

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/JAVAPROJECT"
$env:DB_USER="root"
$env:DB_PASS="yourpassword"
```

Build and run:

```bash
mvn clean package
java -jar target/placement-backend-1.0.0-jar-with-dependencies.jar
```

Backend runs on `http://localhost:8080`

### 3. Frontend Setup

In the project root, start a local server:

```bash
python -m http.server 5500
```

Or use any static file server. Open `http://localhost:5500/index.html` in your browser.

## Usage

### For Students

1. Register at the registration page with your details
2. Login with your email and password
3. Fill in your profile (name, ID, department, degree, CGPA)
4. Select skill levels for each skill (Beginner⭐, Medium⭐⭐, Advanced⭐⭐⭐)
5. Click "Save Profile" to save your information
6. Click "Show Eligible Companies" to see companies you're eligible for

### For Admins

1. Login at admin login page (default: admin@placement.com / admin123)
2. Add companies with required skills and skill levels
3. Companies will be matched to students based on CGPA and skill requirements

## API Endpoints

See `backend/README.md` for detailed API documentation.

## Database Schema

The system uses normalized tables:
- `students` - Student information
- `skills` - Master list of skills
- `student_skills` - Student skill levels (many-to-many)
- `companies` - Company information
- `company_skills` - Company required skills (many-to-many)
- `admin` - Admin credentials

## Default Credentials

- **Admin**: admin@placement.com / admin123

## License

This project is for educational purposes.

