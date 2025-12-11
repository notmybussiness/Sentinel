# Branch Merge Plan

> **Created**: 2025-12-03
> **Purpose**: Consolidate experimental branches and merge Phase 4-6 to main

---

## 🎯 Current Situation

### Branch Status
```bash
* feature/phase4a-read-write-separation  # Current branch (Phase 4a, 5, 6 코드 포함)
  exp5a-fetch-join                       # Phase 3 실험 (EntityGraph)
  exp5b-batch-size                       # Phase 3 실험 (BatchSize)
  exp5c-entity-graph                     # Phase 3 실험 (EntityGraph)
  feature/cache-optimization             # Phase 1-2 실험
  origin                                 # Main branch (현재 Phase 3까지만 반영)
  awesome-franklin                       # 오래된 브랜치 (삭제 필요)
```

### Git Status (Staged Changes)
```
M .claude/settings.local.json
M build.gradle
M scripts/README.md
M src/main/java/com/pjsent/sentinel/config/CacheConfig.java
M src/main/java/com/pjsent/sentinel/portfolio/service/PortfolioService.java
M src/main/resources/application-perf.yml

D docs/spring-security-test-auth-issue.md
D scripts/export_metrics.py
D scripts/results/phase1_cache_experiments/* (다수 CSV 파일)
D scripts/results/phase2_external_api_optimization/* (다수 CSV 파일)
D scripts/results/phase3_db_optimization/* (다수 CSV 파일 및 Python 스크립트)

?? .claude/SETUP_COMPLETE.md
?? .claude/claude_code_prompt.md
?? .claude/sentinel_commands/
?? .gitattributes
?? docs/API_MAP.md
?? docs/INFRASTRUCTURE.md
?? docs/NEXT_PHASE_PLAN.md
?? docs/PROJECT_STRUCTURE.md
?? scripts/.gitattributes
?? scripts/PORTFOLIO_PERFORMANCE_REPORT.md
?? scripts/SCALABILITY_REPORT.md
?? scripts/common/
?? scripts/phase1_cache_experiments/
?? scripts/phase2_external_api_optimization/
?? scripts/phase3_db_optimization/
?? scripts/phase3_vs_phase4_comparison/
?? scripts/phase5_resource_opt/
?? scripts/phase6_scalability_opt/
```

---

## 📋 Merge Strategy

### Step 1: Clean Up Current Branch
**목적**: Git history 정리 및 불필요한 파일 삭제 확정

```bash
# 1. 삭제된 파일들 확정 (Git이 추적하지 않도록)
git add -A

# 2. 새로 추가된 문서들 추가
git add docs/*.md
git add .claude/*.md
git add .claude/backend/
git add scripts/PORTFOLIO_PERFORMANCE_REPORT.md
git add scripts/SCALABILITY_REPORT.md
git add scripts/phase5_resource_opt/
git add scripts/phase6_scalability_opt/

# 3. Commit (Phase 4-6 통합)
git commit -m "feat(phase4-6): integrate Read/Write separation, Virtual Threads, and Local Caching

- Phase 4a: Background scheduler for price updates (-30% response time)
- Phase 5: Virtual Threads + HikariCP 50 (discovered Connection Pool exhaustion)
- Phase 6: Local Caching (Caffeine) + HikariCP 100 (+202% TPS, resolved pool exhaustion)
- Update documentation: CLAUDE.md, EXPERIMENT_STATUS.md
- Add performance reports: PORTFOLIO_PERFORMANCE_REPORT.md, SCALABILITY_REPORT.md

Closes #phase4-6-optimization"
```

---

### Step 2: Merge to Main (origin)
**목적**: Main 브랜치에 Phase 4-6 반영

```bash
# 1. Main 브랜치로 이동
git checkout origin

# 2. feature/phase4a-read-write-separation 병합
git merge feature/phase4a-read-write-separation --no-ff

# 3. Merge commit 메시지
# Title: Merge Phase 4-6 Optimizations (Read/Write Separation + Local Caching)
# Body:
#   - Phase 4a: Background Scheduler (Avg Response: 496ms → 347ms)
#   - Phase 5: Virtual Threads + HikariCP 50
#   - Phase 6: Local Caching (TPS: 160 → 483, +202%)
#
#   Performance Improvements:
#   - TPS: +1,108% (40 → 483 req/s)
#   - Response Time: -92% (1,949ms → 155ms with cache)
#   - Error Rate: -100% (24.45% → 0%)
#   - DB Connection Pool: Exhaustion resolved
#
#   See EXPERIMENT_STATUS.md for detailed results.
```

