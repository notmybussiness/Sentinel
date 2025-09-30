"use client";

import React, { useState, useEffect } from "react";
import { Button } from "./Button";

interface CarouselProps {
  /** 캐러셀 아이템들 */
  children: React.ReactNode[];
  /** 자동 재생 여부 */
  autoplay?: boolean;
  /** 자동 재생 간격 (ms) */
  interval?: number;
  /** 한 번에 보여줄 아이템 수 */
  itemsPerView?: number;
  /** 슬라이드 이동 단위 */
  slidesToScroll?: number;
  /** 간격 (px) */
  gap?: number;
}

export function Carousel({
  children,
  autoplay = false,
  interval = 5000,
  itemsPerView = 3,
  slidesToScroll = 1,
  gap = 16,
}: CarouselProps) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const totalItems = children.length;
  const maxIndex = Math.max(0, totalItems - itemsPerView);

  // 자동 재생
  useEffect(() => {
    if (!autoplay || maxIndex === 0) return;

    const timer = setInterval(() => {
      setCurrentIndex((prev) => {
        const nextIndex = prev + slidesToScroll;
        return nextIndex > maxIndex ? 0 : nextIndex;
      });
    }, interval);

    return () => clearInterval(timer);
  }, [autoplay, interval, maxIndex, slidesToScroll]);

  const goToPrevious = () => {
    setCurrentIndex((prev) => {
      const nextIndex = prev - slidesToScroll;
      return nextIndex < 0 ? maxIndex : nextIndex;
    });
  };

  const goToNext = () => {
    setCurrentIndex((prev) => {
      const nextIndex = prev + slidesToScroll;
      return nextIndex > maxIndex ? 0 : nextIndex;
    });
  };

  const itemWidth = `calc((100% - ${gap * (itemsPerView - 1)}px) / ${itemsPerView})`;
  const translateX = -(currentIndex * (100 / itemsPerView));

  return (
    <div className="relative">
      {/* 캐러셀 컨테이너 */}
      <div className="overflow-hidden">
        <div
          className="flex transition-transform duration-700 ease-in-out"
          style={{
            transform: `translateX(${translateX}%)`,
            gap: `${gap}px`,
          }}
        >
          {children.map((child, index) => (
            <div
              key={index}
              className="flex-shrink-0"
              style={{ width: itemWidth }}
            >
              {child}
            </div>
          ))}
        </div>
      </div>

      {/* 네비게이션 버튼 */}
      {maxIndex > 0 && (
        <>
          <Button
            variant="secondary"
            size="sm"
            onClick={goToPrevious}
            className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-2 z-10 rounded-full w-10 h-10 flex items-center justify-center shadow-high"
            aria-label="이전"
          >
            ←
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={goToNext}
            className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-2 z-10 rounded-full w-10 h-10 flex items-center justify-center shadow-high"
            aria-label="다음"
          >
            →
          </Button>
        </>
      )}

      {/* 인디케이터 */}
      {maxIndex > 0 && (
        <div className="flex justify-center gap-2 mt-4">
          {Array.from({ length: maxIndex + 1 }).map((_, index) => (
            <button
              key={index}
              onClick={() => setCurrentIndex(index)}
              className={`w-2 h-2 rounded-full transition-all ${
                index === currentIndex
                  ? "bg-brand-primary w-6"
                  : "bg-border-tertiary hover:bg-border-secondary"
              }`}
              aria-label={`슬라이드 ${index + 1}로 이동`}
            />
          ))}
        </div>
      )}
    </div>
  );
}