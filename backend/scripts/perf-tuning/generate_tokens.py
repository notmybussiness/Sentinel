#!/usr/bin/env python3
"""
EXP-01: k6 성능 테스트용 JWT 토큰 CSV 생성

실행: python generate_tokens.py
출력: tokens.csv
"""

import requests
import csv
import sys
import argparse

DEFAULT_API_URL = "http://localhost:8080"
OUTPUT_CSV = "tokens.csv"


def verify_server(api_url):
    print("Checking server status...")
    try:
        response = requests.get(f"{api_url}/api/v1/auth/perf/stats", timeout=5)
        if response.status_code == 200:
            stats = response.json()
            print(f"  Server connected")
            print(f"  Total users: {stats.get('totalUsers', 0)}")
            return True
        else:
            print(f"  Server error: HTTP {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print(f"  Connection failed: {api_url}")
        return False
    except Exception as e:
        print(f"  Error: {e}")
        return False


def generate_tokens(api_url):
    print("Generating tokens...")
    try:
        response = requests.get(f"{api_url}/api/v1/auth/perf/tokens", timeout=60)
        if response.status_code != 200:
            print(f"  API error: HTTP {response.status_code}")
            return []
        tokens = response.json()
        print(f"  Generated: {len(tokens)} tokens")
        return tokens
    except Exception as e:
        print(f"  Error: {e}")
        return []


def save_to_csv(tokens, output_file):
    if not tokens:
        print("  No tokens to save")
        return False

    print(f"Saving to {output_file}...")
    try:
        with open(output_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=['userId', 'email', 'nickname', 'accessToken', 'refreshToken'])
            writer.writeheader()
            writer.writerows(tokens)
        print(f"  Saved {len(tokens)} tokens")
        return True
    except Exception as e:
        print(f"  Error: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description='Generate JWT tokens for k6 tests')
    parser.add_argument('--url', default=DEFAULT_API_URL, help='API base URL')
    parser.add_argument('--output', default=OUTPUT_CSV, help='Output CSV file')
    args = parser.parse_args()

    print("=" * 50)
    print("  EXP-01: Token Generator")
    print("=" * 50)

    if not verify_server(args.url):
        sys.exit(1)

    tokens = generate_tokens(args.url)
    if not tokens:
        sys.exit(1)

    if not save_to_csv(tokens, args.output):
        sys.exit(1)

    print("=" * 50)
    print("  Done!")
    print("=" * 50)


if __name__ == "__main__":
    main()
