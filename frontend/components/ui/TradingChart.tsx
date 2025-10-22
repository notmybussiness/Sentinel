"use client";

import React, { useEffect, useRef } from 'react';
import { createChart, ColorType } from 'lightweight-charts';

interface TradingChartProps {
  /** 차트 데이터 (시간별 가격) */
  data: Array<{ time: string; value: number }>;
  /** 차트 높이 */
  height?: number;
  /** 차트 타입 */
  type?: 'line' | 'area';
  /** 색상 (자동 판단: 상승=초록, 하락=빨강) */
  color?: string;
}

/**
 * TradingView 스타일 전문 차트
 *
 * 업비트/바이낸스와 유사한 차트 컴포넌트
 */
export function TradingChart({
  data,
  height = 300,
  type = 'area',
  color,
}: TradingChartProps) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<any>(null);
  const seriesRef = useRef<any>(null);

  useEffect(() => {
    if (!chartContainerRef.current || data.length === 0) return;

    // 색상 자동 판단
    const isPositive = data[data.length - 1]?.value >= data[0]?.value;
    const lineColor = color || (isPositive ? '#00ff88' : '#ff4d6d');
    const topColor = isPositive ? 'rgba(0, 255, 136, 0.4)' : 'rgba(255, 77, 109, 0.4)';
    const bottomColor = isPositive ? 'rgba(0, 255, 136, 0.0)' : 'rgba(255, 77, 109, 0.0)';

    // 차트 생성
    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: height,
      layout: {
        background: { type: ColorType.Solid, color: '#0a0a0a' },
        textColor: '#707070',
      },
      grid: {
        vertLines: { color: 'rgba(255, 255, 255, 0.05)' },
        horzLines: { color: 'rgba(255, 255, 255, 0.05)' },
      },
      timeScale: {
        borderColor: 'rgba(255, 255, 255, 0.1)',
        timeVisible: true,
        secondsVisible: false,
      },
      rightPriceScale: {
        borderColor: 'rgba(255, 255, 255, 0.1)',
      },
      crosshair: {
        vertLine: {
          color: 'rgba(0, 212, 255, 0.5)',
          width: 1,
          style: 1,
        },
        horzLine: {
          color: 'rgba(0, 212, 255, 0.5)',
          width: 1,
          style: 1,
        },
      },
    });

    // 시리즈 추가
    let series: any;
    if (type === 'area') {
      series = chart.addAreaSeries({
        lineColor: lineColor,
        topColor: topColor,
        bottomColor: bottomColor,
        lineWidth: 2,
      });
    } else {
      series = chart.addLineSeries({
        color: lineColor,
        lineWidth: 2,
      });
    }

    // 데이터 변환
    const chartData = data.map(item => ({
      time: item.time,
      value: item.value,
    }));

    series.setData(chartData);

    // 차트 크기 자동 조정
    chart.timeScale().fitContent();

    chartRef.current = chart;
    seriesRef.current = series;

    // 반응형 처리
    const handleResize = () => {
      if (chartContainerRef.current && chartRef.current) {
        chartRef.current.applyOptions({
          width: chartContainerRef.current.clientWidth,
        });
      }
    };

    window.addEventListener('resize', handleResize);

    // Cleanup
    return () => {
      window.removeEventListener('resize', handleResize);
      if (chartRef.current) {
        chartRef.current.remove();
      }
    };
  }, [data, height, type, color]);

  if (data.length === 0) {
    return (
      <div
        className="flex items-center justify-center bg-background-tertiary rounded-8"
        style={{ height }}
      >
        <span className="text-text-quaternary">데이터 없음</span>
      </div>
    );
  }

  return (
    <div
      ref={chartContainerRef}
      className="rounded-8 overflow-hidden"
      style={{ width: '100%', height }}
    />
  );
}
