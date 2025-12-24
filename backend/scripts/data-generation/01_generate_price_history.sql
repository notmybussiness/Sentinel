-- ============================================================
-- Phase 1: 대량 더미 데이터 생성 (100만 건 price_history)
-- ============================================================
-- 목적: Index 성능 테스트, 페이징 비교를 위한 대량 데이터 생성
-- 대상 테이블: price_history
-- 목표 건수: 1,000,000건 (100개 심볼 x 10,000일치 데이터)
-- ============================================================

-- 0. 현재 데이터 수 확인
SELECT 'Before' as status, COUNT(*) as total_count FROM price_history;

-- 1. 기존 테스트 데이터 삭제 (선택사항 - 주석 해제하여 사용)
-- TRUNCATE TABLE price_history RESTART IDENTITY;

-- 2. 심볼 목록 생성을 위한 임시 테이블
DROP TABLE IF EXISTS temp_symbols;
CREATE TEMP TABLE temp_symbols (
    symbol VARCHAR(20),
    asset_type VARCHAR(10),
    base_currency VARCHAR(10),
    base_price NUMERIC(20, 8)
);

-- 3. 100개 심볼 삽입 (주식 50개 + 암호화폐 30개 + ETF 15개 + INDEX 5개)
INSERT INTO temp_symbols (symbol, asset_type, base_currency, base_price) VALUES
-- 미국 주식 (30개)
('AAPL', 'STOCK', 'USD', 175.50),
('GOOGL', 'STOCK', 'USD', 140.25),
('MSFT', 'STOCK', 'USD', 378.90),
('AMZN', 'STOCK', 'USD', 178.35),
('NVDA', 'STOCK', 'USD', 495.22),
('META', 'STOCK', 'USD', 505.75),
('TSLA', 'STOCK', 'USD', 248.50),
('BRK.B', 'STOCK', 'USD', 362.15),
('JPM', 'STOCK', 'USD', 195.80),
('V', 'STOCK', 'USD', 275.45),
('JNJ', 'STOCK', 'USD', 156.30),
('WMT', 'STOCK', 'USD', 165.20),
('PG', 'STOCK', 'USD', 158.75),
('MA', 'STOCK', 'USD', 458.90),
('HD', 'STOCK', 'USD', 345.60),
('CVX', 'STOCK', 'USD', 148.25),
('MRK', 'STOCK', 'USD', 118.40),
('ABBV', 'STOCK', 'USD', 175.85),
('PFE', 'STOCK', 'USD', 28.50),
('KO', 'STOCK', 'USD', 59.75),
('PEP', 'STOCK', 'USD', 168.90),
('COST', 'STOCK', 'USD', 725.30),
('TMO', 'STOCK', 'USD', 528.45),
('AVGO', 'STOCK', 'USD', 1125.60),
('MCD', 'STOCK', 'USD', 295.80),
('CSCO', 'STOCK', 'USD', 50.25),
('ACN', 'STOCK', 'USD', 358.70),
('ABT', 'STOCK', 'USD', 108.35),
('DHR', 'STOCK', 'USD', 252.90),
('LIN', 'STOCK', 'USD', 425.15),
-- 한국 주식 (20개)
('005930', 'STOCK', 'KRW', 71500.00),
('000660', 'STOCK', 'KRW', 132000.00),
('035420', 'STOCK', 'KRW', 195000.00),
('005380', 'STOCK', 'KRW', 185000.00),
('051910', 'STOCK', 'KRW', 425000.00),
('006400', 'STOCK', 'KRW', 78500.00),
('035720', 'STOCK', 'KRW', 52300.00),
('068270', 'STOCK', 'KRW', 168000.00),
('028260', 'STOCK', 'KRW', 315000.00),
('105560', 'STOCK', 'KRW', 62500.00),
('055550', 'STOCK', 'KRW', 43500.00),
('096770', 'STOCK', 'KRW', 115000.00),
('003550', 'STOCK', 'KRW', 78000.00),
('017670', 'STOCK', 'KRW', 425000.00),
('066570', 'STOCK', 'KRW', 92500.00),
('034730', 'STOCK', 'KRW', 48500.00),
('032830', 'STOCK', 'KRW', 165000.00),
('003670', 'STOCK', 'KRW', 58500.00),
('018260', 'STOCK', 'KRW', 95000.00),
('090430', 'STOCK', 'KRW', 35500.00),
-- 암호화폐 (30개)
('BTC', 'CRYPTO', 'KRW', 58500000.00),
('ETH', 'CRYPTO', 'KRW', 3250000.00),
('XRP', 'CRYPTO', 'KRW', 850.00),
('SOL', 'CRYPTO', 'KRW', 125000.00),
('ADA', 'CRYPTO', 'KRW', 650.00),
('DOGE', 'CRYPTO', 'KRW', 125.00),
('DOT', 'CRYPTO', 'KRW', 8500.00),
('MATIC', 'CRYPTO', 'KRW', 1150.00),
('SHIB', 'CRYPTO', 'KRW', 0.012),
('AVAX', 'CRYPTO', 'KRW', 42500.00),
('LINK', 'CRYPTO', 'KRW', 18500.00),
('ATOM', 'CRYPTO', 'KRW', 12500.00),
('UNI', 'CRYPTO', 'KRW', 8200.00),
('LTC', 'CRYPTO', 'KRW', 95000.00),
('ETC', 'CRYPTO', 'KRW', 25500.00),
('BCH', 'CRYPTO', 'KRW', 325000.00),
('XLM', 'CRYPTO', 'KRW', 165.00),
('ALGO', 'CRYPTO', 'KRW', 225.00),
('VET', 'CRYPTO', 'KRW', 35.00),
('FIL', 'CRYPTO', 'KRW', 6500.00),
('THETA', 'CRYPTO', 'KRW', 1250.00),
('ICP', 'CRYPTO', 'KRW', 15500.00),
('SAND', 'CRYPTO', 'KRW', 550.00),
('MANA', 'CRYPTO', 'KRW', 520.00),
('AXS', 'CRYPTO', 'KRW', 9500.00),
('AAVE', 'CRYPTO', 'KRW', 125000.00),
('EOS', 'CRYPTO', 'KRW', 950.00),
('XTZ', 'CRYPTO', 'KRW', 1150.00),
('NEAR', 'CRYPTO', 'KRW', 3250.00),
('FLOW', 'CRYPTO', 'KRW', 950.00),
-- ETF (15개)
('SPY', 'ETF', 'USD', 475.50),
('QQQ', 'ETF', 'USD', 405.25),
('IWM', 'ETF', 'USD', 198.75),
('VTI', 'ETF', 'USD', 242.30),
('VOO', 'ETF', 'USD', 435.80),
('VEA', 'ETF', 'USD', 47.25),
('VWO', 'ETF', 'USD', 41.50),
('BND', 'ETF', 'USD', 72.35),
('GLD', 'ETF', 'USD', 185.60),
('SLV', 'ETF', 'USD', 22.45),
('XLF', 'ETF', 'USD', 38.75),
('XLE', 'ETF', 'USD', 85.30),
('XLK', 'ETF', 'USD', 185.45),
('ARKK', 'ETF', 'USD', 48.25),
('DIA', 'ETF', 'USD', 378.90),
-- INDEX (5개)
('SPX', 'INDEX', 'USD', 4785.50),
('NDX', 'INDEX', 'USD', 16825.30),
('DJI', 'INDEX', 'USD', 37545.25),
('KOSPI', 'INDEX', 'KRW', 2525.50),
('KOSDAQ', 'INDEX', 'KRW', 825.75);

