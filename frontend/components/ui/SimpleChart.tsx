"use client";

import React from 'react';

interface SimpleChartProps {
  /** 차트 데이터 포인트 (값만) */
  data: number[];
  /** 차트 높이 */
  height?: number;
  /** 차트 색상 (양수/음수 자동) */
  color?: 'auto' | 'green' | 'red' | 'blue';
}

/**
 * 간단한 라인 차트
 *
 * 실제 차트 라이브러리 대신 SVG로 간단하게 구현
 */
export function SimpleChart({
  data,
  height = 60,
  color = 'auto',
}: SimpleChartProps) {
  if (data.length < 2) {
    return (
      <div
        className="flex items-center justify-center bg-background-tertiary rounded-8"
        style={{ height }}
      >
        <span className="text-text-quaternary text-mini">데이터 부족</span>
      </div>
    );
  }

  // 데이터 정규화
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  // 색상 결정
  const isPositive = data[data.length - 1] >= data[0];
  const chartColor =
    color === 'auto'
      ? isPositive
        ? '#00ff88'
        : '#ff4d6d'
      : color === 'green'
      ? '#00ff88'
      : color === 'red'
      ? '#ff4d6d'
      : '#00d4ff';

  // SVG 포인트 생성
  const width = 100;
  const padding = 5;
  const points = data
    .map((value, index) => {
      const x = (index / (data.length - 1)) * (width - padding * 2) + padding;
      const y =
        height -
        ((value - min) / range) * (height - padding * 2) -
        padding;
      return `${x},${y}`;
    })
    .join(' ');

  // 영역 채우기용 포인트
  const areaPoints = `0,${height} ${points} ${width},${height}`;

  return (
    <svg
      width="100%"
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      className="rounded-8"
    >
      {/* 배경 */}
      <rect
        width={width}
        height={height}
        fill="var(--tw-colors-background-tertiary)"
      />

      {/* 영역 채우기 */}
      <polygon
        points={areaPoints}
        fill={chartColor}
        opacity="0.1"
      />

      {/* 라인 */}
      <polyline
        points={points}
        fill="none"
        stroke={chartColor}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}