"use client";

import React from "react";

interface SectionProps {
  /** 섹션 제목 (선택) */
  title?: string;
  /** 부제목 (선택) */
  subtitle?: string;
  /** 오른쪽 액션 (선택) */
  action?: React.ReactNode;
  /** 자식 컴포넌트 */
  children: React.ReactNode;
  /** 패딩 제거 여부 */
  noPadding?: boolean;
  /** 배경 색상 */
  background?: "primary" | "secondary" | "tertiary" | "none";
}

export function Section({
  title,
  subtitle,
  action,
  children,
  noPadding = false,
  background = "none",
}: SectionProps) {
  const bgClass = {
    primary: "bg-background-primary",
    secondary: "bg-background-secondary",
    tertiary: "bg-background-tertiary",
    none: "",
  }[background];

  return (
    <section className={`${bgClass} ${noPadding ? "" : "py-8"}`}>
      {/* 섹션 헤더 */}
      {(title || action) && (
        <div className={`flex items-start justify-between ${noPadding ? "" : "px-8"} mb-6`}>
          <div>
            {title && (
              <h2 className="text-2xl font-bold text-text-primary">{title}</h2>
            )}
            {subtitle && (
              <p className="text-text-tertiary text-regular mt-1">{subtitle}</p>
            )}
          </div>
          {action && <div>{action}</div>}
        </div>
      )}

      {/* 섹션 컨텐츠 */}
      <div className={noPadding ? "" : "px-8"}>{children}</div>
    </section>
  );
}