-- 4. 대량 데이터 삽입 (generate_series 사용)
-- 각 심볼별로 10,000일치 데이터 생성 = 100 심볼 x 10,000일 = 1,000,000건
INSERT INTO price_history (symbol, asset_type, base_currency, timestamp, open, high, low, close, volume, created_at)
SELECT
    s.symbol,
    s.asset_type::VARCHAR(10),
    s.base_currency,
    -- 2020-01-01부터 시작하여 매일 데이터 생성
    DATE '2020-01-01' + (day_offset || ' days')::INTERVAL +
    (FLOOR(RANDOM() * 24) || ' hours')::INTERVAL +
    (FLOOR(RANDOM() * 60) || ' minutes')::INTERVAL,
    -- OHLCV 데이터 생성 (랜덤 변동)
    s.base_price * (1 + (RANDOM() - 0.5) * 0.1),  -- Open: ±5% 변동
    s.base_price * (1 + RANDOM() * 0.08),          -- High: +0~8%
    s.base_price * (1 - RANDOM() * 0.08),          -- Low: -0~8%
    s.base_price * (1 + (RANDOM() - 0.5) * 0.06),  -- Close: ±3% 변동
    FLOOR(RANDOM() * 10000000 + 100000),           -- Volume: 10만~1000만
    NOW()
FROM
    temp_symbols s
CROSS JOIN
    generate_series(0, 9999) AS day_offset;  -- 10,000일

-- 5. 결과 확인
SELECT 'After' as status, COUNT(*) as total_count FROM price_history;

-- 6. 심볼별 데이터 분포 확인
SELECT
    asset_type,
    COUNT(DISTINCT symbol) as symbol_count,
    COUNT(*) as total_rows,
    MIN(timestamp) as min_date,
    MAX(timestamp) as max_date
FROM price_history
GROUP BY asset_type
ORDER BY asset_type;

-- 7. 테이블 크기 확인
SELECT
    pg_size_pretty(pg_relation_size('price_history')) as table_size,
    pg_size_pretty(pg_indexes_size('price_history')) as indexes_size,
    pg_size_pretty(pg_total_relation_size('price_history')) as total_size;

-- 8. 현재 인덱스 확인
SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'price_history';

-- 임시 테이블 정리
DROP TABLE IF EXISTS temp_symbols;
