@echo off
REM ===================================================================
REM   Student Placement System - Production Startup
REM ===================================================================

setlocal enabledelayedexpansion
color 0B
cls

echo.
echo ================================================================
echo.
echo   STUDENT PLACEMENT SYSTEM - PRODUCTION STARTUP
echo.
echo ================================================================
echo.

REM Step 1: Check requirements
echo [1/6] Checking requirements...
python --version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo [ERROR] Python not found! Please install Python 3.8+
    pause
    exit /b 1
)

java -version >nul 2>&1
if errorlevel 1 (
    color 0C
    echo [ERROR] Java not found! Please install Java 17+
    pause
    exit /b 1
)

echo       [OK] Python and Java found
echo.

REM Step 2: Check MySQL is running
echo [2/6] Checking MySQL database...
sc query MySQL80 | find "RUNNING" >nul 2>&1
if errorlevel 1 (
    color 0C
    echo       [ERROR] MySQL is not running!
    echo       Please start MySQL: net start MySQL80
    echo.
    pause
    exit /b 1
)
echo       [OK] MySQL is running
echo.

REM Step 3: Kill any existing processes on ports 5500 and 8080
echo [3/6] Cleaning up old processes...
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| find ":5500" ^| find "LISTENING"') do (
    echo       Killing process on port 5500
    taskkill /pid %%a /f >nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| find ":8080" ^| find "LISTENING"') do (
    echo       Killing process on port 8080
    taskkill /pid %%a /f >nul 2>&1
)
timeout /t 2 /nobreak >nul
echo       [OK] Ports cleaned
echo.

REM Step 4: Start Backend Server
echo [4/6] Starting Backend Server...
echo       Cleaning and compiling on port 8080...
echo       This will take 15-20 seconds...
echo.

start "BACKEND - Student Placement System" cmd /k "cd /d "%~dp0backend" && echo ================================================================ && echo    BACKEND SERVER - PORT 8080 && echo ================================================================ && echo. && echo Compiling backend... && echo. && mvn clean compile exec:java -Dexec.mainClass=com.placement.App"

timeout /t 15 /nobreak

echo.
echo [5/6] Starting Frontend Server...

echo.
echo [5/6] Starting Frontend Server...
echo       Serving on port 5500...

start "FRONTEND - Student Placement System" cmd /k "cd /d "%~dp0ui" && echo ================================================================ && echo    FRONTEND SERVER - PORT 5500 && echo ================================================================ && echo. && echo Serving frontend at http://localhost:5500 && echo. && python -m http.server 5500"

timeout /t 3 /nobreak

echo.
echo [6/6] Performing health checks...
timeout /t 5 /nobreak

REM Try to check backend health
curl -s http://localhost:8080/api/health >nul 2>&1
if errorlevel 1 (
    color 0E
    echo       [WARN] Backend not responding yet
    echo       Wait 5 more seconds before logging in
) else (
    echo       [OK] Backend is responding
)

echo.
echo ================================================================
echo.
echo   SYSTEM READY!
echo.
echo ================================================================
echo.
echo   Frontend:  http://localhost:5500/index.html
echo   Backend:   http://localhost:8080/api/health
echo.
echo   LOGIN PAGES:
echo   - Admin:   http://localhost:5500/admin_login.html
echo   - Student: http://localhost:5500/user_login.html
echo.
echo   DEFAULT ADMIN CREDENTIALS:
echo   Email:    admin@placement.com
echo   Password: admin123
echo.
echo   Two windows are running (check taskbar):
echo   1. BACKEND - Student Placement System
echo   2. FRONTEND - Student Placement System
echo.
echo   To stop: Close both command windows or press Ctrl+C in each
echo.
echo ================================================================
echo.
echo   Wait 5-10 seconds for backend to fully start before logging in
echo.
echo ================================================================
echo.

REM Open browser
timeout /t 3 /nobreak
start http://localhost:5500/index.html

echo [INFO] Browser opened to home page
echo       Navigate to Admin Login or Student Login from there
echo.

pause
