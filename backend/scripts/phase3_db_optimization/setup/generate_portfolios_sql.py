#!/usr/bin/env python3
"""
Step 2 (SQL 버전): 포트폴리오 데이터 SQL 생성

✅ 기존 데이터가 있어도 안전하게 추가됨 (ID 자동 증가 사용)

사용법:
    python3 generate_portfolios_sql.py --users 500 > portfolios.sql
    psql -h 192.168.0.5 -U sentinel -d sentinel -f portfolios.sql
"""

import argparse
import random
from datetime import datetime

# 한국 주식 (종목코드 6자리)
KOREAN_STOCKS = [
    "005930", "000660", "035420", "035720", "051910", "006400", "068270",
    "105560", "055550", "003550", "207940", "373220", "000270", "028260",
    "012330", "005380", "066570", "096770", "017670", "032830"
]

# 미국 주식
US_STOCKS = [
    "AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA", "AMD",
    "NFLX", "DIS", "INTC", "CSCO", "ORCL", "IBM", "QCOM", "TXN",
    "JPM", "BAC", "WFC", "GS", "MS", "C", "V", "MA",
    "JNJ", "PFE", "UNH", "CVS", "MRK", "ABBV"
]

# 암호화폐
CRYPTO_SYMBOLS = [
    "BTC", "ETH", "BNB", "XRP", "ADA", "SOL", "DOGE", "DOT",
    "MATIC", "AVAX", "LINK", "UNI", "ATOM", "LTC", "BCH", "XLM"
]

# 포트폴리오 이름 템플릿
PORTFOLIO_NAMES = [
    "장기 투자 포트폴리오",
    "배당주 포트폴리오",
    "성장주 포트폴리오",
    "암호화폐 포트폴리오",
    "균형 포트폴리오",
    "공격적 포트폴리오",
    "안정적 포트폴리오",
    "기술주 포트폴리오",
    "가치 투자 포트폴리오",
    "인덱스 추종 포트폴리오"
]


def generate_random_holding(is_crypto=False):
    """랜덤 종목 데이터 생성"""
    if is_crypto:
        symbol = random.choice(CRYPTO_SYMBOLS)
        asset_type = "CRYPTO"
        base_currency = random.choice(["USD", "KRW"])
        quantity = round(random.uniform(0.01, 10.0), 6)
        average_cost = round(random.uniform(100, 50000), 2)
    else:
        # 한국 주식 vs 미국 주식 (50:50)
        if random.random() < 0.5:
            symbol = random.choice(KOREAN_STOCKS)
            asset_type = "STOCK"
            base_currency = "KRW"
            quantity = round(random.uniform(1, 100), 2)
            average_cost = round(random.uniform(10000, 500000), 0)
        else:
            symbol = random.choice(US_STOCKS)
            asset_type = "STOCK"
            base_currency = "USD"
            quantity = round(random.uniform(1, 100), 2)
            average_cost = round(random.uniform(10, 500), 2)

    total_cost = round(quantity * average_cost, 2)

    return {
        'symbol': symbol,
        'quantity': quantity,
        'average_cost': average_cost,
        'total_cost': total_cost,
        'asset_type': asset_type,
        'base_currency': base_currency
    }


