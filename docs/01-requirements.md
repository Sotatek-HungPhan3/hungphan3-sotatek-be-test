# 01-requirements.md

## Requirement Analysis

> **Tài liệu này xác định WHAT hệ thống phải làm** (functional & non-functional).  
> Dựa trên: README.md + OpenAPI specs + assumptions từ `00-problem-understanding.md`

---

## 1. Functional Requirements

### FR-01: Order REST APIs

| API                    | Mô tả                                  |
| ---------------------- | -------------------------------------- |
| `POST /api/orders`     | Tạo order (validate + payment tự động) |
| `GET /api/orders/{id}` | Lấy chi tiết order                     |
| `GET /api/orders`      | Danh sách orders (pagination)          |
| `PUT /api/orders/{id}` | Cancel Order                           |

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
2. Validate Product → AVAILABLE? Stock đủ?
3. Gọi Payment Service
4. Kết quả:
   - Payment SUCCESS → CONFIRMED
   - Payment PENDING → PENDING
   - Payment FAILED  → FAILED
```

**Response:** Order object với status tương ứng.

---

### FR-03: Cancel Order (PUT /api/orders/{id})

**Request:** `{ "status": "CANCELLED" }`

| Current Status | Cancel? | Kết quả                |
| -------------- | ------- | ---------------------- |
| CONFIRMED      | ❌ No   | Error: Đã thanh toán   |
| PENDING        | ⚠️ Yes  | → CANCELLED            |
| FAILED         | ✅ Yes  | → CANCELLED            |
| CANCELLED      | ✅      | No change (idempotent) |

---

### FR-04: Order Status (chỉ 4 trạng thái)

| Status      | Mô tả                            |
| ----------- | -------------------------------- |
| `CONFIRMED` | Payment thành công               |
| `PENDING`   | Payment đang xử lý               |
| `FAILED`    | Validation hoặc Payment thất bại |
| `CANCELLED` | User hủy đơn                     |

---

### FR-05: Validation Rules

**Member:**

- Phải tồn tại (`GET /api/members/{id}`)
- Phải có status = `ACTIVE`

**Product:**

- Phải tồn tại (`GET /api/products/{id}`)
- Phải có status = `AVAILABLE`
- `availableQuantity >= requestedQuantity`

---

### FR-06: Error Handling

| Lỗi                  | HTTP | Message                      | Mô tả                                  |
| -------------------- | ---- | ---------------------------- | -------------------------------------- |
| Member not found     | 404  | Member does not exist        | Không tìm thấy memberId                |
| Member not active    | 400  | Member is not active         | Member status INACTIVE/SUSPENDED       |
| Product not found    | 404  | Product does not exist       | Không tìm thấy productId               |
| Product discontinued | 400  | Product is discontinued      | Product status DISCONTINUED            |
| Insufficient stock   | 400  | Not enough stock             | Stock < quantity yêu cầu               |
| Payment failed       | 422  | Payment rejected             | Lỗi từ Payment Service (business fail) |
| Service timeout      | 503  | External service timeout     | Timeout khi gọi external service       |
| Service unavailable  | 503  | External service unavailable | Service down hoặc connection refused   |
| Invalid transition   | 400  | Invalid state transition     | Ví dụ: Cancel đơn đã CONFIRMED         |

---

## 2. Non-Functional Requirements

| NFR                 | Mô tả                                              |
| ------------------- | -------------------------------------------------- |
| **Reliability**     | External failure không crash app, trả lỗi graceful |
| **Maintainability** | Clean Architecture, SOLID principles               |
| **Testability**     | Unit test + Integration test với mock              |
| **Observability**   | Logging với orderId, memberId, correlationId       |
| **Performance**     | Timeout configurable (default 5s)                  |

---

## 3. Constraints

- Java 17+, Spring Boot 3.x, Gradle
- External services = mock only (WireMock)
- Thời gian: ~4 giờ

---

## 4. Out of Scope

- Stock reservation thật
- Distributed transaction / Saga
- Async payment callback
- Refund API
- Auth/AuthZ
- Discount/promotion

---
