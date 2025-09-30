import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      // 다크 테마 색상 팔레트
      colors: {
        // 브랜드 색상 (네온 청록색)
        brand: {
          primary: "#00d4ff",
          secondary: "#0098ff",
          text: "#0a0a0a",
        },
        // 배경 색상 (진한 검정 계열)
        background: {
          primary: "#0a0a0a",      // 메인 배경
          secondary: "#131313",     // 보조 배경
          tertiary: "#1a1a1a",      // 3차 배경
          quaternary: "#1e1e1e",    // 카드 배경
          quinary: "#2a2a2a",       // 호버 배경
          elevated: "#242424",      // 강조 배경
        },
        // 텍스트 색상 (밝은 회색/흰색)
        text: {
          primary: "#ffffff",       // 주요 텍스트
          secondary: "#e0e0e0",     // 보조 텍스트
          tertiary: "#a0a0a0",      // 3차 텍스트
          quaternary: "#707070",    // 4차 텍스트 (비활성)
          disabled: "#505050",      // 비활성화
        },
        // 테두리 색상
        border: {
          primary: "#2a2a2a",
          secondary: "#333333",
          tertiary: "#404040",
          translucent: "rgba(255,255,255,.08)",
        },
        // 링크 색상
        link: {
          primary: "#00d4ff",
          hover: "#00ffff",
        },
        // 강조 색상 (수익/손실)
        accent: {
          blue: "#0098ff",          // 정보
          red: "#ff4d6d",           // 손실/매도
          green: "#00ff88",         // 수익/매수
          orange: "#ffaa00",        // 경고
          yellow: "#ffdd00",        // 주의
          purple: "#a78bfa",        // Mock 데이터
          indigo: "#8b5cf6",        // 보조
          cyan: "#00d4ff",          // 강조
        },
        // 의미론적 색상
        semantic: {
          success: "#00ff88",
          error: "#ff4d6d",
          warning: "#ffaa00",
          info: "#00d4ff",
        },
      },
      // 폰트 패밀리
      fontFamily: {
        sans: [
          "Inter Variable",
          "SF Pro Display",
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "Roboto",
          "sans-serif",
        ],
        serif: [
          "Tiempos Headline",
          "ui-serif",
          "Georgia",
          "Cambria",
          "Times New Roman",
          "Times",
          "serif",
        ],
        mono: [
          "JetBrains Mono",
          "Fira Code",
          "ui-monospace",
          "SF Mono",
          "Menlo",
          "monospace",
        ],
      },
      // 폰트 크기
      fontSize: {
        micro: "0.6875rem",
        mini: "0.75rem",
        small: "0.8125rem",
        regular: "0.9375rem",
        large: "1.125rem",
      },
      // 폰트 굵기
      fontWeight: {
        light: "300",
        normal: "400",
        medium: "500",
        semibold: "600",
        bold: "700",
      },
      // 테두리 반경
      borderRadius: {
        "4": "4px",
        "6": "6px",
        "8": "8px",
        "12": "12px",
        "16": "16px",
        "24": "24px",
        "32": "32px",
        "rounded": "9999px",
        "circle": "50%",
      },
      // 그림자 (다크 테마용 - 더 강한 그림자)
      boxShadow: {
        none: "0px 0px 0px transparent",
        tiny: "0px 1px 2px 0px rgba(0,0,0,.5)",
        low: "0px 2px 8px -2px rgba(0,0,0,.6)",
        medium: "0px 4px 16px rgba(0,0,0,.7)",
        high: "0px 8px 32px rgba(0,0,0,.8)",
        glow: "0px 0px 20px rgba(0, 212, 255, 0.3)",
        "glow-green": "0px 0px 20px rgba(0, 255, 136, 0.3)",
        "glow-red": "0px 0px 20px rgba(255, 77, 109, 0.3)",
      },
      // 애니메이션 속도
      transitionDuration: {
        quick: "100ms",
        regular: "250ms",
      },
      // z-index
      zIndex: {
        max: "10000",
        tooltip: "1100",
        toasts: "800",
        dialog: "700",
        popover: "600",
        overlay: "500",
        header: "100",
        footer: "50",
      },
      // 최대 너비
      maxWidth: {
        page: "1280px",
        prose: "736px",
      },
      // 백그라운드 이미지 (그라데이션)
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "gradient-conic": "conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))",
        "gradient-brand": "linear-gradient(135deg, #00d4ff 0%, #0098ff 100%)",
        "gradient-success": "linear-gradient(135deg, #00ff88 0%, #00cc6a 100%)",
        "gradient-danger": "linear-gradient(135deg, #ff4d6d 0%, #ff1744 100%)",
      },
    },
  },
  plugins: [],
};

export default config;
