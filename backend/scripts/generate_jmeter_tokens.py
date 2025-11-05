#!/usr/bin/env python3
"""
JMeter 성능 테스트용 토큰 CSV 생성 스크립트

사용법:
    python generate_jmeter_tokens.py

출력:
    jmeter_tokens.csv - JMeter에서 사용할 토큰 목록

요구사항:
    pip install requests
"""

import requests
import csv
import sys
from typing import List, Dict

# 설정
API_BASE_URL = "http://localhost:8080"
OUTPUT_CSV = "jmeter_tokens.csv"


def generate_tokens() -> List[Dict]:
    """
    모든 성능 테스트 유저의 토큰 발급
    """
    print("🔄 토큰 발급 API 호출 중...")

    try:
        response = requests.get(
            f"{API_BASE_URL}/api/v1/auth/perf/tokens",
            timeout=60  # 500개 토큰 생성이므로 넉넉하게
        )

        if response.status_code != 200:
            print(f"❌ API 호출 실패: HTTP {response.status_code}")
            print(f"응답: {response.text}")
            return []

        tokens = response.json()
        print(f"✅ 토큰 발급 완료: {len(tokens)}개")
        return tokens

    except requests.exceptions.ConnectionError:
        print("❌ 서버 연결 실패. Backend가 실행 중인지 확인하세요.")
        print(f"   URL: {API_BASE_URL}")
        return []
    except Exception as e:
        print(f"❌ 오류 발생: {e}")
        return []


def save_to_csv(tokens: List[Dict], filename: str = OUTPUT_CSV):
    """
    토큰 목록을 CSV 파일로 저장

    CSV 형식:
    userId,email,nickname,accessToken,refreshToken
    """
    if not tokens:
        print("❌ 저장할 토큰이 없습니다.")
        return

    print(f"🔄 CSV 파일 생성 중: {filename}")

    try:
        with open(filename, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(
                f,
                fieldnames=['userId', 'email', 'nickname', 'accessToken', 'refreshToken']
            )
            writer.writeheader()
            writer.writerows(tokens)

        print(f"✅ CSV 파일 생성 완료: {filename}")
        print(f"   총 {len(tokens)}개 토큰 저장")

    except Exception as e:
        print(f"❌ CSV 저장 실패: {e}")


def verify_server():
    """
    서버 상태 확인
    """
    print("🔍 서버 상태 확인 중...")

    try:
        response = requests.get(
            f"{API_BASE_URL}/api/v1/auth/perf/stats",
            timeout=5
        )

        if response.status_code == 200:
            stats = response.json()
            print(f"✅ 서버 연결 성공")
            print(f"   총 유저: {stats.get('totalUsers', 0)}/500")
            print(f"   상태: {stats.get('status', 'UNKNOWN')}")
            print(f"   메시지: {stats.get('message', '')}")

            if stats.get('totalUsers', 0) < 500:
                print("\n⚠️  경고: 유저가 500명 미만입니다.")
                print("   SQL 스크립트를 실행해서 유저를 생성하세요:")
                print("   psql -U sentinel -d sentinel -f scripts/perf-test-users.sql")
                return False

            return True
        else:
            print(f"❌ 서버 응답 오류: HTTP {response.status_code}")
            return False

    except requests.exceptions.ConnectionError:
        print("❌ 서버 연결 실패")
        print(f"   URL: {API_BASE_URL}")
        print("\n확인 사항:")
        print("  1. Backend가 실행 중인가요?")
        print("  2. perf 프로파일로 실행 중인가요?")
        print("     ./gradlew bootRun --args='--spring.profiles.active=perf'")
        return False
    except Exception as e:
        print(f"❌ 오류 발생: {e}")
        return False


def main():
    """
    메인 실행 함수
    """
    print("=" * 60)
    print("JMeter 성능 테스트용 토큰 CSV 생성")
    print("=" * 60)
    print()

    # 1. 서버 상태 확인
    if not verify_server():
        print("\n❌ 서버 상태 확인 실패. 스크립트를 종료합니다.")
        sys.exit(1)

    print()

    # 2. 토큰 발급
    tokens = generate_tokens()

    if not tokens:
        print("\n❌ 토큰 발급 실패. 스크립트를 종료합니다.")
        sys.exit(1)

    print()

    # 3. CSV 저장
    save_to_csv(tokens)

    print()
    print("=" * 60)
    print("✅ 완료!")
    print("=" * 60)
    print()
    print("다음 단계:")
    print("  1. JMeter 스크립트 열기")
    print("  2. CSV Data Set Config 추가")
    print("  3. Filename: jmeter_tokens.csv")
    print("  4. Variable Names: userId,email,nickname,accessToken,refreshToken")
    print("  5. HTTP Header Manager에 추가:")
    print("     Authorization: Bearer ${accessToken}")
    print()


if __name__ == "__main__":
    main()
