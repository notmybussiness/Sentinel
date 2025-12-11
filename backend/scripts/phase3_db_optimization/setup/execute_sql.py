#!/usr/bin/env python3
"""
SQL 파일을 PostgreSQL에 직접 실행

사용법:
    python execute_sql.py portfolios.sql
"""

import sys
import psycopg2

DB_HOST = "192.168.0.5"
DB_PORT = 5432
DB_NAME = "sentinel"
DB_USER = "sentinel"
DB_PASSWORD = "sentinel_password"


def execute_sql_file(sql_file):
    print(f"🔄 연결 중: {DB_HOST}:{DB_PORT}/{DB_NAME}")

    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD
        )

        print(f"✅ 연결 성공!")
        print(f"🔄 SQL 파일 읽기 중: {sql_file}")

        with open(sql_file, 'r', encoding='utf-8') as f:
            sql = f.read()

        print(f"📊 SQL 크기: {len(sql):,} bytes")
        print(f"🔄 실행 중... (10-30초 소요)")

        cursor = conn.cursor()
        cursor.execute(sql)
        conn.commit()

        print(f"✅ 완료!")

        cursor.close()
        conn.close()

    except psycopg2.Error as e:
        print(f"❌ DB 에러: {e}")
        sys.exit(1)
    except FileNotFoundError:
        print(f"❌ 파일을 찾을 수 없습니다: {sql_file}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ 에러: {e}")
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("사용법: python execute_sql.py <sql_file>")
        sys.exit(1)

    execute_sql_file(sys.argv[1])
