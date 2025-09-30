"use client";

import React from "react";

interface PercentageChangeProps {
  /** 변화율 (%) */
  value: number;
  /** 텍스트 크기 */
  size?: "sm" | "md" | "lg";
  /** 화살표 표시 여부 */
  showArrow?: boolean;
  /** 기간 표시 (선택) */
  period?: string;
}

export function PercentageChange({
  value,
  size = "md",
  showArrow = true,
  period,
}: PercentageChangeProps) {
  const isPositive = value > 0;
  const isNegative = value < 0;
  const isNeutral = value === 0;

  const colorClass = isPositive
    ? "text-accent-green"
    : isNegative
    ? "text-accent-red"
    : "text-text-tertiary";

  const sizeClass = {
    sm: "text-small",
    md: "text-regular",
    lg: "text-large",
  }[size];

  const arrow = isPositive ? "▲" : isNegative ? "▼" : "━";

  return (
    <span className={`inline-flex items-center gap-1 font-medium ${colorClass} ${sizeClass}`}>
      {showArrow && <span>{arrow}</span>}
      <span>
        {isPositive && "+"}
        {value.toFixed(2)}%
      </span>
      {period && (
        <span className="text-text-quaternary font-normal">({period})</span>
      )}
    </span>
  );
}