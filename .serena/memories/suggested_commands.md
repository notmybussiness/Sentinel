# Sentinel - Development Commands

## System Info
- **Platform**: Windows (MINGW64)
- **Shell utilities**: Standard unix commands via MINGW64 (ls, cd, grep, find, etc.)

## Backend Commands

### Build & Run
```bash
# Navigate to backend
cd backend

# Build project
./gradlew.bat build

# Run application (default profile)
./gradlew.bat bootRun

# Run with specific profiles
./gradlew.bat bootRun --args='--spring.profiles.active=dev,secret'
./gradlew.bat bootRun --args='--spring.profiles.active=perf,secret'

# Clean build
./gradlew.bat clean build
```

### Testing
```bash
# Run all tests
./gradlew.bat test

# Run specific test class
./gradlew.bat test --tests "PortfolioServiceTest"

# Run with test coverage
./gradlew.bat test jacocoTestReport
```

### Docker (Infrastructure)
```bash
# Start all services (PostgreSQL, Redis, Kafka)
cd backend
docker-compose up -d

# Start specific service
docker-compose up -d postgres
docker-compose up -d redis

# Stop all services
docker-compose down

# View logs
docker-compose logs -f redis
```

## Frontend Commands

### Development
```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Lint code
npm run lint
```

### E2E Testing (Playwright)
```bash
# Install browsers
npx playwright install

# Run E2E tests
npx playwright test

# Run with UI
npx playwright test --ui
```

## Python RAG Service

### Development
```bash
cd python-rag/embedding-service

# Install dependencies
pip install -r requirements.txt

# Start service
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Docker
```bash
cd python-rag
docker-compose up -d
```

## Performance Testing (k6)

```bash
# Install k6 (if not installed)
# Windows: choco install k6

# Run baseline test
k6 run backend/scripts/perf-tuning/exp01_db_indexing.js

# Run with specific VUs and duration
k6 run --vus 50 --duration 30s script.js
```

## Git Commands
```bash
# Create feature branch
git checkout -b feat/feature-name

# Standard commit workflow
git add .
git commit -m "feat: description"

# Push to remote
git push origin feat/feature-name
```

## Useful Paths
| Item | Path |
|------|------|
| Backend source | `backend/src/main/java/com/pjsent/sentinel/` |
| Frontend pages | `frontend/app/` |
| API specs | `.claude/specs/` |
| Feature plans | `docs/plans/` |
| Docker compose | `backend/docker-compose.yml` |
| Gradle config | `backend/build.gradle` |
