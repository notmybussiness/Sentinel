"use client";

import React, { useState } from "react";

export interface Tab {
  /** 탭 키 */
  key: string;
  /** 탭 레이블 */
  label: string;
  /** 탭 내용 */
  content: React.ReactNode;
  /** 비활성화 여부 */
  disabled?: boolean;
  /** 배지 (선택) */
  badge?: string | number;
}

interface TabsProps {
  /** 탭 목록 */
  tabs: Tab[];
  /** 기본 활성 탭 */
  defaultTab?: string;
  /** 탭 변경 콜백 */
  onChange?: (key: string) => void;
}

export function Tabs({ tabs, defaultTab, onChange }: TabsProps) {
  const [activeTab, setActiveTab] = useState(defaultTab || tabs[0]?.key);

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    onChange?.(key);
  };

  const currentTab = tabs.find((tab) => tab.key === activeTab);

  return (
    <div className="w-full">
      {/* 탭 헤더 */}
      <div className="flex border-b border-border-primary">
        {tabs.map((tab) => {
          const isActive = tab.key === activeTab;
          return (
            <button
              key={tab.key}
              onClick={() => !tab.disabled && handleTabChange(tab.key)}
              disabled={tab.disabled}
              className={`
                relative px-4 py-3 text-regular font-medium transition-colors
                ${
                  isActive
                    ? "text-brand-primary"
                    : "text-text-tertiary hover:text-text-secondary"
                }
                ${tab.disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"}
              `}
            >
              <span className="flex items-center gap-2">
                {tab.label}
                {tab.badge !== undefined && (
                  <span
                    className={`
                    px-2 py-0.5 rounded-4 text-mini font-semibold
                    ${
                      isActive
                        ? "bg-brand-primary/20 text-brand-primary"
                        : "bg-background-tertiary text-text-quaternary"
                    }
                  `}
                  >
                    {tab.badge}
                  </span>
                )}
              </span>
              {/* 활성 탭 하단 바 */}
              {isActive && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-brand-primary" />
              )}
            </button>
          );
        })}
      </div>

      {/* 탭 컨텐츠 */}
      <div className="py-4">{currentTab?.content}</div>
    </div>
  );
}