def generate_sql(total_users=500, min_portfolios=1, max_portfolios=20,
                 min_holdings=3, max_holdings=10):
    """포트폴리오 및 Holdings SQL 생성"""

    print("-- ================================================")
    print("-- Performance Test Portfolio Data")
    print(f"-- Generated: {datetime.now().isoformat()}")
    print(f"-- Users: {total_users}")
    print(f"-- Portfolios per user: {min_portfolios}-{max_portfolios}")
    print(f"-- Holdings per portfolio: {min_holdings}-{max_holdings}")
    print("-- ✅ Safe to run even if portfolios already exist")
    print("-- ================================================")
    print()

    # 트랜잭션 시작
    print("BEGIN;")
    print()

    # Outlier 유저 수 (1%)
    outlier_count = max(1, int(total_users * 0.01))

    total_portfolios = 0
    total_holdings = 0

    for user_idx in range(total_users):
        user_id = user_idx + 1  # perftest001@sentinel.com = user_id 1

        # Outlier 판정
        is_outlier = user_idx < outlier_count

        if is_outlier:
            # Type A (짝수) vs Type B (홀수)
            if user_idx % 2 == 0:
                # Type A: 포트폴리오 1개, 종목 0-1개
                portfolio_count = 1
                holdings_range = (0, 1)
            else:
                # Type B: 포트폴리오 50개, 종목 25-35개
                portfolio_count = 50
                holdings_range = (25, 35)
        else:
            # 일반 유저
            portfolio_count = random.randint(min_portfolios, max_portfolios)
            holdings_range = (min_holdings, max_holdings)

        # 포트폴리오 생성
        for p_idx in range(portfolio_count):
            name = random.choice(PORTFOLIO_NAMES)
            if p_idx > 0:
                name = f"{name} {p_idx + 1}"

            # SQL-safe 문자열 처리
            name = name.replace("'", "''")
            description = f"테스트용 포트폴리오 (User {user_id})".replace("'", "''")

            # Portfolio INSERT (ID 자동 증가, RETURNING으로 받기)
            print(f"-- Portfolio for User {user_id} ({p_idx + 1}/{portfolio_count})")
            print(f"DO $$")
            print(f"DECLARE")
            print(f"  v_portfolio_id BIGINT;")
            print(f"BEGIN")
            print(f"  -- Create portfolio")
            print(f"  INSERT INTO portfolios (user_id, name, description, total_value, total_cost, "
                  f"total_gain_loss, total_gain_loss_percent, created_at, updated_at)")
            print(f"  VALUES ({user_id}, '{name}', '{description}', 0, 0, 0, 0, NOW(), NOW())")
            print(f"  RETURNING id INTO v_portfolio_id;")
            print()

            total_portfolios += 1

            # Holdings 생성
            holding_count = random.randint(holdings_range[0], holdings_range[1])
            is_crypto_portfolio = "암호화폐" in name

            holdings = []
            symbols_used = set()  # UNIQUE 제약 (portfolio_id, symbol) 처리

            for h_idx in range(holding_count):
                # UNIQUE 제약 위반 방지
                max_retry = 10
                for retry in range(max_retry):
                    holding = generate_random_holding(is_crypto_portfolio)
                    if holding['symbol'] not in symbols_used:
                        symbols_used.add(holding['symbol'])
                        holdings.append(holding)
                        break

            # Holdings 배치 INSERT
            if holdings:
                print(f"  -- Add {len(holdings)} holdings")
                print(f"  INSERT INTO portfolio_holdings (portfolio_id, symbol, quantity, average_cost, "
                      f"current_price, market_value, total_cost, gain_loss, gain_loss_percent, "
                      f"asset_type, base_currency, created_at, updated_at)")
                print(f"  VALUES")

                for idx, h in enumerate(holdings):
                    total_holdings += 1

                    # 마지막 row는 세미콜론
                    separator = ";" if idx == len(holdings) - 1 else ","

                    print(f"    (v_portfolio_id, '{h['symbol']}', {h['quantity']}, {h['average_cost']}, "
                          f"0, 0, {h['total_cost']}, 0, 0, "
                          f"'{h['asset_type']}', '{h['base_currency']}', NOW(), NOW()){separator}")

            print(f"END $$;")
            print()

        # 진행 상황 출력 (50명마다)
        if (user_idx + 1) % 50 == 0:
            print(f"-- Progress: {user_idx + 1}/{total_users} users | "
                  f"Portfolios: {total_portfolios} | Holdings: {total_holdings}")
            print()

    # 커밋
    print("COMMIT;")
    print()

    # 통계
    print("-- ================================================")
    print("-- Summary")
    print("-- ================================================")
    print(f"-- Total Users: {total_users}")
    print(f"-- Total Portfolios: {total_portfolios}")
    print(f"-- Total Holdings: {total_holdings}")
    print(f"-- Avg Portfolios/User: {total_portfolios / total_users:.1f}")
    print(f"-- Avg Holdings/Portfolio: {total_holdings / total_portfolios:.1f}" if total_portfolios > 0 else "")
    print("-- ================================================")


def main():
    parser = argparse.ArgumentParser(
        description='성능 테스트용 포트폴리오 데이터 SQL 생성'
    )
    parser.add_argument('--users', type=int, default=500, help='총 유저 수 (기본: 500)')
    parser.add_argument('--min-portfolios', type=int, default=1, help='유저당 최소 포트폴리오 (기본: 1)')
    parser.add_argument('--max-portfolios', type=int, default=20, help='유저당 최대 포트폴리오 (기본: 20)')
    parser.add_argument('--min-holdings', type=int, default=3, help='포트폴리오당 최소 종목 (기본: 3)')
    parser.add_argument('--max-holdings', type=int, default=10, help='포트폴리오당 최대 종목 (기본: 10)')

    args = parser.parse_args()

    generate_sql(
        total_users=args.users,
        min_portfolios=args.min_portfolios,
        max_portfolios=args.max_portfolios,
        min_holdings=args.min_holdings,
        max_holdings=args.max_holdings
    )


if __name__ == "__main__":
    main()
