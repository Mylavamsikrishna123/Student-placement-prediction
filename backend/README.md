# Student Placement Prediction System - Backend

Java backend using SparkJava and MySQL JDBC for the Student Placement Prediction System.

## Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x

## Database Setup

1. Create the database using the provided schema:
```sql
mysql -u root -p < database_schema.sql
```

Or run the SQL file in your MySQL client. The schema includes:
- `students` - Student information
- `skills` - Master list of skills
- `student_skills` - Student skill levels (1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐)
- `companies` - Company information
- `company_skills` - Company required skills with levels
- `admin` - Admin credentials (default: admin@placement.com / admin123)

## Configure Database Connection

Set environment variables or adjust defaults in `Db.java`:

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

## Build & Run

```bash
cd backend
mvn clean package
java -jar target/placement-backend-1.0.0-jar-with-dependencies.jar
```

Server listens on `http://localhost:8080`.

## API Endpoints

### Authentication
- **POST** `/api/register` - Student registration
  ```json
  { "name", "idNumber", "email", "password", "department", "degree" }
  ```

- **POST** `/api/login` - Login (student or admin)
  ```json
  { "email", "password", "role": "student" | "admin" }
  ```

### Student Profile
- **POST** `/api/student/profile` - Save student profile with skills
  ```json
  { "email", "name", "idNumber", "department", "degree", "collegeName", "phone", "cgpa", "skills": {"Java": 3, "Python": 2} }
  ```

### Companies
- **GET** `/api/companies` - Get all companies
- **POST** `/api/companies` - Add company
  ```json
  { "name", "link", "skills": {"Java": 3, "Python": 2} }
  ```

### Eligibility
- **GET** `/api/eligible?cgpa=8.5&skills=Java:3,Python:2` - Get eligible companies

**Matching Rules:**
- If CGPA ≥ 9.0: Show all companies
- If CGPA ≥ 7.0: Show companies where student meets all required skill levels
- Skill levels: 1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐

## Frontend

The frontend HTML files are in the root directory. Serve them using:
```bash
python -m http.server 5500
```

Then open `http://localhost:5500/index.html` in your browser.

Frontend pages call the API at `http://localhost:8080`.
