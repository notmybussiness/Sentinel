# Bug Report Template - Sentinel

Use this template to report bugs found during QA testing.

---

## BUG-001: [Short Description]

**Reported By**: [Your Name]
**Date**: 2025-10-19
**Test Case**: TC-XXX
**Priority**: ⬜ Critical ⬜ High ⬜ Medium ⬜ Low

### Environment
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Browser**: Chrome/Edge/Firefox
- **OS**: Windows/Mac/Linux

### Component/Feature
- [ ] Unified Holding Modal
- [ ] PriceDisplay Component
- [ ] Dev Login
- [ ] AI Analysis Modal
- [ ] Rebalancing Modal
- [ ] Backtest Lab
- [ ] Other: _____________

### Severity Classification
- **Critical**: Application crash, data loss, security vulnerability
- **High**: Major feature broken, workaround exists
- **Medium**: Minor feature broken, non-critical
- **Low**: Cosmetic issue, minor UX improvement

### Steps to Reproduce
1.
2.
3.
4.
5.

### Expected Behavior
```
What should happen?
```

### Actual Behavior
```
What actually happens?
```

### Screenshots
Attach screenshots here or describe visually:
```


```

### Console Errors
```javascript
// Copy/paste console errors here


```

### Network Request (if applicable)
```http
POST /api/v1/portfolios/1/holdings HTTP/1.1

Request Body:
{

}

Response Status: 500
Response Body:
{

}
```

### Reproducibility
- [ ] Always (100%)
- [ ] Often (>50%)
- [ ] Sometimes (<50%)
- [ ] Rare (<10%)
- [ ] Cannot reproduce

### Impact
**Users Affected**: ⬜ All ⬜ Most ⬜ Some ⬜ Few

**Business Impact**:
```
How does this affect users or business operations?
```

### Root Cause Analysis (if known)
```


```

### Proposed Solution
```


```

### Workaround (if exists)
```
Temporary way to avoid the issue:
```

### Related Issues
- Related to BUG-XXX
- Blocks TC-XXX
- Caused by recent change: [commit hash or PR]

---

## Example Bug Reports

---

### BUG-EXAMPLE-001: Modal does not close after adding holding

**Reported By**: QA Team
**Date**: 2025-10-19
**Test Case**: TC-002
**Priority**: ⬜ Critical ✅ High ⬜ Medium ⬜ Low

#### Environment
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Browser**: Chrome 120
- **OS**: Windows 11

#### Component/Feature
- [x] Unified Holding Modal

#### Steps to Reproduce
1. Log in via dev login
2. Open portfolio detail page
3. Click "+ 종목 추가"
4. Fill Stock form: AAPL, 10, 150
5. Click "추가" button
6. API call succeeds (200 OK)
7. Modal remains open

#### Expected Behavior
```
Modal should close automatically after successful API response.
Holdings list should update to show new AAPL holding.
```

#### Actual Behavior
```
Modal stays open.
User must manually close modal.
Holdings list does update correctly, but modal is stuck.
```

#### Console Errors
```javascript
No console errors observed.
```

#### Network Request
```http
POST /api/v1/portfolios/123/holdings HTTP/1.1

Request Body:
{
  "symbol": "AAPL",
  "quantity": 10,
  "averageCost": 150.50,
  "assetType": "STOCK"
}

Response Status: 200 OK
Response Body:
{
  "id": 456,
  "symbol": "AAPL",
  "quantity": 10,
  "averageCost": 150.50,
  "assetType": "STOCK",
  "currentPrice": 152.30
}
```

#### Reproducibility
- [x] Always (100%)

#### Impact
**Users Affected**: ⬜ All ✅ Most ⬜ Some ⬜ Few

**Business Impact**:
```
High impact on user experience.
Users can still close modal manually, but it's confusing.
May cause users to think the operation failed.
```

#### Root Cause Analysis
```
Likely missing modal close logic in success callback.
Check AddHoldingModal component's onSuccess handler.
```

#### Proposed Solution
```typescript
// In AddHoldingModal component
const handleSubmit = async (data) => {
  try {
    await addHoldingMutation.mutateAsync(data);
    // Add this:
    onClose(); // Close modal on success
  } catch (error) {
    // Handle error
  }
};
```

#### Workaround
```
User can manually close modal by:
- Clicking backdrop
- Clicking X button
- Pressing ESC key
Holding is added correctly despite modal staying open.
```

---

### BUG-EXAMPLE-002: PriceDisplay shows "NaN" for failed crypto prices

**Reported By**: QA Team
**Date**: 2025-10-19
**Test Case**: TC-009
**Priority**: ✅ Critical ⬜ High ⬜ Medium ⬜ Low

#### Environment
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Browser**: Chrome 120
- **OS**: Windows 11

#### Component/Feature
- [x] PriceDisplay Component

#### Steps to Reproduce
1. Log in and create portfolio
2. Add crypto holding: SOL (Solana)
3. Upbit API fails to return price (500 error)
4. Observe PriceDisplay component

#### Expected Behavior
```
PriceDisplay should show "-" when price is null/undefined.
No console errors.
Component should not crash.
```

#### Actual Behavior
```
PriceDisplay shows "NaN" in the UI.
Console error: "TypeError: Cannot read property 'price' of null"
```

