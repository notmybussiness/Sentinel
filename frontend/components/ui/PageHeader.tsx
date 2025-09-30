"use client";

import React from "react";

interface PageHeaderProps {
  /** 페이지 제목 */
  title: string;
  /** 부제목 (선택) */
  subtitle?: string;
  /** 오른쪽 액션 영역 (선택) */
  actions?: React.ReactNode;
  /** 하단 내비게이션/탭 (선택) */
  navigation?: React.ReactNode;
}

export function PageHeader({
  title,
  subtitle,
  actions,
  navigation,
}: PageHeaderProps) {
  return (
    <div className="border-b border-border-primary">
      <div className="py-6 px-8">
        <div className="flex items-start justify-between">
          {/* 제목 영역 */}
          <div>
            <h1 className="text-3xl font-bold text-text-primary">{title}</h1>
            {subtitle && (
              <p className="text-text-tertiary text-regular mt-2">{subtitle}</p>
            )}
          </div>

          {/* 액션 영역 */}
          {actions && <div className="flex items-center gap-3">{actions}</div>}
        </div>
      </div>

      {/* 네비게이션 영역 */}
      {navigation && <div className="px-8">{navigation}</div>}
    </div>
  );
}