# 00-problem-understanding.md

## Problem Understanding & Bottleneck Discovery

### 1) What is the nature of the backend problem?

- Build **Order Service** (Spring Boot 3.x, Java 17+, Gradle) providing REST APIs for orders:
  `POST /api/orders` (create + validate + payment), `GET /api/orders/{id}`, `GET /api/orders`, `PUT /api/orders/{id}` (cancel only).
- Order Service must **integrate** 3 external services (only OpenAPI available, do not exist in reality → must mock):

  - **Member Service**: validate member exists & active before creating order. Endpoint: `GET /api/members/{memberId}`.
  - **Product Service**: retrieve product info and check stock. Endpoints: `GET /api/products/{productId}`, `GET /api/products/{productId}/stock`.
  - **Payment Service**: process payment when order is "confirmed/ready". Endpoint: `POST /api/payments` (and `GET /api/payments/{paymentId}` for lookup).

- The test evaluates **production mindset in microservices**: separation of concerns, dependency management, error handling, resilience, test strategy, docs.

---

### 2) System values to protect?

**Important "assets" to protect (production mindset):**

1. **Order Business Correctness**

   - Do not create order for invalid/inactive member.
   - Do not create/confirm order when product does not exist or insufficient stock.

2. **State Consistency between Order ↔ Payment**

   - Payment is only called when order is in appropriate state ("confirmed/ready").

3. **Reliability when depending on external services**

   - External service can timeout/be unavailable/return error → Order service must still handle it controllably, return clear error, log sufficiently.

4. **Auditability & debuggability**

   - Logging + "meaningful" error messages for tracing, observability.

---

### 3) External Services Details (from OpenAPI Specs)

#### 3.1) Member Service

```yaml
Member:
  id: int64
  name: string
  email: string (email format)
  status: enum [ACTIVE, INACTIVE, SUSPENDED] # ⚠️ Need to handle all 3 statuses
  grade: enum [BRONZE, SILVER, GOLD, PLATINUM] # 🔹 Extension point for future

ErrorResponse: { code: "MEMBER_NOT_FOUND", message: "..." }
```

**Business Rules to define:**
| Status | Allow Order Creation? | Reason |
|--------|-----------------------|--------|
| `ACTIVE` | ✅ Yes | Member operating normally |
| `INACTIVE` | ❌ No | Member has stopped operating |
| `SUSPENDED` | ❌ No | Member suspended (policy violation) |

**Assumption**: `grade` (BRONZE/SILVER/GOLD/PLATINUM) does not affect order business logic in this scope. Can be extended to apply discount/priority later.

---

#### 3.2) Product Service

```yaml
Product:
  id: int64
  name: string
  price: double
  status: enum [AVAILABLE, OUT_OF_STOCK, DISCONTINUED] # ⚠️ Important!

ProductStock:
  productId: int64
  quantity: int # Total in warehouse
  reservedQuantity: int # Reserved for pending orders
  availableQuantity: int # = quantity - reservedQuantity, orderable
```

**Business Rules to define:**
| Product Status | Stock Check | Allow Order? |
|----------------|-------------|--------------|
| `AVAILABLE` | `availableQuantity >= requestedQty` | ✅ Yes |
| `AVAILABLE` | `availableQuantity < requestedQty` | ❌ No (insufficient stock) |
| `OUT_OF_STOCK` | Any | ❌ No (unless back-order) |
| `DISCONTINUED` | Any | ❌ No (product discontinued) |

**⚠️ Design Gap - Stock Reservation:**

- Spec returns `reservedQuantity` → External Product Service **has internal reservation mechanism**.
- **BUT** does not expose endpoint for Order Service to call reserve/deduct stock.
- **Accepted Risk**: Race condition when concurrent orders check stock OK together → both create → oversell.
- **Mitigation in this scope**: Best-effort check before order creation. Log warning about limitation.

---

#### 3.3) Payment Service

```yaml
PaymentRequest: # ← Request body when calling POST /api/payments
  orderId: int64 (required)
  amount: double (required)
  paymentMethod: enum [CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER] (required!) # ⚠️

Payment: # ← Response
  id: int64
  orderId: int64
  amount: double
  status: enum [PENDING, COMPLETED, FAILED, REFUNDED] # ⚠️ 4 statuses!
  transactionId: string
  createdAt: datetime

Error Responses:
  400: INVALID_PAYMENT_REQUEST (validation error - bad request)
  422: PAYMENT_FAILED (business error - e.g., insufficient funds)
```