#### Console Errors
```javascript
TypeError: Cannot read property 'price' of null
    at PriceDisplay.tsx:15
    at updateComponent
    at React.render
```

#### Network Request
```http
GET /api/v1/crypto/price/SOL?baseCurrency=KRW HTTP/1.1

Response Status: 500 Internal Server Error
Response Body:
{
  "error": "Failed to fetch price from Upbit"
}
```

#### Reproducibility
- [x] Always (100%) for SOL, ADA, DOGE

#### Impact
**Users Affected**: ✅ All (anyone adding unsupported crypto)

**Business Impact**:
```
Critical UX issue. Displaying "NaN" is unacceptable.
May cause loss of user trust.
Should degrade gracefully.
```

#### Root Cause Analysis
```
PriceDisplay component does not handle null/undefined price.

Current code:
const price = holding.currentPrice;
return <span>${price.toFixed(2)}</span>; // Crashes if price is null

Should be:
const price = holding.currentPrice;
if (!price || isNaN(price)) return <span>-</span>;
return <span>${price.toFixed(2)}</span>;
```

#### Proposed Solution
```typescript
// In PriceDisplay.tsx
interface PriceDisplayProps {
  price: number | null | undefined;
  currency: 'USD' | 'KRW';
}

const PriceDisplay: React.FC<PriceDisplayProps> = ({ price, currency }) => {
  // Handle null/undefined/NaN
  if (price == null || isNaN(price)) {
    return <span className="text-gray-500">-</span>;
  }

  // Format valid price
  const formatted = currency === 'KRW'
    ? `₩${price.toLocaleString()}`
    : `$${price.toFixed(2)}`;

  return <span>{formatted}</span>;
};
```

#### Workaround
```
None. Users cannot avoid this issue when adding unsupported crypto.
```

---

### BUG-EXAMPLE-003: Dev login requires page refresh

**Reported By**: QA Team
**Date**: 2025-10-19
**Test Case**: TC-013
**Priority**: ⬜ Critical ✅ High ⬜ Medium ⬜ Low

#### Environment
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Browser**: Chrome 120
- **OS**: Windows 11

#### Component/Feature
- [x] Dev Login

#### Steps to Reproduce
1. Navigate to /login
2. Click "개발자 로그인 (테스트)"
3. API call succeeds, tokens saved to localStorage
4. Page does not redirect
5. User must manually refresh (F5)
6. Then redirect happens

#### Expected Behavior
```
After dev login API returns 200:
1. Tokens saved to localStorage
2. Immediate redirect to "/" (no refresh)
3. User sees authenticated home page
```

#### Actual Behavior
```
After dev login API returns 200:
1. Tokens saved to localStorage ✅
2. No redirect happens ❌
3. User stuck on login page
4. Manual refresh required
5. Then redirect works
```

#### Console Errors
```javascript
No errors. Just no redirect.
```

#### Network Request
```http
POST /api/v1/auth/dev-login HTTP/1.1

Response Status: 200 OK
Response Body:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {
    "id": 1,
    "email": "dev@sentinel.com",
    "name": "Developer"
  }
}
```

#### Reproducibility
- [x] Always (100%)

#### Impact
**Users Affected**: ✅ All (dev environment)

**Business Impact**:
```
Medium-high impact on developer experience.
Adds friction to testing workflow.
Users may think login failed.
```

#### Root Cause Analysis
```
Likely missing router.push() after successful login.

Current code:
const handleDevLogin = async () => {
  const response = await devLogin();
  // Tokens saved, but no redirect!
};

Should be:
const handleDevLogin = async () => {
  const response = await devLogin();
  router.push('/'); // Add this
};
```

#### Proposed Solution
```typescript
// In LoginPage.tsx or auth hook
import { useRouter } from 'next/navigation';

const handleDevLogin = async () => {
  try {
    const response = await devLogin();
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);

    // Add immediate redirect
    router.push('/');
  } catch (error) {
    console.error('Dev login failed:', error);
  }
};
```

#### Workaround
```
User can manually:
1. Refresh page (F5) after clicking dev login
2. Manually navigate to "/" in address bar
Login succeeds, just needs manual navigation.
```

---

## Blank Bug Report Template

Copy this section to create new bug reports:

---

### BUG-XXX: [Short Description]

**Reported By**:
**Date**: 2025-10-19
**Test Case**: TC-XXX
**Priority**: ⬜ Critical ⬜ High ⬜ Medium ⬜ Low

#### Environment
- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Browser**:
- **OS**:

#### Component/Feature
- [ ] Unified Holding Modal
- [ ] PriceDisplay Component
- [ ] Dev Login
- [ ] AI Analysis Modal
- [ ] Rebalancing Modal
- [ ] Backtest Lab
- [ ] Other: _____________

#### Steps to Reproduce
1.
2.
3.

#### Expected Behavior
```

```

#### Actual Behavior
```

```

#### Console Errors
```javascript


```

#### Network Request (if applicable)
```http


```

#### Reproducibility
- [ ] Always (100%)
- [ ] Often (>50%)
- [ ] Sometimes (<50%)
- [ ] Rare (<10%)

#### Impact
**Users Affected**: ⬜ All ⬜ Most ⬜ Some ⬜ Few

**Business Impact**:
```

```

#### Root Cause Analysis
```

```

#### Proposed Solution
```

```

#### Workaround
```

```

---

**End of Bug Report Template**
