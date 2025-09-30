# 🎨 Design System & Theme Data

## Color Palette

### Primary Colors
```typescript
// Brand Colors
'brand-primary': '#00d4ff',      // Neon cyan - primary brand color
'brand-secondary': '#0099cc',    // Darker cyan - hover states
'brand-text': '#0a0a0a',        // Text on brand background

// Background Colors
'background-primary': '#0a0a0a',    // Main page background
'background-secondary': '#131313',  // Card background
'background-tertiary': '#1a1a1a',   // Slightly lighter background
'background-quaternary': '#2a2a2a', // Section backgrounds
```

### Accent Colors
```typescript
// Success/Positive
'accent-green': '#00ff88',        // Gains, positive changes
'success-dark': '#00cc6a',        // Darker success state

// Error/Negative
'accent-red': '#ff4d6d',          // Losses, negative changes
'error-dark': '#cc3d57',          // Darker error state

// Warning
'warning': '#ffc107',             // Warning states
'warning-dark': '#ff9800',        // Darker warning

// Info
'info': '#2196f3',                // Info messages
```

### Text Colors
```typescript
'text-primary': '#ffffff',         // Main text
'text-secondary': '#b3b3b3',       // Secondary text
'text-tertiary': '#808080',        // Labels, hints
'text-quaternary': '#4d4d4d',      // Disabled text
```

### Border Colors
```typescript
'border-primary': '#333333',       // Default borders
'border-secondary': '#262626',     // Subtle borders
```

### Special Colors
```typescript
'mock-badge': '#a78bfa',          // Purple mock data indicator
'link-primary': '#00d4ff',        // Links
'link-hover': '#00ffcc',          // Link hover
```

---

## Gradients

### Brand Gradient
```css
.bg-gradient-brand {
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
}
```

### Success Gradient
```css
.bg-gradient-success {
  background: linear-gradient(135deg, #00ff88 0%, #00cc6a 100%);
}
```

### Danger Gradient
```css
.bg-gradient-danger {
  background: linear-gradient(135deg, #ff4d6d 0%, #cc3d57 100%);
}
```

### Page Background Gradient
```css
body {
  background: linear-gradient(135deg, #0a0a0a 0%, #0f0f0f 50%, #0a0a0a 100%);
}
```

---

## Glassmorphism Effect

### Card Style
```css
.glass-card {
  background: rgba(19, 19, 19, 0.8);        /* 80% opacity */
  backdrop-filter: blur(8px);               /* Blur effect */
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(51, 51, 51, 0.5); /* Semi-transparent border */
}

.glass-card:hover {
  background: rgba(19, 19, 19, 0.9);        /* 90% on hover */
}
```

---

## Spacing System

### Padding/Margin Scale
```typescript
'spacing-xs': '4px',    // 0.25rem
'spacing-sm': '8px',    // 0.5rem
'spacing-md': '12px',   // 0.75rem (default gap)
'spacing-lg': '16px',   // 1rem
'spacing-xl': '24px',   // 1.5rem
'spacing-2xl': '32px',  // 2rem
```

### Gap Classes (Compact Design)
```css
gap-1: 4px
gap-2: 8px
gap-3: 12px   /* Standard gap */
gap-4: 16px
gap-6: 24px
```

### Container Widths
```css
max-w-6xl: 1152px  /* Standard content width */
max-w-7xl: 1280px  /* Header width */
px-8: 32px         /* Horizontal padding */
```

---

## Border Radius

### Rounded Corners
```typescript
'rounded-4': '4px',
'rounded-8': '8px',
'rounded-12': '12px',   // Standard for cards
'rounded-16': '16px',
'rounded-full': '9999px',
```

---

## Shadows & Effects

### Box Shadows
```css
.shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.5);
.shadow-md: 0 4px 6px rgba(0, 0, 0, 0.5);
.shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.5);

/* Glow Effects */
.shadow-glow: 0 0 20px rgba(0, 212, 255, 0.3);
.shadow-glow-green: 0 0 20px rgba(0, 255, 136, 0.3);
.shadow-glow-red: 0 0 20px rgba(255, 77, 109, 0.3);
```

### Backdrop Blur
```css
.backdrop-blur-none: backdrop-filter: blur(0);
.backdrop-blur-sm: backdrop-filter: blur(8px);
.backdrop-blur-md: backdrop-filter: blur(12px);
.backdrop-blur-lg: backdrop-filter: blur(16px);
```

---

## Typography

### Font Families
```typescript
fontFamily: {
  sans: ['Geist', 'system-ui', 'sans-serif'],
  mono: ['Geist Mono', 'monospace'],
}
```

### Font Sizes
```typescript
'text-xs': '12px',
'text-sm': '14px',     // Most text
'text-base': '16px',
'text-lg': '18px',     // Card titles
'text-xl': '20px',
'text-2xl': '24px',
'text-3xl': '30px',    // Page headers
```

### Font Weights
```typescript
'font-normal': 400,
'font-medium': 500,
'font-semibold': 600,   // Headings
'font-bold': 700,
```

---

## Animation & Transitions

### Transition Durations
```typescript
'duration-fast': '150ms',
'duration-regular': '300ms',   // Standard
'duration-slow': '500ms',
'duration-carousel': '700ms',  // Carousel slides
```

### Easing Functions
```css
ease-in-out  /* Standard easing */
ease-out     /* Button hovers */
```

### Common Animations
```css
/* Fade In */
@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Slide In from Bottom */
@keyframes slide-in-from-bottom {
  from { transform: translateY(1rem); }
  to { transform: translateY(0); }
}

/* Pulse Glow (Neon Effect) */
@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 20px rgba(0, 212, 255, 0.3); }
  50% { box-shadow: 0 0 30px rgba(0, 212, 255, 0.5); }
}
```

---

## Responsive Breakpoints

```typescript
screens: {
  'sm': '640px',
  'md': '768px',    // Desktop-first breakpoint
  'lg': '1024px',
  'xl': '1280px',
  '2xl': '1536px',
}
```

### Usage Pattern
Desktop-first approach:
```tsx
// Default: mobile styles
className="text-sm"

// md: and above (768px+)
className="text-sm md:text-base md:flex"
```

---

## Usage Examples

### Glassmorphism Card
```tsx
<div className="bg-background-secondary/80 backdrop-blur-sm border border-border-primary/50 rounded-12 p-3 hover:bg-background-secondary/90 transition-all duration-regular">
  Content
</div>
```

### Percentage Color
```tsx
const color = value >= 0 ? 'text-accent-green' : 'text-accent-red';
```

### Mock Badge
```tsx
<span className="bg-mock-badge/20 text-mock-badge border border-mock-badge/30 rounded-8 px-2 py-1 text-xs">
  MOCK
</span>
```

---

## Accessibility

### Contrast Ratios
- Text on dark background: WCAG AA compliant (4.5:1)
- Links: Clear color differentiation
- Focus states: Visible outline on all interactive elements

### Focus Indicators
```css
.focus-visible:focus {
  outline: 2px solid #00d4ff;
  outline-offset: 2px;
}
```

---

**Last Updated**: 2025-10-01
**Design Language**: Glassmorphism Dark Theme
**Primary Framework**: Tailwind CSS 3.4