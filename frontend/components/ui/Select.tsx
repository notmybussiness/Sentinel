"use client";

import React, { useState, useRef, useEffect } from "react";

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectProps {
  /** 옵션 목록 */
  options: SelectOption[];
  /** 선택된 값 */
  value?: string;
  /** 기본 선택값 */
  defaultValue?: string;
  /** 플레이스홀더 */
  placeholder?: string;
  /** 변경 콜백 */
  onChange?: (value: string) => void;
  /** 비활성화 여부 */
  disabled?: boolean;
  /** 에러 여부 */
  error?: boolean;
}

export function Select({
  options,
  value,
  defaultValue,
  placeholder = "선택하세요",
  onChange,
  disabled = false,
  error = false,
}: SelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState(value || defaultValue || "");
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((opt) => opt.value === selectedValue);

  // 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (optionValue: string) => {
    setSelectedValue(optionValue);
    setIsOpen(false);
    onChange?.(optionValue);
  };

  return (
    <div ref={containerRef} className="relative w-full">
      {/* Select 버튼 */}
      <button
        type="button"
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={`
          w-full px-4 py-2.5 rounded-8 text-regular text-left
          flex items-center justify-between
          transition-colors
          ${
            error
              ? "bg-background-secondary border-2 border-accent-red text-text-primary"
              : "bg-background-secondary border border-border-primary text-text-primary"
          }
          ${
            disabled
              ? "opacity-50 cursor-not-allowed"
              : "hover:bg-background-tertiary cursor-pointer"
          }
          focus:outline-none focus:border-brand-primary
        `}
      >
        <span className={selectedOption ? "text-text-primary" : "text-text-quaternary"}>
          {selectedOption?.label || placeholder}
        </span>
        <span
          className={`transition-transform ${isOpen ? "rotate-180" : "rotate-0"}`}
        >
          ▼
        </span>
      </button>

      {/* 드롭다운 메뉴 */}
      {isOpen && (
        <div className="absolute z-50 w-full mt-2 bg-background-quaternary border border-border-primary rounded-8 shadow-high max-h-60 overflow-y-auto">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => !option.disabled && handleSelect(option.value)}
              disabled={option.disabled}
              className={`
                w-full px-4 py-2.5 text-left text-regular
                transition-colors
                ${
                  option.value === selectedValue
                    ? "bg-brand-primary/10 text-brand-primary font-medium"
                    : "text-text-primary hover:bg-background-quinary"
                }
                ${option.disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"}
              `}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}