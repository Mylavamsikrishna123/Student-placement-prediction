# 🚀 Student Placement System - Setup Guide

## Quick Start (5 minutes)

### Prerequisites
- **Java 17+** (required) - [Download](https://www.oracle.com/java/technologies/downloads/)
- **MySQL 8.0+** (required) - [Download](https://dev.mysql.com/downloads/mysql/)
- **Python 3.8+** (optional, for frontend server) - [Download](https://www.python.org/downloads/)

### 1️⃣ Database Setup

#### Step 1: Create Database
```sql
CREATE DATABASE JAVAPROJECT;
```

#### Step 2: Apply Schema
```bash
mysql -u root -p JAVAPROJECT < database_schema.sql
```

**Default credentials:**
- Username: `root`
- Password: `root`
- Host: `localhost`
- Database: `JAVAPROJECT`

### 2️⃣ Configure Environment

Copy the example configuration:
```bash
cp .env.example .env
```

Edit `.env` if you have custom database credentials:
```
DB_URL=jdbc:mysql://localhost:3306/JAVAPROJECT
DB_USER=root
DB_PASS=root
```

### 3️⃣ Start Backend

**Windows:**
```bash
run-backend.bat
```

**Linux/Mac:**
```bash
DB_URL=jdbc:mysql://localhost:3306/JAVAPROJECT \
DB_USER=root \
DB_PASS=root \
java -jar backend/target/placement-backend-1.0.0-jar-with-dependencies.jar
```

Expected output:
```
Started HTTP server on port 8080
```

### 4️⃣ Start Frontend

Choose one option:

**Option A - Python (Recommended):**
```bash
python -m http.server 5500
```

**Option B - Live Server (VS Code Extension):**
- Install "Live Server" extension
- Right-click `index.html` → "Open with Live Server"

**Option C - Node.js:**
```bash
npx http-server -p 5500
```

### 5️⃣ Access Application

Open your browser:
- **User Portal:** http://localhost:5500
- **Admin Portal:** http://localhost:5500/admin_login.html
- **Backend API:** http://localhost:8080

---

## 📁 Project Structure

```
Student Placement System/
│
├── Frontend (HTML/CSS/JavaScript)
│   ├── index.html                 # User home page
│   ├── user_login.html            # User login
│   ├── user_register.html         # User registration
│   ├── student_dashboard.html     # Student profile & applications
│   ├── admin_login.html           # Admin login
│   ├── admin_dashboard.html       # Admin panel (company management)
│   └── style.css                  # Global styles
│
├── Backend (Java/Maven)
│   ├── src/
│   │   └── main/java/com/placement/
│   │       ├── App.java           # REST API endpoints
│   │       └── Db.java            # Database operations
│   ├── target/                    # Compiled JAR (auto-generated)
│   ├── pom.xml                    # Maven configuration
│   └── README.md                  # Backend documentation
│
├── Database
│   └── database_schema.sql        # MySQL schema
│
├── Configuration
│   ├── .env.example               # Environment template
│   └── .gitignore                 # Git ignore rules
│
└── Documentation
    ├── README.md                  # Main readme
    ├── SETUP.md                   # This file
    └── CLEANUP_REPORT.md          # Project cleanup info
```

---

## 🔧 Troubleshooting

### Backend Won't Start

**Issue:** `Failed to connect to database`
- Ensure MySQL is running
- Verify credentials in `.env`
- Check database exists: `SHOW DATABASES;`

**Issue:** Port 8080 already in use
- Kill existing process: `netstat -ano | findstr :8080`
- Or change port in `App.java`

### Frontend Shows Blank Page

- Check browser console (F12) for errors
- Ensure backend is running on port 8080
- Try hard refresh (Ctrl+Shift+R)

### Maven Not Found (if building from source)

Install Maven from: https://maven.apache.org/download.cgi

Add to PATH:
```bash
setx MAVEN_HOME "C:\path\to\apache-maven-3.9.x"
setx PATH "%PATH%;%MAVEN_HOME%\bin"
```

---

## 🔄 Build from Source (Optional)

If you need to rebuild the backend:

1. **Install Maven** (see troubleshooting)
2. **Build:**
   ```bash
   cd backend
   mvn clean package
   cd ..
   ```
3. **Run:**
   ```bash
   run-backend.bat
   ```

---

## 🔐 Security Notes

- ✅ Default admin credentials are hidden (not displayed in UI)
- ⚠️ Change default database password in production
- ⚠️ Never commit `.env` file (it's in `.gitignore`)
- ✅ CORS is configured for localhost:5500

---

## 📝 API Endpoints

### Companies
- `GET /api/companies` - List all companies
- `POST /api/companies` - Add company
- `PUT /api/companies/:id` - Update company
- `DELETE /api/companies/:id` - Delete company

### Users
- `POST /api/users/login` - User login
- `POST /api/users/register` - User registration
- `POST /api/admin/login` - Admin login

### Applications
- `GET /api/applications` - List applications
- `POST /api/applications` - Submit application
- `PUT /api/applications/:id` - Update application status

---

## ☁️ Deployment

### Railway (Backend)
1. Push code to GitHub
2. Connect GitHub repo to Railway
3. Set environment variables: `DB_URL`, `DB_USER`, `DB_PASS`
4. Deploy

### Netlify (Frontend)
1. Push code to GitHub
2. Connect GitHub repo to Netlify
3. Set build command: `echo "No build needed"`
4. Set publish directory: `.` (root)
5. Deploy

---

## 💡 Tips

- Use `Ctrl+Shift+Delete` to clear browser cache if seeing old data
- Check browser console (F12) for API errors
- Backend logs appear in terminal where `run-backend.bat` was executed
- Database can be managed with MySQL Workbench

---

## 🆘 Need Help?

1. Check `CLEANUP_REPORT.md` for project cleanup details
2. Review `backend/README.md` for backend-specific info
3. Check browser console (F12) for frontend errors
4. Check terminal output for backend errors

---

**Happy coding! 🎉**
