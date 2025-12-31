# Sentinel - Task Completion Checklist

## Before Starting a Task
1. Read relevant API spec from `.claude/specs/API_*.md`
2. For complex features, use `/feature-planner` skill to create a plan
3. Review existing code patterns in the codebase

## During Development

### Backend Changes
- [ ] Follow domain package structure
- [ ] Use appropriate annotations (@Service, @Transactional, etc.)
- [ ] Add logging with `log.info()` for important operations
- [ ] Use Korean comments for business logic explanations
- [ ] Apply caching where appropriate

### Frontend Changes
- [ ] Follow component structure patterns
- [ ] Use TypeScript types properly
- [ ] Handle loading/error states

## After Completing a Task

### Build & Compile
```bash
# Backend
cd backend && ./gradlew.bat build

# Frontend
cd frontend && npm run build
```

### Run Tests
```bash
# Backend unit tests
cd backend && ./gradlew.bat test

# Frontend E2E tests
cd frontend && npx playwright test
```

### Code Quality
```bash
# Frontend linting
cd frontend && npm run lint
```

### Verify Functionality
- [ ] Manual testing of new/changed features
- [ ] No regressions in existing functionality
- [ ] Edge cases handled

### Performance (if applicable)
For performance-related changes:
```bash
# Run k6 load test
k6 run backend/scripts/perf-tuning/{test-script}.js
```
Compare before/after metrics.

## Quality Gates (from feature-planner skill)

### Build & Compilation
- [ ] Project builds without errors
- [ ] No syntax errors

### Testing
- [ ] All existing tests pass
- [ ] New tests added for new functionality
- [ ] Test coverage maintained or improved

### Code Quality
- [ ] Linting passes
- [ ] Type checking passes
- [ ] Code formatting consistent

### Functionality
- [ ] Manual testing confirms feature works
- [ ] No regressions
- [ ] Edge cases tested

### Security & Performance
- [ ] No new security vulnerabilities
- [ ] No performance degradation

## Documentation Updates
- [ ] Update `.claude/CLAUDE.md` if project status changed
- [ ] Update relevant API spec if endpoints changed
- [ ] Update feature plan if completing a phase
