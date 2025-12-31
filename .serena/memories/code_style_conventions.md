# Sentinel - Code Style & Conventions

## Backend (Java/Spring Boot)

### Package Structure
Domain-driven organization:
```
com.pjsent.sentinel/
├── {domain}/
│   ├── controller/   # REST controllers
│   ├── service/      # Business logic
│   ├── repository/   # Data access
│   ├── entity/       # JPA entities
│   ├── dto/          # Data transfer objects
│   └── config/       # Domain-specific config
└── common/
    ├── config/       # Shared configurations
    └── exception/    # Global exception handling
```

### Annotations & Patterns
- **Controllers**: `@RestController`, `@RequestMapping`
- **Services**: `@Service`, `@Transactional(readOnly = true)` for queries
- **Repositories**: `@Repository`, extend `JpaRepository`
- **DTOs**: Use Java records or Lombok `@Data`
- **Logging**: Lombok `@Slf4j`
- **Constructor injection**: Lombok `@RequiredArgsConstructor`

### Naming Conventions
- **Classes**: PascalCase (`PortfolioService`)
- **Methods**: camelCase (`getPortfolioById`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **Packages**: lowercase (`portfolio.service`)
- **DTOs**: Suffix with `Dto`, `Request`, `Response`

### Code Style
```java
// Service example
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    /**
     * 포트폴리오 조회
     * Korean comments for business logic explanations
     */
    @Cacheable(value = "portfolios", key = "#portfolioId")
    public PortfolioDto getPortfolioById(Long portfolioId) {
        log.info("포트폴리오 조회. ID: {}", portfolioId);
        // ...
    }
}
```

### Caching
- Use `@Cacheable`, `@CacheEvict` annotations
- Cache names: lowercase with camelCase (`stockPrice`, `portfolios`)

### Exception Handling
- Use `BusinessException` for business logic errors
- Use `ResourceNotFoundException` for 404 cases
- Global exception handler in `GlobalExceptionHandler`

## Frontend (TypeScript/React)

### File Structure
```
app/
├── {route}/
│   └── page.tsx     # Page component
components/
├── ui/              # Reusable UI components
└── {feature}/       # Feature-specific components
lib/
├── api/             # API client functions
└── utils/           # Utility functions
```

### Naming Conventions
- **Components**: PascalCase (`PortfolioCard.tsx`)
- **Files**: kebab-case or PascalCase for components
- **Functions**: camelCase (`fetchPortfolio`)
- **Types/Interfaces**: PascalCase with `I` prefix or no prefix

### TypeScript
- Strict mode enabled
- Use explicit types, avoid `any`
- Prefer interfaces for object shapes

## Git Conventions

### Branch Naming
- `feat/{feature-name}` - New features
- `fix/{bug-name}` - Bug fixes
- `perf/{optimization}` - Performance improvements

### Commit Messages
- `feat:` New feature
- `fix:` Bug fix
- `perf:` Performance improvement
- `refactor:` Code refactoring
- `test:` Adding tests
- `docs:` Documentation

### Important Rules
- ❌ No direct push to `main`
- ❌ No Claude signature in commits
- ✅ Atomic commits per feature
- ✅ Korean comments for business logic

## Development Principles

1. **Spec-First**: Follow `.claude/specs/API_*.md` for API implementation
2. **Plan-First**: Use `/feature-planner` skill for complex features
3. **Test-First**: TDD cycle (Red → Green → Refactor)
4. **Evidence-Based**: Performance changes require k6 measurements