**Business Decisions to define:**

1.  **Where does PaymentMethod come from?** → Order entity must have `paymentMethod` field or receive from request when confirming.
2.  **Handle 4 Payment statuses:**
    - `COMPLETED` → Order transitions to COMPLETED
    - `PENDING` → Order waits at PAYMENT_PENDING state (async processing)
    - `FAILED` → Order rollback to CONFIRMED or PAYMENT_FAILED
    - `REFUNDED` → Trigger from cancel order flow

**Error Handling:**
| HTTP Status | Error Code | Retry? | Action |
|-------------|------------|--------|--------|
| 400 | `INVALID_PAYMENT_REQUEST` | ❌ No | Fix request data |
| 422 | `PAYMENT_FAILED` | 🔄 Maybe | Can try another method |

---

### 4) Bottleneck / Main Risks

1.  **Distributed failure & latency** (biggest risk point)

    - Order Service depends on 3 external calls (Member/Product/Payment). When 1 service is slow or down → likely causes timeouts, cascade failure.

2.  **Specific Race Conditions:**

    - **Concurrent order creation**: 2 orders check stock OK together → both create → oversell
    - **Double submit confirm**: User clicks twice → 2 payment requests for same order
    - **Order update while payment processing**: PUT order while payment is PENDING

3.  **Idempotency & double payment**

    - If retry payment or client calls again → can create multiple payments for same order if no guard.
    - Payment spec has no idempotency key → Order Service needs to implement guard itself (check if payment already exists for order).

4.  **Business flow design for create/update/confirm**

    - Requirement says "when creating or processing order" must validate/check/payment, but does not define detailed state machine.

---

### 5) Order State Machine (Proposed)

```
POST /api/orders
      │
      ├─1. Validate Member (GET /api/members/{id})
      │     ├─ Member does not exist ──────────────→ 404 (Order not created)
      │     ├─ Member INACTIVE/SUSPENDED ──────────→ 400 (Order not created)
      │     ├─ Member Service timeout/unavailable ─→ 503 (Order not created)
      │     └─ Member ACTIVE ✅ → continue
      │
      ├─2. Validate Product + Stock (GET /api/products/{id}, /stock)
      │     ├─ Product does not exist ─────────────→ 404 (Order not created)
      │     ├─ Product DISCONTINUED ───────────────→ 400 (Order not created)
      │     ├─ Insufficient stock ─────────────────→ 400 (Order not created)
      │     ├─ Product Service timeout/unavailable → 503 (Order not created)
      │     └─ Product OK + Stock OK ✅ → continue
      │
      ├─3. Call Payment Service (POST /api/payments)
      │     ├─ Payment SUCCESS ────────────────────→ Order = CONFIRMED ✅
      │     ├─ Payment PENDING ────────────────────→ Order = PENDING ⏳
      │     ├─ Payment FAILED ─────────────────────→ Order = FAILED ❌
      │     └─ Payment Service timeout/unavailable → 503 (Order not created)
      │
      └─ Response: OrderResponse with corresponding status


PUT /api/orders/{id}
      │
      └─ Cancel Order
            ├─ CONFIRMED → ❌ Error (already paid)
            ├─ PENDING   → ✅ CANCELLED
            ├─ FAILED    → ✅ CANCELLED
            └─ CANCELLED → ✅ No change (idempotent)
```

**State Transitions & Outcome Summary:**

| Operation | Scenarios / Input                                        | Resulting Status  | HTTP Response                                          |
| --------- | -------------------------------------------------------- | ----------------- | ------------------------------------------------------ |
| **POST**  | Invalid Member / Product / Stock                         | **(Not Created)** | `400 Bad Request` or `404 Not Found`                   |
| **POST**  | External Service (Member/Product/Payment) Timeout or 503 | **(Not Created)** | `503 Service Unavailable`                              |
| **POST**  | All Valid + Payment **SUCCESS**                          | **CONFIRMED**     | `200 OK`                                               |
| **POST**  | All Valid + Payment **PENDING**                          | **PENDING**       | `200 OK`                                               |
| **POST**  | All Valid + Payment **FAILED**                           | **FAILED**        | `422 Unprocessable Entity` (or 200 with status=FAILED) |
| **PUT**   | Current Status = **PENDING** or **FAILED**               | **CANCELLED**     | `200 OK`                                               |
| **PUT**   | Current Status = **CONFIRMED**                           | **(Error)**       | `400 Bad Request` (Cannot cancel paid order)           |
| **PUT**   | Current Status = **CANCELLED**                           | **CANCELLED**     | `200 OK` (Idempotent)                                  |

