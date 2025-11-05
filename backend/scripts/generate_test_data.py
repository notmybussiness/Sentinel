#!/usr/bin/env python3
"""
Sentinel Performance Test Data Generator
대량의 테스트 유저 및 포트폴리오 데이터를 PostgreSQL에 생성

사용법:
    python generate_test_data.py --users 1000 --mode full

옵션:
    --users: 생성할 유저 수 (기본값: 1000)
    --mode: 생성 모드 (normal, edge, full)
        - normal: 일반 사용자만 (1-5개 포트폴리오, 2-10개 holdings)
        - edge: 엣지 케이스 포함 (30개 포트폴리오, 40개 holdings)
        - full: 둘 다 생성
    --clean: 기존 테스트 데이터 삭제 후 재생성
"""

import psycopg2
import random
import string
from datetime import datetime, timedelta
import argparse
from typing import List, Tuple

# ==================== Configuration ====================

DB_CONFIG = {
    'host': '192.168.0.5',
    'port': 5432,
    'database': 'sentinel',
    'user': 'sentinel',
    'password': 'sentinel_password'
}

# 테스트용 주식 심볼 (JMeter에서도 사용)
STOCK_SYMBOLS = [
    'AAPL', 'GOOGL', 'MSFT', 'TSLA', 'NVDA', 'AMZN', 'META', 'NFLX',
    'AMD', 'INTC', 'COIN', 'SQ', 'PYPL', 'V', 'MA', 'JPM'
]

# 테스트용 암호화폐 심볼
CRYPTO_SYMBOLS = [
    'BTC', 'ETH', 'BNB', 'XRP', 'ADA', 'SOL', 'DOGE', 'DOT',
    'MATIC', 'AVAX', 'LINK', 'UNI', 'ATOM', 'LTC', 'BCH', 'XLM'
]

# ==================== Database Functions ====================

def get_connection():
    """PostgreSQL 연결 생성"""
    return psycopg2.connect(**DB_CONFIG)

def clean_test_data(conn):
    """테스트 데이터 삭제 (dev@sentinel.com 제외)"""
    print("🧹 기존 테스트 데이터 삭제 중...")

    with conn.cursor() as cur:
        # dev@sentinel.com은 유지
        cur.execute("""
            DELETE FROM portfolio_holdings
            WHERE portfolio_id IN (
                SELECT p.id FROM portfolios p
                JOIN users u ON p.user_id = u.id
                WHERE u.email LIKE 'testuser%@test.com'
            )
        """)
        deleted_holdings = cur.rowcount

        cur.execute("""
            DELETE FROM portfolios
            WHERE user_id IN (
                SELECT id FROM users WHERE email LIKE 'testuser%@test.com'
            )
        """)
        deleted_portfolios = cur.rowcount

        cur.execute("DELETE FROM users WHERE email LIKE 'testuser%@test.com'")
        deleted_users = cur.rowcount

        conn.commit()

        print(f"  ✅ 삭제 완료: Users {deleted_users}, Portfolios {deleted_portfolios}, Holdings {deleted_holdings}")

def generate_random_email(index: int) -> str:
    """테스트용 이메일 생성"""
    return f"testuser{index:05d}@test.com"

def insert_users_batch(conn, start_index: int, count: int) -> List[int]:
    """배치로 유저 생성"""
    users = []
    for i in range(start_index, start_index + count):
        email = generate_random_email(i)
        kakao_id = f"test-kakao-{i:05d}"
        name = f"테스트유저{i}"
        users.append((kakao_id, email, name))

    with conn.cursor() as cur:
        cur.executemany("""
            INSERT INTO users (kakao_id, email, name, profile_image_url, created_at, updated_at)
            VALUES (%s, %s, %s, NULL, NOW(), NOW())
        """, users)
        conn.commit()

    # 생성된 user_id 조회
    with conn.cursor() as cur:
        cur.execute("""
            SELECT id FROM users
            WHERE email LIKE 'testuser%@test.com'
            ORDER BY id DESC
            LIMIT %s
        """, (count,))
        user_ids = [row[0] for row in cur.fetchall()]

    return user_ids[::-1]  # 순서 반전 (oldest first)

