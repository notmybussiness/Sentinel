-- ==============================================================================
-- EXP-01: Rollback - Remove indexes if needed
-- ==============================================================================
-- Use this script if indexes cause issues or for A/B testing
-- ==============================================================================

-- Portfolio indexes
DROP INDEX CONCURRENTLY IF EXISTS idx_portfolios_user_id;
DROP INDEX CONCURRENTLY IF EXISTS idx_portfolios_user_id_created_at;

-- Portfolio Holdings index
DROP INDEX CONCURRENTLY IF EXISTS idx_portfolio_holdings_portfolio_id;

-- User Session indexes
DROP INDEX CONCURRENTLY IF EXISTS idx_user_sessions_user_id;
DROP INDEX CONCURRENTLY IF EXISTS idx_user_sessions_access_token_hash;
DROP INDEX CONCURRENTLY IF EXISTS idx_user_sessions_refresh_token_hash;
DROP INDEX CONCURRENTLY IF EXISTS idx_user_sessions_expires_at;

-- Verify removal
SELECT
    tablename,
    indexname
FROM pg_indexes
WHERE tablename IN ('portfolios', 'portfolio_holdings', 'user_sessions')
  AND indexname LIKE 'idx_%'
ORDER BY tablename;