**Handling External Service Errors:**

> ⚠️ **Important**: External services do not actually exist - need to mock in tests.

| Situation           | Handling                                                   |
| ------------------- | ---------------------------------------------------------- |
| Service unavailable | Return HTTP 503, log error, Order not created              |
| Timeout             | Return HTTP 503, configurable timeout (default 5s)         |
| Error response      | Map error → suitable HTTP status (see Error Mapping Table) |

---

### 6) Error Mapping Table

| External Service | Error                           | Order Service Response            | HTTP Status |
| ---------------- | ------------------------------- | --------------------------------- | ----------- |
| Member           | `MEMBER_NOT_FOUND`              | "Member does not exist"           | 404         |
| Member           | Status = INACTIVE/SUSPENDED     | "Member is not active"            | 400         |
| Product          | `PRODUCT_NOT_FOUND`             | "Product does not exist"          | 404         |
| Product          | Status = DISCONTINUED           | "Product is discontinued"         | 400         |
| Product          | `availableQuantity` < requested | "Insufficient stock"              | 400         |
| Payment          | `INVALID_PAYMENT_REQUEST`       | "Invalid payment data"            | 400         |
| Payment          | `PAYMENT_FAILED`                | "Payment rejected: {reason}"      | 422         |
| Any              | Timeout                         | "Service temporarily unavailable" | 503         |
| Any              | Connection refused              | "External service unavailable"    | 503         |

---

### 7) Where to focus?

In 4 hours, should focus on "point-scoring" areas according to rubric:

1.  **Core CRUD APIs of Order** + validate input + standard REST error handling.
2.  **Clear integration boundary** (ports/adapters or client layer) with Member/Product/Payment.
3.  **Minimum resilience** for external calls: timeout, controlled retry, meaningful error mapping, log correlation-id (or request-id). (Topic encourages circuit breaker/retry is a plus).
4.  **"Right place" testing strategy**
    - Unit test for domain/service logic + integration test with mock external (WireMock/Testcontainers mock server) fits very well.

---

### 8) What can be simplified?

To avoid over-engineering:

- **DB**: Choose Postgres, write docker compose for Postgres.
- **Inventory**: only do "pre-check stock" per API spec, do not try to simulate complex reserve/deduct (because spec does not support).
- **Payment async**: Can simplify by assuming payment always returns COMPLETED synchronously for MVP. PENDING flow is extension.
- **Observability**: standard log + structured fields (orderId, memberId, productId) is enough; metrics/tracing are "nice-to-have".

---

### 9) Assumptions (Consolidated)

> These assumptions need to be stated clearly from start to avoid being "caught" because requirements lack detail.

1.  **Minimal Order model**

    - Order includes: id, memberId, list items (productId, quantity, price snapshot), totalAmount, paymentMethod, status, timestamps.

2.  **Member validation rules**

    - Only `ACTIVE` members are allowed to create orders.
    - `INACTIVE` and `SUSPENDED` members rejected with clear message.
    - `grade` does not affect logic in this scope.

3.  **Product validation rules**

    - Product must be `AVAILABLE` and `availableQuantity >= requestedQuantity`.
    - `DISCONTINUED` products rejected even if stock exists.

4.  **When to call Payment?**

    - Payment is called **immediately in POST** after successful validation.
    - If Payment SUCCESS → Order status = **CONFIRMED**.
    - `paymentMethod` is provided in request body when creating order.

5.  **Stock reservation limitation**

    - Accept best-effort check, no real reservation.
    - Race condition can happen, log warning.

6.  **External services must be mocked**

    - No real endpoints → in local/dev/test will mock according to OpenAPI (WireMock/MockWebServer).

7.  **Idempotency**
    - Order Service implements guard against double payment itself by checking existing payment before calling.

---
