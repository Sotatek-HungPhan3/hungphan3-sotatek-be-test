# 01-requirements.md

## Requirement Analysis

> **This document defines WHAT the system must do** (functional & non-functional).
> Based on: README.md + OpenAPI specs + assumptions from `00-problem-understanding.md`

---

## 1. Functional Requirements

### FR-01: Order REST APIs

| API                    | Description                                     |
| ---------------------- | ----------------------------------------------- |
| `POST /api/orders`     | Create order (validate + payment automatically) |
| `GET /api/orders/{id}` | Get order details                               |
| `GET /api/orders`      | List orders (pagination)                        |
| `PUT /api/orders/{id}` | Cancel Order                                    |

---

### FR-02: Create Order Flow (POST /api/orders)

**Request:**

```json
{
  "memberId": "int64 (required)",
  "items": [{ "productId": "int64", "quantity": "int > 0" }],
  "paymentMethod": "CREDIT_CARD | DEBIT_CARD | BANK_TRANSFER"
}
```

**Flow:**

```
1. Validate Member → ACTIVE?
2. Validate Product → AVAILABLE? Stock sufficient?
3. Call Payment Service
4. Result:
   - Payment SUCCESS → CONFIRMED
   - Payment PENDING → PENDING
   - Payment FAILED  → FAILED
```

**Response:** Order object with corresponding status.

---

### FR-03: Cancel Order (PUT /api/orders/{id})

**Request:** `{ "status": "CANCELLED" }`

| Current Status | Cancel? | Result                 |
| -------------- | ------- | ---------------------- |
| CONFIRMED      | ❌ No   | Error: Already paid    |
| PENDING        | ⚠️ Yes  | → CANCELLED            |
| FAILED         | ✅ Yes  | → CANCELLED            |
| CANCELLED      | ✅      | No change (idempotent) |

---

### FR-04: Order Status (only 4 statuses)

| Status      | Description                  |
| ----------- | ---------------------------- |
| `CONFIRMED` | Payment successful           |
| `PENDING`   | Payment processing           |
| `FAILED`    | Validation or Payment failed |
| `CANCELLED` | User cancelled order         |

---

### FR-05: Validation Rules

**Member:**

- Must exist (`GET /api/members/{id}`)
- Must have status = `ACTIVE`

**Product:**

- Must exist (`GET /api/products/{id}`)
- Must have status = `AVAILABLE`
- `availableQuantity >= requestedQuantity`

---

### FR-06: Error Handling

| Error                | HTTP | Message                      | Description                                |
| -------------------- | ---- | ---------------------------- | ------------------------------------------ |
| Member not found     | 404  | Member does not exist        | memberId not found                         |
| Member not active    | 400  | Member is not active         | Member status INACTIVE/SUSPENDED           |
| Product not found    | 404  | Product does not exist       | productId not found                        |
| Product discontinued | 400  | Product is discontinued      | Product status DISCONTINUED                |
| Insufficient stock   | 400  | Not enough stock             | Stock < requested quantity                 |
| Payment failed       | 422  | Payment rejected             | Error from Payment Service (business fail) |
| Service timeout      | 503  | External service timeout     | Timeout when calling external service      |
| Service unavailable  | 503  | External service unavailable | Service down or connection refused         |
| Invalid transition   | 400  | Invalid state transition     | Example: Cancel CONFIRMED order            |

---

## 2. Non-Functional Requirements

| NFR                 | Description                                                 |
| ------------------- | ----------------------------------------------------------- |
| **Reliability**     | External failure does not crash app, returns graceful error |
| **Maintainability** | Clean Architecture, SOLID principles                        |
| **Testability**     | Unit test + Integration test with mock                      |
| **Observability**   | Logging with orderId, memberId, correlationId               |
| **Performance**     | Timeout configurable (default 5s)                           |

---

## 3. Constraints

- Java 17+, Spring Boot 3.x, Gradle
- External services = mock only (WireMock)
- Time: ~4 hours

---

## 4. Out of Scope

- Real Stock reservation
- Distributed transaction / Saga
- Async payment callback
- Refund API
- Auth/AuthZ
- Discount/promotion

---