def insert_portfolios_batch(conn, user_id: int, portfolio_count: int) -> List[int]:
    """유저당 포트폴리오 배치 생성"""
    portfolios = []
    for i in range(portfolio_count):
        name = f"포트폴리오 {i+1}"
        description = f"테스트용 포트폴리오 #{i+1}"
        portfolios.append((user_id, name, description))

    with conn.cursor() as cur:
        cur.executemany("""
            INSERT INTO portfolios (user_id, name, description, created_at, updated_at)
            VALUES (%s, %s, %s, NOW(), NOW())
            RETURNING id
        """, portfolios)
        portfolio_ids = [row[0] for row in cur.fetchall()]
        conn.commit()

    return portfolio_ids

def insert_holdings_batch(conn, portfolio_id: int, holding_count: int):
    """포트폴리오당 holdings 배치 생성"""
    holdings = []

    # 주식과 암호화폐를 섞어서 생성 (7:3 비율)
    stock_count = int(holding_count * 0.7)
    crypto_count = holding_count - stock_count

    # 주식 holdings
    for _ in range(stock_count):
        symbol = random.choice(STOCK_SYMBOLS)
        quantity = round(random.uniform(1, 100), 2)
        avg_price = round(random.uniform(50, 500), 2)
        asset_type = 'STOCK'
        holdings.append((portfolio_id, asset_type, symbol, quantity, avg_price))

    # 암호화폐 holdings
    for _ in range(crypto_count):
        symbol = random.choice(CRYPTO_SYMBOLS)
        quantity = round(random.uniform(0.001, 10), 6)
        avg_price = round(random.uniform(100, 50000), 2)
        asset_type = 'CRYPTO'
        holdings.append((portfolio_id, asset_type, symbol, quantity, avg_price))

    with conn.cursor() as cur:
        cur.executemany("""
            INSERT INTO portfolio_holdings
            (portfolio_id, asset_type, symbol, quantity, average_purchase_price, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
        """, holdings)
        conn.commit()

# ==================== Data Generation Strategies ====================

def generate_normal_users(conn, user_count: int, start_index: int = 1, portfolio_range=(1, 5), holding_range=(2, 10)):
    """일반 사용자 생성 (커스터마이징 가능)"""
    print(f"\n📊 일반 사용자 {user_count}명 생성 중...")
    print(f"  포트폴리오: {portfolio_range[0]}-{portfolio_range[1]}개/user")
    print(f"  Holdings: {holding_range[0]}-{holding_range[1]}개/portfolio")

    batch_size = 100
    total_portfolios = 0
    total_holdings = 0

    for batch_start in range(start_index, start_index + user_count, batch_size):
        batch_count = min(batch_size, start_index + user_count - batch_start)

        # 배치 유저 생성
        user_ids = insert_users_batch(conn, batch_start, batch_count)

        # 각 유저에 포트폴리오 생성
        for user_id in user_ids:
            portfolio_count = random.randint(portfolio_range[0], portfolio_range[1])
            portfolio_ids = insert_portfolios_batch(conn, user_id, portfolio_count)
            total_portfolios += portfolio_count

            # 각 포트폴리오에 holdings 생성
            for portfolio_id in portfolio_ids:
                holding_count = random.randint(holding_range[0], holding_range[1])
                insert_holdings_batch(conn, portfolio_id, holding_count)
                total_holdings += holding_count

        progress = min(batch_start + batch_size - start_index, user_count)
        print(f"  진행률: {progress}/{user_count} ({progress*100//user_count}%)")

    print(f"  ✅ 생성 완료: Users {user_count}, Portfolios {total_portfolios}, Holdings {total_holdings}")
    return user_count

