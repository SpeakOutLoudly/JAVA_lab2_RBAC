@echo off
REM Quick start script for RBAC CLI System

echo ========================================
echo RBAC CLI System - Quick Start
echo ========================================
echo.

echo Step 1: Compiling project...
call mvn clean compile
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Starting application...
echo.
echo Default credentials:
echo   Username: admin
echo   Password: admin123
echo.
echo ========================================
echo.

call mvn exec:java -Dexec.mainClass="com.study.Main"

pause
