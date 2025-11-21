# Student Placement Prediction System - Deployment Guide

## Local Development

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Python 3.8+ (for frontend)

### Setup
1. Create MySQL database:
```bash
mysql -u root -p < database_schema.sql
```

2. Build backend:
```bash
cd backend
mvn clean package
```

3. Run backend:
```bash
java -jar target/placement-backend-1.0.0-jar-with-dependencies.jar
```

4. Run frontend (in separate terminal):
```bash
cd ui
python3 -m http.server 5500
```

5. Access application: `http://localhost:5500`

## Docker Deployment (Local)

### Build Docker image:
```bash
docker build -t sps:latest .
```

### Run Docker container:
```bash
docker run -p 8080:8080 -p 5500:5500 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/JAVAPROJECT \
  -e DB_USER=root \
  -e DB_PASS=password \
  sps:latest
```

## Render Deployment

### Prerequisites
- Render account (https://render.com)
- GitHub repository connected

### Steps
1. Connect your GitHub repository to Render
2. Render will automatically detect `render.yaml`
3. Set environment variables in Render dashboard:
   - `DB_URL`: MySQL connection string
   - `DB_USER`: Database username
   - `DB_PASS`: Database password
4. Deploy!

### MySQL Setup on Render
- Use MySQL Add-on from Render marketplace, or
- Connect to external MySQL database via `DB_URL`

## API Endpoints

### Authentication
- `POST /api/register` - Register new student
- `POST /api/login` - Login (student/admin)

### Student Profile
- `GET /api/student/profile?email=...` - Get student profile
- `POST /api/student/profile` - Update student profile

### Companies
- `GET /api/companies` - List all companies
- `POST /api/companies` - Add company (admin)
- `PUT /api/companies/:id` - Update company (admin)
- `DELETE /api/companies/:id` - Delete company (admin)

### Eligibility
- `GET /api/eligible?cgpa=...&skills=...` - Get eligible companies

## Architecture

### Backend
- Pure Java HttpServer (JDK 17+)
- No external web frameworks (removed Spark)
- Manual JSON parsing/serialization
- JDBC for database access
- Per-method connection management
- CORS support with OPTIONS handling

### Frontend
- Static HTML/CSS/JavaScript
- Bootstrap 5.3 for styling
- Fetch API for backend communication
- Session storage for user state

### Database
- MySQL with predefined schema
- Student, Company, Skill tables
- Many-to-many relationships

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_URL | jdbc:mysql://localhost:3306/JAVAPROJECT | MySQL connection URL |
| DB_USER | root | Database username |
| DB_PASS | root | Database password |
| PORT | 8080 | Backend server port |

## Health Check

Backend health check endpoint: `/api/health`
Returns: `{"status":"ok"}` with HTTP 200

## Troubleshooting

### "Address already in use"
- Port 8080 is in use. Change PORT env var or kill existing process.

### "Failed to fetch" in browser
- Check CORS headers: `Access-Control-Allow-Origin: *`
- Verify backend is running on correct port
- Check browser console for actual error

### Database connection error
- Verify MySQL is running
- Check DB_URL, DB_USER, DB_PASS environment variables
- Ensure JAVAPROJECT database exists

## License
MIT