def generate_edge_case_users(conn, start_index: int):
    """엣지 케이스 사용자 생성"""
    print(f"\n🔥 엣지 케이스 사용자 생성 중...")

    # Case 1: 포트폴리오 30개 + 각 포트폴리오 5-10 holdings
    user_ids = insert_users_batch(conn, start_index, 1)
    user_id = user_ids[0]
    portfolio_ids = insert_portfolios_batch(conn, user_id, 30)
    total_holdings = 0
    for portfolio_id in portfolio_ids:
        holding_count = random.randint(5, 10)
        insert_holdings_batch(conn, portfolio_id, holding_count)
        total_holdings += holding_count
    print(f"  ✅ Case 1: 포트폴리오 30개, Holdings {total_holdings}개")

    # Case 2: 포트폴리오 5개 + 각 포트폴리오 40개 holdings
    user_ids = insert_users_batch(conn, start_index + 1, 1)
    user_id = user_ids[0]
    portfolio_ids = insert_portfolios_batch(conn, user_id, 5)
    total_holdings = 0
    for portfolio_id in portfolio_ids:
        insert_holdings_batch(conn, portfolio_id, 40)
        total_holdings += 40
    print(f"  ✅ Case 2: 포트폴리오 5개, Holdings {total_holdings}개")

    # Case 3: 포트폴리오 1개 + holdings 2-10개 범위 (여러 명)
    user_ids = insert_users_batch(conn, start_index + 2, 3)
    for user_id in user_ids:
        portfolio_ids = insert_portfolios_batch(conn, user_id, 1)
        holding_count = random.choice([2, 5, 8, 10])
        insert_holdings_batch(conn, portfolio_ids[0], holding_count)
    print(f"  ✅ Case 3: 최소 holdings 사용자 3명")

    return 5  # 총 5명 생성

# ==================== Main Script ====================

def main():
    parser = argparse.ArgumentParser(description='Sentinel 성능 테스트 데이터 생성')
    parser.add_argument('--users', type=int, default=1000, help='생성할 유저 수 (기본값: 1000)')
    parser.add_argument('--mode', choices=['normal', 'edge', 'full'], default='full',
                        help='생성 모드: normal(일반), edge(엣지), full(둘다)')
    parser.add_argument('--clean', action='store_true', help='기존 테스트 데이터 삭제')

    # 인덱스 실험용 옵션
    parser.add_argument('--portfolios', type=str, default='1-5',
                        help='포트폴리오 개수 범위 (예: 10-100, 기본값: 1-5)')
    parser.add_argument('--holdings', type=str, default='2-10',
                        help='Holdings 개수 범위 (예: 5-50, 기본값: 2-10)')

    args = parser.parse_args()

    # 범위 파싱
    portfolio_range = tuple(map(int, args.portfolios.split('-')))
    holding_range = tuple(map(int, args.holdings.split('-')))

    print("=" * 60)
    print("  Sentinel Performance Test Data Generator")
    print("=" * 60)
    print(f"  Target Users: {args.users}")
    print(f"  Mode: {args.mode}")
    print(f"  Portfolios/User: {portfolio_range[0]}-{portfolio_range[1]}")
    print(f"  Holdings/Portfolio: {holding_range[0]}-{holding_range[1]}")
    print(f"  Clean: {args.clean}")
    print("=" * 60)

    try:
        # DB 연결
        conn = get_connection()
        print("✅ PostgreSQL 연결 성공")

        # 기존 데이터 삭제
        if args.clean:
            clean_test_data(conn)

        start_index = 1

        # 일반 사용자 생성
        if args.mode in ['normal', 'full']:
            generated = generate_normal_users(conn, args.users, start_index, portfolio_range, holding_range)
            start_index += generated

        # 엣지 케이스 생성
        if args.mode in ['edge', 'full']:
            generated = generate_edge_case_users(conn, start_index)
            start_index += generated

        # 최종 통계
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM users WHERE email LIKE 'testuser%@test.com'")
            total_users = cur.fetchone()[0]

            cur.execute("""
                SELECT COUNT(*) FROM portfolios
                WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'testuser%@test.com')
            """)
            total_portfolios = cur.fetchone()[0]

            cur.execute("""
                SELECT COUNT(*) FROM portfolio_holdings
                WHERE portfolio_id IN (
                    SELECT p.id FROM portfolios p
                    JOIN users u ON p.user_id = u.id
                    WHERE u.email LIKE 'testuser%@test.com'
                )
            """)
            total_holdings = cur.fetchone()[0]

        print("\n" + "=" * 60)
        print("  📊 최종 통계")
        print("=" * 60)
        print(f"  총 Users:      {total_users:,}")
        print(f"  총 Portfolios: {total_portfolios:,}")
        print(f"  총 Holdings:   {total_holdings:,}")
        print(f"  평균 Portfolios/User: {total_portfolios/total_users:.1f}")
        print(f"  평균 Holdings/Portfolio: {total_holdings/total_portfolios:.1f}")
        print("=" * 60)
        print("✅ 데이터 생성 완료!")

        conn.close()

    except Exception as e:
        print(f"❌ 에러 발생: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()
