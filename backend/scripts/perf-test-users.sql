-- ============================================
-- Performance Test Users Generation
-- 500명의 테스트 유저 생성
-- ============================================

-- 기존 테스트 유저 삭제 (선택사항)
DELETE FROM users WHERE email LIKE 'perftest%@sentinel.com';

-- 500명의 테스트 유저 생성
-- BCrypt 해시: "password123" (실제로는 사용 안 함, JWT 토큰만 사용)
DO $$
DECLARE
    i INTEGER;
    user_email VARCHAR(100);
    user_nickname VARCHAR(50);
BEGIN
    FOR i IN 1..500 LOOP
        user_email := 'perftest' || LPAD(i::TEXT, 3, '0') || '@sentinel.com';
        user_nickname := 'PerfUser_' || LPAD(i::TEXT, 3, '0');

        -- users 테이블에 삽입
        INSERT INTO users (
            email,
            nickname,
            profile_image_url,
            oauth_provider,
            oauth_id,
            created_at,
            updated_at
        ) VALUES (
            user_email,
            user_nickname,
            NULL,
            'PERF_TEST',  -- 성능 테스트 전용 Provider
            'perf_' || i,
            NOW(),
            NOW()
        )
        ON CONFLICT (email) DO NOTHING;  -- 중복 시 무시
    END LOOP;

    -- 생성 결과 확인
    RAISE NOTICE '✅ 성능 테스트 유저 생성 완료: % 명', (SELECT COUNT(*) FROM users WHERE email LIKE 'perftest%@sentinel.com');
END $$;

-- 확인 쿼리
SELECT
    COUNT(*) as total_users,
    MIN(id) as first_user_id,
    MAX(id) as last_user_id
FROM users
WHERE email LIKE 'perftest%@sentinel.com';

-- 샘플 유저 확인
SELECT id, email, nickname, oauth_provider, created_at
FROM users
WHERE email LIKE 'perftest%@sentinel.com'
ORDER BY id
LIMIT 10;
