"use client";

import React from "react";

interface PriceDisplayProps {
  /** 가격 */
  amount: number | null;
  /** 통화 */
  currency?: "KRW" | "USD" | "BTC" | "ETH";
  /** 텍스트 크기 */
  size?: "sm" | "md" | "lg" | "xl";
  /** 텍스트 색상 */
  color?: "primary" | "secondary" | "tertiary" | "success" | "error";
  /** 소수점 자릿수 */
  decimals?: number;
}

export function PriceDisplay({
  amount,
  currency = "KRW",
  size = "md",
  color = "primary",
  decimals,
}: PriceDisplayProps) {
  const sizeClass = {
    sm: "text-small",
    md: "text-regular",
    lg: "text-large",
    xl: "text-2xl",
  }[size];

  const colorClass = {
    primary: "text-text-primary",
    secondary: "text-text-secondary",
    tertiary: "text-text-tertiary",
    success: "text-accent-green",
    error: "text-accent-red",
  }[color];

  const formatPrice = (value: number | null): string => {
    // null 또는 undefined 체크
    if (value === null || value === undefined || isNaN(value)) {
      return "-";
    }

    const decimalPlaces = decimals ?? (currency === "KRW" ? 0 : 2);

    switch (currency) {
      case "KRW":
        return `₩${value.toLocaleString("ko-KR", {
          minimumFractionDigits: decimalPlaces,
          maximumFractionDigits: decimalPlaces,
        })}`;
      case "USD":
        return `$${value.toLocaleString("en-US", {
          minimumFractionDigits: decimalPlaces,
          maximumFractionDigits: decimalPlaces,
        })}`;
      case "BTC":
        return `₿${value.toFixed(8)}`;
      case "ETH":
        return `Ξ${value.toFixed(6)}`;
      default:
        return value.toLocaleString();
    }
  };

  return (
    <span className={`font-semibold tabular-nums ${sizeClass} ${colorClass}`}>
      {formatPrice(amount)}
    </span>
  );
}