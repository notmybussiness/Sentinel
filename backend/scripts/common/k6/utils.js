/**
 * k6 Common Utilities
 *
 * 공통으로 사용되는 유틸리티 함수 모음
 */

/**
 * 토큰 파일 로드
 *
 * @param {string} filePath - 토큰 CSV 파일 경로
 * @returns {Array<string>} JWT 토큰 배열
 */
export function loadTokens(filePath) {
  try {
    const data = open(filePath);
    const lines = data.trim().split('\n');

    // CSV 헤더 제거 (첫 줄이 "user_id,token"인 경우)
    const hasHeader = lines[0].includes('user_id') || lines[0].includes('token');
    const tokens = hasHeader ? lines.slice(1) : lines;

    // token 컬럼 추출
    return tokens.map(line => {
      const parts = line.split(',');
      return parts[1] || parts[0]; // 두 번째 컬럼 또는 전체
    });
  } catch (e) {
    console.error(`❌ Failed to load tokens from ${filePath}: ${e}`);
    return [];
  }
}

/**
 * 랜덤 배열 요소 선택
 *
 * @param {Array} arr - 배열
 * @returns {*} 랜덤 요소
 */
export function randomChoice(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/**
 * 랜덤 정수 생성
 *
 * @param {number} min - 최소값
 * @param {number} max - 최대값
 * @returns {number} 랜덤 정수
 */
export function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * 랜덤 실수 생성
 *
 * @param {number} min - 최소값
 * @param {number} max - 최대값
 * @returns {number} 랜덤 실수
 */
export function randomFloat(min, max) {
  return Math.random() * (max - min) + min;
}

/**
 * HTTP 헤더 생성 (JWT 포함)
 *
 * @param {string} token - JWT 토큰
 * @returns {Object} HTTP 헤더
 */
export function makeHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}

/**
 * 성공 응답 체크
 *
 * @param {Object} response - HTTP 응답
 * @returns {boolean} 성공 여부
 */
export function isSuccess(response) {
  return response.status >= 200 && response.status < 300;
}

/**
 * JSON 응답 파싱
 *
 * @param {Object} response - HTTP 응답
 * @returns {Object|null} 파싱된 JSON 또는 null
 */
export function parseJSON(response) {
  try {
    return response.json();
  } catch (e) {
    console.error(`❌ JSON parse error: ${e}`);
    return null;
  }
}

/**
 * 시간 포맷 (ms → 초.밀리초)
 *
 * @param {number} ms - 밀리초
 * @returns {string} 포맷된 시간 (예: "1.234s")
 */
export function formatDuration(ms) {
  if (ms < 1000) {
    return `${ms.toFixed(0)}ms`;
  }
  return `${(ms / 1000).toFixed(3)}s`;
}

/**
 * 백분율 포맷
 *
 * @param {number} value - 0~1 사이 값
 * @returns {string} 포맷된 백분율 (예: "85.3%")
 */
export function formatPercent(value) {
  return `${(value * 100).toFixed(1)}%`;
}

/**
 * 숫자 포맷 (천단위 구분)
 *
 * @param {number} num - 숫자
 * @returns {string} 포맷된 숫자 (예: "1,234,567")
 */
export function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}
