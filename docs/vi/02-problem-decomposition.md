# 02-problem-decomposition.md

## Problem Decomposition

### 0) Mục tiêu decomposition

- Tách hệ thống Order Service thành các **problem blocks** rõ ràng để:

  - Dễ thiết kế Clean Architecture (domain/app/infrastructure)
  - Dễ test theo từng block
  - Khoanh vùng rủi ro (external calls, state machine, idempotency)

---

## 1) Problem Blocks / Domains

### A. Order Management (Core Domain)

**Trách nhiệm**

- Quản lý vòng đời Order: create → validate → confirm → payment result → cancel
- Enforce state transitions (CONFIRMED, PENDING, FAILED, CANCELLED)
- Expose REST APIs theo FR-01 (4 endpoints: Create+Payment, Get, List, Cancel)

**Dữ liệu sở hữu**

- Order

  - id, memberId
  - items[]: (productId, productName snapshot, unitPrice snapshot, quantity, subtotal)
  - totalAmount
  - paymentMethod
  - status
  - paymentId (nullable)
  - timestamps

- Optimistic locking/version (để guard concurrent updates)

**Boundary**

- Public REST API boundary: `/api/orders/**`
- Internal boundary: Application use-cases (create/get/list/confirm/cancel/update)

**Dependency**

- Depends on: Validation (member/product), Payment Orchestration, Repository
- Must NOT depend trực tiếp vào HTTP client/DB (clean boundary)

**Classification:** **Core**

---

### B. Order Validation (Supporting Domain)

**Trách nhiệm**

- Validate business rules tại thời điểm CREATE (theo FR-02, FR-03)

  - Member exists + status ACTIVE
  - Product exists + status AVAILABLE
  - Stock availableQuantity >= requestedQty

- Convert external errors → domain validation errors

**Dữ liệu**

- Không sở hữu dữ liệu persistent
- Sử dụng DTO snapshot từ external:

  - Member(id,status,grade)
  - Product(id,status,price)
  - ProductStock(availableQuantity,reservedQuantity,quantity)

**Boundary**

- Input: Order draft (memberId, items)
- Output: ValidationResult (pass/fail + reasons)

**Dependency**

- Depends on external service clients (MemberClient, ProductClient) qua Ports
- Không gọi Payment

**Classification:** **Supporting** (vì phục vụ core order lifecycle)

---

### C. Payment Orchestration (Supporting Domain)

**Trách nhiệm**

- Khi tạo đơn hàng (POST /api/orders):

  - Sau khi validate thành công, gọi Payment Service ngay lập tức.
  - Cập nhật Order status dựa trên kết quả (CONFIRMED/PENDING/FAILED).

- Enforce idempotency guard:

  - Nếu order đã có paymentId hoặc status đã tiến xa → không create payment lần nữa

**Dữ liệu**

- Payment reference trong Order: paymentId
- Optional: PaymentSnapshot (payment status/transactionId) nếu lưu (tuỳ scope)

**Boundary**

- Input: orderId (hoặc Order aggregate)
- Output: updated Order state + paymentId

**Dependency**

- Depends on PaymentClient qua Port
- Depends on Order repository (để update atomically + optimistic lock)

**Classification:** **Supporting**

---

### D. Persistence & Query (Generic / Infrastructure)

**Trách nhiệm**

- Lưu trữ Order (CRUD DB)
- Pagination, sorting, filtering (status/memberId)
- Optimistic locking support

**Dữ liệu**

- Order tables/entities

**Boundary**

- Port: OrderRepository (save/find/list/update status)
- Adapter: JPA/Hibernate implementation

**Dependency**

- Depends on DB/JPA
- Used by Application layer

**Classification:** **Generic** (hạ tầng)

---

### E. External Service Integration (Generic / Infrastructure)

**Trách nhiệm**

- HTTP client calls tới:

  - Member Service
  - Product Service (+ stock)
  - Payment Service

- Timeout/retry mapping (ở mức client)
- Parse external ErrorResponse {code,message}
- Emit correlationId header (propagate)

**Dữ liệu**

- DTO theo OpenAPI specs (Member/Product/Stock/Payment)

**Boundary**

- Ports:

  - MemberClientPort.getMember(memberId)
  - ProductClientPort.getProduct(productId), getStock(productId)
  - PaymentClientPort.createPayment(request)

