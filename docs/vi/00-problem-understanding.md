# 00-problem-understanding.md

## Problem Understanding & Bottleneck Discovery

### 1) Bản chất bài toán backend là gì?

- Xây dựng **Order Service** (Spring Boot 3.x, Java 17+, Gradle) cung cấp REST APIs cho đơn hàng:
  `POST /api/orders` (tạo + validate + payment), `GET /api/orders/{id}`, `GET /api/orders`, `PUT /api/orders/{id}` (cancel only).
- Order Service phải **tích hợp** 3 dịch vụ external (chỉ có OpenAPI, không tồn tại thật → phải mock):

  - **Member Service**: validate member tồn tại & active trước khi tạo đơn. Endpoint: `GET /api/members/{memberId}`.
  - **Product Service**: lấy thông tin sản phẩm và kiểm tra tồn kho. Endpoints: `GET /api/products/{productId}`, `GET /api/products/{productId}/stock`.
  - **Payment Service**: xử lý thanh toán khi order "confirmed/ready". Endpoint: `POST /api/payments` (và có `GET /api/payments/{paymentId}` để tra cứu).

- Bài test đánh giá **tư duy production trong microservices**: tách lớp, quản lý dependency, xử lý lỗi, resilience, test strategy, docs.

---

### 2) Giá trị hệ thống cần bảo vệ?

**Các "assets" quan trọng cần bảo vệ (production mindset):**

1. **Tính đúng đắn nghiệp vụ Order**

   - Không tạo order cho member không hợp lệ/không active.
   - Không tạo/confirm order khi sản phẩm không tồn tại hoặc không đủ tồn kho.

2. **Tính nhất quán trạng thái giữa Order ↔ Payment**

   - Payment chỉ gọi khi order đã ở trạng thái phù hợp ("confirmed/ready").

3. **Độ tin cậy khi phụ thuộc external services**

   - External service có thể timeout/không khả dụng/ trả lỗi → Order service vẫn phải xử lý có kiểm soát, trả lỗi rõ ràng, log đủ.

4. **Auditability & debuggability**

   - Logging + error message "có ý nghĩa" để truy vết, quan sát.

---

### 3) Chi tiết External Services (từ OpenAPI Specs)

#### 3.1) Member Service

```yaml
Member:
  id: int64
  name: string
  email: string (email format)
  status: enum [ACTIVE, INACTIVE, SUSPENDED] # ⚠️ Cần handle cả 3 status
  grade: enum [BRONZE, SILVER, GOLD, PLATINUM] # 🔹 Extension point cho tương lai

ErrorResponse: { code: "MEMBER_NOT_FOUND", message: "..." }
```

**Business Rules cần define:**
| Status | Cho phép tạo Order? | Lý do |
|--------|---------------------|-------|
| `ACTIVE` | ✅ Có | Member hoạt động bình thường |
| `INACTIVE` | ❌ Không | Member đã ngưng hoạt động |
| `SUSPENDED` | ❌ Không | Member bị đình chỉ (vi phạm chính sách) |

**Assumption**: `grade` (BRONZE/SILVER/GOLD/PLATINUM) không ảnh hưởng business logic order trong scope này. Có thể mở rộng để áp dụng discount/priority sau.

---

#### 3.2) Product Service

```yaml
Product:
  id: int64
  name: string
  price: double
  status: enum [AVAILABLE, OUT_OF_STOCK, DISCONTINUED] # ⚠️ Quan trọng!

ProductStock:
  productId: int64
  quantity: int # Tổng số trong kho
  reservedQuantity: int # Đã reserve cho pending orders
  availableQuantity: int # = quantity - reservedQuantity, có thể order
```

**Business Rules cần define:**
| Product Status | Stock Check | Cho phép Order? |
|----------------|-------------|-----------------|
| `AVAILABLE` | `availableQuantity >= requestedQty` | ✅ Có |
| `AVAILABLE` | `availableQuantity < requestedQty` | ❌ Không (insufficient stock) |
| `OUT_OF_STOCK` | Bất kỳ | ❌ Không (trừ khi back-order) |
| `DISCONTINUED` | Bất kỳ | ❌ Không (sản phẩm đã ngưng) |

**⚠️ Design Gap - Stock Reservation:**

- Spec có trả về `reservedQuantity` → External Product Service **có internal reservation mechanism**.
- **NHƯNG** không expose endpoint để Order Service gọi reserve/deduct stock.
- **Accepted Risk**: Race condition khi concurrent orders cùng check stock OK → cả hai tạo → oversell.
- **Mitigation trong scope này**: Best-effort check before order creation. Ghi log warning về limitation.

---

#### 3.3) Payment Service

