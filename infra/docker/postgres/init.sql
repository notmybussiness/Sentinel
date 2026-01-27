-- =====================================================
-- Sentinel Database Initialization Script
-- =====================================================
-- Creates initial database structure, users, and extensions
-- =====================================================

-- Enable useful extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create application user with limited privileges (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sentinel_app') THEN
        CREATE ROLE sentinel_app WITH LOGIN PASSWORD 'app_password';
    END IF;
END
$$;

-- Grant privileges
GRANT CONNECT ON DATABASE sentinel TO sentinel_app;
GRANT USAGE ON SCHEMA public TO sentinel_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO sentinel_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sentinel_app;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sentinel_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO sentinel_app;

-- Create read-only user for analytics (optional)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sentinel_readonly') THEN
        CREATE ROLE sentinel_readonly WITH LOGIN PASSWORD 'readonly_password';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE sentinel TO sentinel_readonly;
GRANT USAGE ON SCHEMA public TO sentinel_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sentinel_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO sentinel_readonly;

-- =====================================================
-- Performance Indexes (application-specific)
-- =====================================================

-- Note: These indexes should be created after the application
-- has created its tables via JPA. Run manually or via migration.

-- Example indexes (uncomment when tables exist):
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users(email);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_portfolio_id ON transactions(portfolio_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);

-- =====================================================
-- Maintenance Functions
-- =====================================================

-- Function to get table sizes
CREATE OR REPLACE FUNCTION get_table_sizes()
RETURNS TABLE (
    table_name text,
    row_count bigint,
    total_size text,
    index_size text,
    toast_size text
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.tablename::text,
        c.reltuples::bigint AS row_estimate,
        pg_size_pretty(pg_total_relation_size(quote_ident(t.tablename)::regclass)) AS total_size,
        pg_size_pretty(pg_indexes_size(quote_ident(t.tablename)::regclass)) AS index_size,
        pg_size_pretty(pg_total_relation_size(quote_ident(t.tablename)::regclass) - pg_relation_size(quote_ident(t.tablename)::regclass)) AS toast_size
    FROM pg_tables t
    JOIN pg_class c ON c.relname = t.tablename
    WHERE t.schemaname = 'public'
    ORDER BY pg_total_relation_size(quote_ident(t.tablename)::regclass) DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to check connection count
CREATE OR REPLACE FUNCTION get_connection_stats()
RETURNS TABLE (
    state text,
    count bigint,
    max_conn int
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COALESCE(s.state, 'total')::text,
        COUNT(*)::bigint,
        (SELECT setting::int FROM pg_settings WHERE name = 'max_connections')
    FROM pg_stat_activity s
    WHERE s.datname = current_database()
    GROUP BY ROLLUP(s.state);
END;
$$ LANGUAGE plpgsql;

-- Log initialization complete
DO $$
BEGIN
    RAISE NOTICE 'Sentinel database initialization completed successfully!';
END
$$;
