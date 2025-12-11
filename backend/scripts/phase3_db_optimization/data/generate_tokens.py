#!/usr/bin/env python3
"""
JMeter 성능 테스트용 JWT 토큰 CSV 생성
실행: python step1_generate_tokens.py
출력: jmeter_tokens.csv
"""

import requests
import csv
import sys

API_BASE_URL = "http://192.168.0.58:8080"
OUTPUT_CSV = "jmeter_tokens.csv"


def verify_server():
    print("🔍 서버 상태 확인 중...")
    try:
        response = requests.get(f"{API_BASE_URL}/api/v1/auth/perf/stats", timeout=5)
        if response.status_code == 200:
            stats = response.json()
            print(f"✅ 서버 연결 성공")
            print(f"   총 유저: {stats.get('totalUsers', 0)}/500")

            if stats.get('totalUsers', 0) < 500:
                print("\n⚠️  유저가 500명 미만입니다.")
                print("   실행: psql -U sentinel -d sentinel -f step0_create_users.sql")
                return False
            return True
        else:
            print(f"❌ 서버 응답 오류: HTTP {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ 서버 연결 실패")
        print(f"   URL: {API_BASE_URL}")
        print("   Backend가 perf 프로파일로 실행 중인지 확인하세요.")
        return False
    except Exception as e:
        print(f"❌ 오류: {e}")
        return False


def generate_tokens():
    print("🔄 토큰 발급 중...")
    try:
        response = requests.get(f"{API_BASE_URL}/api/v1/auth/perf/tokens", timeout=60)

        if response.status_code != 200:
            print(f"❌ API 호출 실패: HTTP {response.status_code}")
            return []

        tokens = response.json()
        print(f"✅ 토큰 발급 완료: {len(tokens)}개")
        return tokens
    except Exception as e:
        print(f"❌ 오류: {e}")
        return []


def save_to_csv(tokens):
    if not tokens:
        print("❌ 저장할 토큰이 없습니다.")
        return

    print(f"🔄 CSV 파일 생성 중: {OUTPUT_CSV}")

    try:
        with open(OUTPUT_CSV, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=['userId', 'email', 'nickname', 'accessToken', 'refreshToken'])
            writer.writeheader()
            writer.writerows(tokens)

        print(f"✅ CSV 생성 완료: {OUTPUT_CSV} ({len(tokens)}개)")
    except Exception as e:
        print(f"❌ CSV 저장 실패: {e}")


def main():
    print("=" * 60)
    print("JMeter 토큰 CSV 생성")
    print("=" * 60)
    print()

    if not verify_server():
        print("\n❌ 서버 확인 실패")
        sys.exit(1)

    print()
    tokens = generate_tokens()

    if not tokens:
        print("\n❌ 토큰 발급 실패")
        sys.exit(1)

    print()
    save_to_csv(tokens)

    print()
    print("=" * 60)
    print("✅ 완료!")
    print("=" * 60)


if __name__ == "__main__":
    main()