```yaml
PaymentRequest: # ← Request body khi gọi POST /api/payments
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

**Business Decisions cần xác định:**

1. **PaymentMethod từ đâu?** → Order entity phải có field `paymentMethod` hoặc nhận từ request khi confirm.
2. **Handle 4 Payment statuses:**
   - `COMPLETED` → Order chuyển sang COMPLETED
   - `PENDING` → Order chờ ở trạng thái PAYMENT_PENDING (async processing)
   - `FAILED` → Order rollback về CONFIRMED hoặc PAYMENT_FAILED
   - `REFUNDED` → Trigger từ cancel order flow

**Error Handling:**
| HTTP Status | Error Code | Retry? | Action |
|-------------|------------|--------|--------|
| 400 | `INVALID_PAYMENT_REQUEST` | ❌ No | Fix request data |
| 422 | `PAYMENT_FAILED` | 🔄 Maybe | Có thể thử phương thức khác |

---

### 4) Bottleneck / Rủi ro chính

1. **Distributed failure & latency** (điểm rủi ro lớn nhất)

   - Order Service phụ thuộc 3 external calls (Member/Product/Payment). Khi 1 dịch vụ chậm hoặc down → dễ gây timeouts, cascade failure.

2. **Race Conditions cụ thể:**

   - **Concurrent order creation**: 2 orders cùng check stock OK → cả 2 tạo → oversell
   - **Double submit confirm**: User click 2 lần → 2 payment requests cho cùng order
   - **Order update while payment processing**: PUT order trong khi payment đang PENDING

3. **Idempotency & double payment**

   - Nếu retry payment hoặc client gọi lại → có thể tạo nhiều payments cho cùng order nếu không có guard.
   - Spec payment không có idempotency key → Order Service cần tự implement guard (check if payment already exists for order).

4. **Thiết kế flow nghiệp vụ khi create/update/confirm**

   - Requirement nói "khi tạo hoặc xử lý order" phải validate/check/payment, nhưng không định nghĩa chi tiết state machine.

---

### 5) Order State Machine (Proposed)

```
POST /api/orders
      │
      ├─1. Validate Member (GET /api/members/{id})
      │     ├─ Member không tồn tại ───────────────→ 404 (Order không tạo)
      │     ├─ Member INACTIVE/SUSPENDED ──────────→ 400 (Order không tạo)
      │     ├─ Member Service timeout/unavailable ─→ 503 (Order không tạo)
      │     └─ Member ACTIVE ✅ → tiếp tục
      │
      ├─2. Validate Product + Stock (GET /api/products/{id}, /stock)
      │     ├─ Product không tồn tại ──────────────→ 404 (Order không tạo)
      │     ├─ Product DISCONTINUED ───────────────→ 400 (Order không tạo)
      │     ├─ Insufficient stock ─────────────────→ 400 (Order không tạo)
      │     ├─ Product Service timeout/unavailable → 503 (Order không tạo)
      │     └─ Product OK + Stock OK ✅ → tiếp tục
      │
      ├─3. Gọi Payment Service (POST /api/payments)
      │     ├─ Payment SUCCESS ────────────────────→ Order = CONFIRMED ✅
      │     ├─ Payment PENDING ────────────────────→ Order = PENDING ⏳
      │     ├─ Payment FAILED ─────────────────────→ Order = FAILED ❌
      │     └─ Payment Service timeout/unavailable → 503 (Order không tạo)
      │
      └─ Response: OrderResponse với status tương ứng


PUT /api/orders/{id}
      │
      └─ Cancel Order
            ├─ CONFIRMED → ❌ Error (đã thanh toán)
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

**Xử lý External Service Errors:**

> ⚠️ **Quan trọng**: Các dịch vụ external không thực sự tồn tại - cần mock trong tests.

| Tình huống             | Xử lý                                                     |
| ---------------------- | --------------------------------------------------------- |
| Dịch vụ không khả dụng | Trả HTTP 503, log error, Order không được tạo             |
| Timeout                | Trả HTTP 503, configurable timeout (default 5s)           |
| Phản hồi lỗi           | Map error → HTTP status phù hợp (xem Error Mapping Table) |

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

### 7) Phần nào cần focus?

Trong 4 giờ, nên focus vào các "điểm ăn điểm" theo rubric:

1. **Core CRUD APIs của Order** + validate input + error handling REST chuẩn.
2. **Integration boundary rõ ràng** (ports/adapters hoặc client layer) với Member/Product/Payment.
3. **Resilience tối thiểu** cho external calls: timeout, retry có kiểm soát, mapping lỗi meaningful, log correlation-id (hoặc request-id). (Đề khuyến khích circuit breaker/retry là điểm cộng).
4. **Testing strategy "đúng chỗ"**
   - Unit test cho domain/service logic + integration test với mock external (WireMock/Testcontainers mock server) là rất hợp bài.

---

### 8) Phần nào có thể simplify?

Để tránh over-engineering:

- **DB**: Chọn Postgres, viết docker compose cho Postgres.
- **Inventory**: chỉ làm "pre-check stock" theo API spec, không cố mô phỏng reserve/deduct phức tạp (vì spec không hỗ trợ).
- **Payment async**: Có thể simplify bằng cách assume payment luôn trả COMPLETED synchronously cho MVP. PENDING flow là extension.
- **Observability**: log chuẩn + structured fields (orderId, memberId, productId) là đủ; metrics/tracing là "nice-to-have".

---

### 9) Assumptions (Consolidated)

> Các giả định này cần ghi rõ ngay từ đầu để tránh bị "bắt bẻ" vì requirement thiếu chi tiết.

1. **Order model tối thiểu**

   - Order gồm: id, memberId, danh sách items (productId, quantity, price snapshot), totalAmount, paymentMethod, status, timestamps.

2. **Member validation rules**

   - Chỉ `ACTIVE` members được phép tạo order.
   - `INACTIVE` và `SUSPENDED` members bị từ chối với message rõ ràng.
   - `grade` không ảnh hưởng logic trong scope này.

3. **Product validation rules**

   - Product phải `AVAILABLE` và `availableQuantity >= requestedQuantity`.
   - `DISCONTINUED` products bị từ chối dù còn stock.

4. **Khi nào gọi Payment?**

   - Payment được gọi **ngay trong POST** sau khi validate thành công.
   - Nếu Payment SUCCESS → Order status = **CONFIRMED**.
   - `paymentMethod` được cung cấp trong request body khi tạo order.

5. **Stock reservation limitation**

   - Chấp nhận best-effort check, không có real reservation.
   - Race condition có thể xảy ra, ghi log warning.

6. **External services phải mock**

   - Không có endpoint thực → trong local/dev/test sẽ mock theo OpenAPI (WireMock/MockWebServer).

7. **Idempotency**
   - Order Service tự implement guard chống double payment bằng cách check existing payment trước khi gọi.

---