---

### Step 3: Tag Release
**목적**: Phase 6 완료 시점 기록

```bash
# Create annotated tag
git tag -a v1.0-phase6-complete -m "Phase 6 Complete: Local Caching + HikariCP 100

Performance Highlights:
- TPS: 483 req/s (+202% from Phase 5)
- Cache Hit Rate: 50.4%
- Pending Connections: 0 (resolved)
- Avg Response: 155ms (-60%)

Architecture:
- Caffeine Local Cache (Portfolio, Market, Crypto)
- HikariCP Connection Pool: 100
- Virtual Threads: Enabled

Next: Phase 7 (Kafka EDA)"

# Push tag to remote
git push origin v1.0-phase6-complete
```

---

### Step 4: Delete Experimental Branches
**목적**: Git 정리 (더 이상 필요 없는 브랜치 삭제)

```bash
# Delete local branches
git branch -d exp5a-fetch-join
git branch -d exp5b-batch-size
git branch -d exp5c-entity-graph
git branch -d feature/cache-optimization
git branch -d awesome-franklin

# Delete remote branches (if pushed)
git push origin --delete exp5a-fetch-join
git push origin --delete exp5b-batch-size
git push origin --delete exp5c-entity-graph
git push origin --delete feature/cache-optimization
git push origin --delete awesome-franklin
```

**Note**: `feature/phase4a-read-write-separation`은 merge 후 자동으로 삭제되지 않으므로:
```bash
git branch -d feature/phase4a-read-write-separation
git push origin --delete feature/phase4a-read-write-separation
```

---

### Step 5: Verify Merge
**검증 사항**:

```bash
# 1. Check current branch
git branch
# Expected: * origin (main)

# 2. Check tags
git tag
# Expected: v1.0-phase6-complete

# 3. Verify file changes
ls docs/
# Expected: API_MAP.md, INFRASTRUCTURE.md, NEXT_PHASE_PLAN.md, PROJECT_STRUCTURE.md

ls scripts/phase6_scalability_opt/
# Expected: results/, tests/

# 4. Check CacheConfig
cat src/main/java/com/pjsent/sentinel/config/CacheConfig.java | grep "maximum-size"
# Expected: .maximumSize(10000)

# 5. Check application-perf.yml
cat src/main/resources/application-perf.yml | grep "maximum-pool-size"
# Expected: maximum-pool-size: 100
```

---

## 🚨 Rollback Plan (If Needed)

### If Merge Fails
```bash
# Abort merge
git merge --abort

# Return to feature branch
git checkout feature/phase4a-read-write-separation
```

### If Merge Succeeds but Issues Found
```bash
# Revert merge commit
git revert -m 1 HEAD

# Or reset to previous commit (⚠️ Destructive)
git reset --hard HEAD~1
```

---

## 📝 Post-Merge Checklist

- [ ] Main branch updated with Phase 4-6 code
- [ ] Tag `v1.0-phase6-complete` created
- [ ] Experimental branches deleted
- [ ] Documentation updated (CLAUDE.md, EXPERIMENT_STATUS.md)
- [ ] Performance reports committed (PORTFOLIO_PERFORMANCE_REPORT.md, SCALABILITY_REPORT.md)
- [ ] Backend runs successfully (`gradlew bootRun`)
- [ ] Tests pass (if any)

---

## 🔜 Next Steps After Merge

1. **Phase 7 Preparation**: Create `feat/phase7-kafka-eda` branch
2. **Kafka Setup**: Docker Compose configuration
3. **Event Schema Design**: Portfolio events (Created, Updated, PriceUpdateRequested)
4. **Consumer Implementation**: Background processing for price updates

---

**Estimated Time**: 30 minutes
**Risk Level**: Low (all changes tested in experimental branches)
**Backup**: Git tags allow easy rollback if needed
