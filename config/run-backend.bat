@echo off
setlocal enabledelayedexpansion

REM ===================================================================
REM   Student Placement System - Backend Startup Script
REM ===================================================================

title Student Placement System - Backend
echo.
echo ============================================================
echo   Student Placement System - Backend Server
echo ============================================================
echo.

REM Check if JAR exists
set JAR_FILE=backend\target\placement-backend-1.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR file not found at: %JAR_FILE%
    echo.
    echo Please build the project first:
    echo   1. Install Maven from: https://maven.apache.org/download.cgi
    echo   2. Run: cd backend ^& mvn clean package
    echo   3. Run this script again
    echo.
    pause
    exit /b 1
)

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found! Please install Java 17 or later.
    echo Download from: https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

REM Load environment variables from .env if it exists
if exist ".env" (
    echo [INFO] Loading environment from .env file...
    for /f "usebackq tokens=* eol=# delims=" %%A in (".env") do (
        set "line=%%A"
        if not "!line!"=="" (
            for /f "tokens=1,* delims==" %%B in ("!line!") do (
                set "%%B=%%C"
            )
        )
    )
)

REM Set defaults if not in .env
if not defined DB_URL set DB_URL=jdbc:mysql://localhost:3306/JAVAPROJECT
if not defined DB_USER set DB_USER=root
if not defined DB_PASS set DB_PASS=root

echo [INFO] Database Configuration:
echo        URL: !DB_URL!
echo        User: !DB_USER!
echo.
echo [INFO] Starting backend server...
echo [INFO] Backend: http://localhost:8080
echo [INFO] Frontend: http://localhost:5500
echo.
echo Press Ctrl+C to stop the server
echo ============================================================
echo.

REM Run the backend
java -jar "%JAR_FILE%"

if errorlevel 1 (
    echo.
    echo [ERROR] Backend failed to start!
    echo.
    echo Possible causes:
    echo   - MySQL is not running
    echo   - Database credentials are incorrect
    echo   - Database JAVAPROJECT does not exist
    echo   - Port 8080 is already in use
    echo.
    pause
    exit /b 1
)

