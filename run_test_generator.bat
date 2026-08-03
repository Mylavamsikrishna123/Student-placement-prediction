@echo off
cd /d "%~dp0"
echo Creating Test Cases Excel File...
python create_test_excel.py
if %errorlevel% equ 0 (
    echo.
    echo ✅ Excel file created successfully!
    echo 📁 Look for TEST_CASES_DETAILED.xlsx in this folder
    echo.
    start TEST_CASES_DETAILED.xlsx
) else (
    echo ❌ Error creating file. Check Python installation.
    pause
)
