@echo off
REM ===================================================================
REM   Student Placement System - Frontend Startup Script
REM ===================================================================

title Student Placement System - Frontend
echo.
echo ============================================================
echo   ^^ Student Placement System - Frontend Server
echo ============================================================
echo.

REM Check if Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found! Please install Python 3.8 or later.
    echo Download from: https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)

echo [INFO] Starting frontend server...
echo [INFO] Frontend URL: http://localhost:5500
echo.
echo Press Ctrl+C to stop the server
echo ============================================================
echo.

REM Change to parent directory (project root) to serve HTML files
cd ..

REM Start Python HTTP server on port 5500
python -m http.server 5500

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start frontend server!
    echo Port 5500 might already be in use.
    echo.
    pause
    exit /b 1
)
