-- ==============================================================================
-- EXP-01: DB Indexing for Performance Optimization
-- ==============================================================================
-- Target: 217 TPS → 300-400 TPS
-- Tested Tables: portfolios, portfolio_holdings, user_sessions
-- ==============================================================================

-- Pre-check: Current index status
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename IN ('portfolios', 'portfolio_holdings', 'user_sessions')
ORDER BY tablename, indexname;

-- ==============================================================================
-- 1. Portfolio Indexes
-- ==============================================================================
-- Query: SELECT * FROM portfolios WHERE user_id = ?
-- Impact: Portfolio list API (60% of traffic)

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_portfolios_user_id
ON portfolios(user_id);

-- Composite index for sorted results
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_portfolios_user_id_created_at
ON portfolios(user_id, created_at DESC);

-- ==============================================================================
-- 2. Portfolio Holdings Indexes
-- ==============================================================================
-- Query: SELECT * FROM portfolio_holdings WHERE portfolio_id = ?
-- Impact: Portfolio detail API (20% of traffic)

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_portfolio_holdings_portfolio_id
ON portfolio_holdings(portfolio_id);

-- ==============================================================================
-- 3. User Session Indexes (with partial index for active sessions only)
-- ==============================================================================
-- Query: SELECT * FROM user_sessions WHERE access_token_hash = ? AND is_active = true
-- Impact: Every authenticated request (100% of traffic)

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_user_id
ON user_sessions(user_id);

-- Partial index: Only active sessions (reduces index size significantly)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_access_token_hash
ON user_sessions(access_token_hash)
WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_refresh_token_hash
ON user_sessions(refresh_token_hash)
WHERE is_active = true;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_expires_at
ON user_sessions(expires_at)
WHERE is_active = true;

-- ==============================================================================
-- Post-check: Verify indexes created
-- ==============================================================================
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_indexes
JOIN pg_class ON pg_class.relname = pg_indexes.indexname
WHERE tablename IN ('portfolios', 'portfolio_holdings', 'user_sessions')
ORDER BY tablename, indexname;

-- ==============================================================================
-- ANALYZE tables to update statistics
-- ==============================================================================
ANALYZE portfolios;
ANALYZE portfolio_holdings;
ANALYZE user_sessions;

-- ==============================================================================
-- Verification: Check query plan improvement
-- ==============================================================================
-- Run these EXPLAIN ANALYZE queries to verify index usage:

-- Test 1: Portfolio by user_id
-- EXPLAIN ANALYZE SELECT * FROM portfolios WHERE user_id = 1;

-- Test 2: Holdings by portfolio_id
-- EXPLAIN ANALYZE SELECT * FROM portfolio_holdings WHERE portfolio_id = 1;

-- Test 3: Session by access_token_hash
-- EXPLAIN ANALYZE SELECT * FROM user_sessions WHERE access_token_hash = 'test' AND is_active = true;
