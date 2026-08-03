@echo off
cd /d "%~dp0"
echo Starting backend server on port 8080...
mvn exec:java "-Dexec.mainClass=com.placement.App"
pause