- Adapters: RestClient/WebClient + resilience

**Classification:** **Generic** (integration)

---

### F. Error Handling & Observability (Generic)

**Trách nhiệm**

- Global exception handler → chuẩn hóa ErrorResponse (code/message/timestamp/path)
- Mapping table external error → HTTP status (FR-06)
- Logging:

  - orderId/memberId/productId
  - correlationId/requestId

**Boundary**

- Cross-cutting: filters/interceptors/advice

**Classification:** **Generic**

---

## 2) Boundary & Dependency Map (tổng quan)

### Public Boundary (API)

- Controllers: OrderController

  - gọi Application Use Cases
  - không chứa business logic

### Application Boundary (Use Cases)

- CreateOrderUseCase (POST: validate + payment → CONFIRMED/PENDING/FAILED)
- GetOrderUseCase
- ListOrdersUseCase
- CancelOrderUseCase (PUT: cancel only)

### Domain Boundary (Core Rules)

- Order aggregate + state transitions rules
- Domain errors (InvalidTransition, ValidationFailed, PaymentRejected, etc.)

### Infrastructure Boundary

- Repository adapter (JPA)
- HTTP client adapters (Member/Product/Payment)
- Logging/correlation filter
- Mock server (WireMock) for tests

**Dependency rule (Clean Architecture)**

- Controller → Application → Domain
- Infrastructure implements Ports, injected into Application
- Domain không phụ thuộc Spring/JPA/HTTP

---

## 3) Dòng dữ liệu theo các flow chính

### Flow 1: Create Order (POST /api/orders)

1. API `POST /orders`
2. CreateOrderUseCase:
   - **Validate Member**: Nếu không tồn tại (404), inactive/suspended (400), hoặc service down (503) → Trả lỗi ngay, không tạo Order.
   - **Validate Product + Stock**: Nếu không tồn tại (404), discontinued (400), thiếu hàng (400), hoặc service down (503) → Trả lỗi ngay, không tạo Order.
   - **Tạo Order entity** với status tạm thời (để persistence).
   - **Gọi Payment Service (TỰ ĐỘNG)**:
     - Payment **SUCCESS** → Cập nhật Order = `CONFIRMED`.
     - Payment **PENDING** → Cập nhật Order = `PENDING`.
     - Payment **FAILED** → Cập nhật Order = `FAILED`.
     - Payment Service **Timeout/503** → Có thể rollback/xóa order tạm hoặc trả lỗi 503 (tùy thiết kế atomicity).
3. Trả về `OrderResponse` với status cuối cùng.

### Flow 2: Cancel Order (PUT /api/orders/{id})

1. API `PUT /orders/{id}` với body `{ "status": "CANCELLED" }`
2. CancelOrderUseCase:
   - **Check Hiện tại**:
     - Nếu là `CONFIRMED` → Trả lỗi 400 (Không thể hủy đơn đã thanh toán).
     - Nếu là `CANCELLED` → Không làm gì, trả về 200 OK (Idempotent).
     - Nếu là `PENDING` hoặc `FAILED` → Cập nhật status thành `CANCELLED`.
3. Trả về `OrderResponse`.

---

## 4) Core / Supporting / Generic (tóm tắt)

### Core

- Order Aggregate + State Machine
- Order CRUD + transitions enforcement

### Supporting

- OrderValidation (member/product/stock rules)
- PaymentOrchestration (confirm/payment mapping)

### Generic

- Persistence (JPA repository)
- External integrations (HTTP clients + DTO)
- Error handling & logging/correlation

---

## 5) “Đường ranh quan trọng” cần giữ để dễ làm & dễ chấm

- **Không để Controller gọi thẳng external services** → luôn qua Use Case
- **Không trộn logic payment vào repository layer**
- **State transition nằm trong Domain** (Order aggregate method)
- **External error mapping nằm ở integration/adapter + app layer**, trả domain error rõ ràng

---

### (AI hỗ trợ ở bước nào + nói với interviewer)

- **AI hỗ trợ (Decomposition):** dùng AI để bóc tách blocks theo Clean Architecture, xác định ownership dữ liệu + dependency boundaries để tránh “spaghetti service”.
- **Giải thích với interviewer:** “Em tách core order lifecycle khỏi validation/payment/integration để dễ test và giảm coupling; phần external được wrap bằng ports/adapters để mock/test dễ.”

---
