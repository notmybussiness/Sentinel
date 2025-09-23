@echo off
REM Sentinel MySQL Development Database Startup Script
REM Auto-starts MySQL Docker container for development

cd /d "%~dp0\.."

echo [INFO] Starting Sentinel MySQL Development Database...

REM Check if Docker is running
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

REM Check if container already exists and is running
docker ps -q -f name=sentinel-mysql-dev >nul 2>&1
if %errorlevel% equ 0 (
    echo [INFO] MySQL container is already running
    goto :health_check
)

REM Check if container exists but is stopped
docker ps -aq -f name=sentinel-mysql-dev >nul 2>&1
if %errorlevel% equ 0 (
    echo [INFO] Starting existing MySQL container...
    docker start sentinel-mysql-dev
    goto :health_check
)

REM Create and start new container
echo [INFO] Creating new MySQL container...
docker-compose -f docker-compose.dev.yml up -d

:health_check
echo [INFO] Waiting for MySQL to be ready...
timeout /t 5 /nobreak >nul

REM Wait for MySQL to be healthy
:wait_loop
docker exec sentinel-mysql-dev mysqladmin ping -h localhost -u sentinel_user -psentinel_pass >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] MySQL not ready yet, waiting...
    timeout /t 3 /nobreak >nul
    goto :wait_loop
)

echo [SUCCESS] MySQL Development Database is ready!
echo [INFO] Connection: localhost:3307
echo [INFO] Database: sentinel_dev
echo [INFO] User: sentinel_user
echo [INFO] Password: sentinel_pass

REM Optional: Open database client
REM start "" "mysql://sentinel_user:sentinel_pass@localhost:3307/sentinel_dev"

exit /b 0