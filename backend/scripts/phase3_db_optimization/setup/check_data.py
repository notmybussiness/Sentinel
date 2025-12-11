#!/usr/bin/env python3
"""
데이터베이스 데이터 확인
"""

import psycopg2

DB_HOST = "192.168.0.5"
DB_PORT = 5432
DB_NAME = "sentinel"
DB_USER = "sentinel"
DB_PASSWORD = "sentinel_password"


def check_data():
    conn = psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD
    )

    cursor = conn.cursor()

    print("=" * 60)
    print("Database Statistics")
    print("=" * 60)

    # Portfolios 개수
    cursor.execute("SELECT COUNT(*) FROM portfolios;")
    portfolio_count = cursor.fetchone()[0]
    print(f"Total Portfolios: {portfolio_count:,}")

    # Holdings 개수
    cursor.execute("SELECT COUNT(*) FROM portfolio_holdings;")
    holding_count = cursor.fetchone()[0]
    print(f"Total Holdings: {holding_count:,}")

    # 평균
    if portfolio_count > 0:
        print(f"Avg Holdings/Portfolio: {holding_count / portfolio_count:.1f}")

    print()
    print("Top 10 Users by Portfolio Count:")
    print("-" * 60)

    cursor.execute("""
        SELECT user_id, COUNT(*) as cnt
        FROM portfolios
        GROUP BY user_id
        ORDER BY cnt DESC
        LIMIT 10;
    """)

    for row in cursor.fetchall():
        print(f"  User {row[0]:3d}: {row[1]:3d} portfolios")

    print()
    print("Top 10 Portfolios by Holding Count:")
    print("-" * 60)

    cursor.execute("""
        SELECT p.id, p.name, COUNT(h.id) as cnt
        FROM portfolios p
        LEFT JOIN portfolio_holdings h ON p.id = h.portfolio_id
        GROUP BY p.id, p.name
        ORDER BY cnt DESC
        LIMIT 10;
    """)

    for row in cursor.fetchall():
        print(f"  Portfolio {row[0]:4d} ({row[1][:30]:30s}): {row[2]:2d} holdings")

    print("=" * 60)

    cursor.close()
    conn.close()


if __name__ == "__main__":
    check_data()
