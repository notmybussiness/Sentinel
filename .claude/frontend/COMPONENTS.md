# 🧩 Frontend Component Library

## Component Categories

### UI Base Components (`components/ui/`)

#### 1. **Card**
**Path**: `components/ui/Card.tsx`

**Props**:
```typescript
interface CardProps {
  children: React.ReactNode;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  className?: string;
  onClick?: () => void;
}
```

**Features**:
- Glassmorphism effect (backdrop-blur-sm, 80% opacity)
- Hover state (90% opacity)
- Semi-transparent borders
- Responsive padding

**Usage**:
```tsx
<Card padding="md">
  <h3>Card Title</h3>
  <p>Content here</p>
</Card>
```

---

#### 2. **Button**
**Path**: `components/ui/Button.tsx`

**Variants**:
- `primary`: Brand gradient background
- `secondary`: Semi-transparent with border
- `ghost`: Transparent with hover effect
- `danger`: Red accent for destructive actions

**Sizes**: `sm`, `md`, `lg`

**Usage**:
```tsx
<Button variant="primary" size="md" onClick={handleClick}>
  Submit
</Button>
```

---

#### 3. **StatCard**
**Path**: `components/ui/StatCard.tsx`

**Props**:
```typescript
interface StatCardProps {
  label: string;
  value: string | number | React.ReactNode;
  change?: number;
  changePeriod?: string;
  isMock?: boolean;
}
```

**Features**:
- Automatic color coding (green: positive, red: negative)
- Mock badge display
- Percentage change with arrow indicators

**Usage**:
```tsx
<StatCard
  label="Total Value"
  value="₩50,000,000"
  change={11.5}
  changePeriod="전월 대비"
  isMock
/>
```

---

#### 4. **PercentageChange**
**Path**: `components/ui/PercentageChange.tsx`

**Features**:
- Automatic color (green/red based on value)
- Arrow indicators (↑/↓)
- Percentage formatting

**Usage**:
```tsx
<PercentageChange value={5.23} />
// Output: +5.23% ↑ (green)
```

---

#### 5. **PriceDisplay**
**Path**: `components/ui/PriceDisplay.tsx`

**Supported Currencies**: ₩, $, ₿, Ξ

**Usage**:
```tsx
<PriceDisplay amount={150000} currency="₩" />
// Output: ₩150,000
```

---

#### 6. **Carousel**
**Path**: `components/ui/Carousel.tsx`

**Props**:
```typescript
interface CarouselProps {
  children: React.ReactNode[];
  itemsPerView?: number;    // How many items to show (default: 3)
  slidesToScroll?: number;  // How many to slide per click (default: 1)
  gap?: number;             // Gap between items (default: 12px)
  autoplay?: boolean;
  interval?: number;        // Autoplay interval in ms
}
```

**Features**:
- Smooth sliding animation (duration-700, ease-in-out)
- Auto-play support
- Navigation arrows
- Responsive layout

**Usage**:
```tsx
<Carousel itemsPerView={3} slidesToScroll={1} autoplay interval={5000}>
  {items.map(item => <ItemCard key={item.id} {...item} />)}
</Carousel>
```

---

#### 7. **SimpleChart**
**Path**: `components/ui/SimpleChart.tsx`

**Features**:
- SVG-based line chart
- Gradient fill
- Responsive width
- Mock data support

**Usage**:
```tsx
<SimpleChart
  data={[100, 120, 115, 130, 125]}
  height={160}
  color="#00d4ff"
/>
```

---

#### 8. **Tabs**
**Path**: `components/ui/Tabs.tsx`

**Usage**:
```tsx
<Tabs defaultValue="all">
  <TabsList>
    <TabsTrigger value="all">All</TabsTrigger>
    <TabsTrigger value="stocks">Stocks</TabsTrigger>
  </TabsList>
  <TabsContent value="all">
    Content for All tab
  </TabsContent>
</Tabs>
```

---

#### 9. **Select**
**Path**: `components/ui/Select.tsx`

**Props**:
```typescript
interface SelectProps {
  value: string;
  onValueChange: (value: string) => void;
  options: Array<{label: string; value: string}>;
  placeholder?: string;
}
```

---

#### 10. **Modal**
**Path**: `components/ui/Modal.tsx`

**Features**:
- Backdrop blur
- Escape key to close
- Click outside to close
- Smooth fade animation

**Usage**:
```tsx
<Modal isOpen={isOpen} onClose={handleClose} title="Modal Title">
  <p>Modal content</p>
</Modal>
```

---

#### 11. **PageHeader**
**Path**: `components/ui/PageHeader.tsx`

**Usage**:
```tsx
<PageHeader
  title="Portfolio"
  subtitle="Manage your investments"
  action={<Button>Create New</Button>}
/>
```

---

#### 12. **Section**
**Path**: `components/ui/Section.tsx`

**Features**:
- Title + subtitle + action button
- Customizable padding and background
- Consistent spacing

---

### Domain Components

#### Portfolio Components (`components/portfolio/`)

**1. PortfolioCard**: Portfolio summary with chart
**2. RecommendedPortfolioCard**: Carousel portfolio cards
**3. HoldingItem**: Individual stock holding display
**4. RebalancingModal**: Buy/Sell recommendations modal

#### Market Components (`components/market/`)

**1. IndexCard**: Market index display
**2. StockSearchBar**: Asset search with autocomplete
**3. AssetFilter**: Type filter (ALL/STOCK/CRYPTO/COMMODITY)

#### Layout Components (`components/layout/`)

**1. Header**: Navigation with auth menu

#### Common Components (`components/common/`)

**1. MockDataBadge**: Purple badge for mock data
**2. EmptyState**: Empty state with icon and message

---

## Design Patterns

### Glassmorphism
All cards use glassmorphism effect:
```css
bg-background-secondary/80 backdrop-blur-sm border-border-primary/50
hover:bg-background-secondary/90
```

### Spacing
Consistent compact spacing:
- Gap: `gap-3` (12px)
- Padding: `p-3` (12px)
- Margin: `mb-2` (8px)

### Typography
- Headings: `font-semibold`
- Body: `text-sm` or `text-base`
- Labels: `text-xs` with `text-text-tertiary`

---

## Component Development Checklist

When creating new components:
- [ ] Add TypeScript interface for props
- [ ] Add JSDoc comments
- [ ] Use "use client" if interactive
- [ ] Support dark theme colors
- [ ] Add responsive breakpoints (md: 768px+)
- [ ] Include loading/error states
- [ ] Add to this documentation

---

**Last Updated**: 2025-10-01
**Total Components**: 20+ (12 UI base + 8 domain)