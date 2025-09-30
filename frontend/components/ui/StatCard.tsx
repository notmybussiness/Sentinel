"use client";

import React from "react";
import { Card } from "./Card";

interface StatCardProps {
  /** 통계 제목 */
  label: string;
  /** 주요 값 */
  value: string | number;
  /** 변화량 (선택) */
  change?: number;
  /** 변화 기간 (선택) */
  changePeriod?: string;
  /** 아이콘 (선택) */
  icon?: React.ReactNode;
  /** 추가 정보 (선택) */
  subtitle?: string;
  /** 클릭 핸들러 */
  onClick?: () => void;
  /** Mock 데이터 여부 */
  isMock?: boolean;
}

export function StatCard({
  label,
  value,
  change,
  changePeriod = "24h",
  icon,
  subtitle,
  onClick,
  isMock = false,
}: StatCardProps) {
  const changeColor =
    change !== undefined
      ? change > 0
        ? "text-accent-green"
        : change < 0
        ? "text-accent-red"
        : "text-text-tertiary"
      : "";

  return (
    <Card
      onClick={onClick}
      padding="sm"
      className={`relative ${onClick ? "cursor-pointer hover:bg-background-quinary" : ""}`}
    >
      {/* Mock 데이터 배지 */}
      {isMock && (
        <div className="absolute top-1.5 right-1.5 px-1.5 py-0.5 bg-accent-purple/20 text-accent-purple text-micro rounded-4">
          MOCK
        </div>
      )}

      {/* 헤더 */}
      <div className="flex items-center justify-between mb-2">
        <span className="text-text-tertiary text-mini">{label}</span>
        {icon && <div className="text-text-tertiary text-sm">{icon}</div>}
      </div>

      {/* 값 */}
      <div className="mb-1">
        <div className="text-text-primary text-xl font-semibold">{value}</div>
        {subtitle && (
          <div className="text-text-quaternary text-micro mt-0.5">{subtitle}</div>
        )}
      </div>

      {/* 변화량 */}
      {change !== undefined && (
        <div className={`flex items-center gap-1 text-mini ${changeColor}`}>
          <span>{change > 0 ? "▲" : change < 0 ? "▼" : "━"}</span>
          <span className="font-medium">
            {Math.abs(change).toFixed(2)}%
          </span>
          <span className="text-text-quaternary text-micro">({changePeriod})</span>
        </div>
      )}
    </Card>
  );
}