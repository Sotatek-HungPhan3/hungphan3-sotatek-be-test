# 02-problem-decomposition.md

## Problem Decomposition

### 0) Decomposition Goals

- Separate Order Service system into clear **problem blocks** to:

  - Facilitate Clean Architecture design (domain/app/infrastructure)
  - Facilitate testing by block
  - Isolate risks (external calls, state machine, idempotency)

---

## 1) Problem Blocks / Domains

### A. Order Management (Core Domain)

**Responsibilities**

- Manage Order lifecycle: create → validate → confirm → payment result → cancel
- Enforce state transitions (CONFIRMED, PENDING, FAILED, CANCELLED)
- Expose REST APIs according to FR-01 (4 endpoints: Create+Payment, Get, List, Cancel)

**Owned Data**

- Order

  - id, memberId
  - items[]: (productId, productName snapshot, unitPrice snapshot, quantity, subtotal)
  - totalAmount
  - paymentMethod
  - status
  - paymentId (nullable)
  - timestamps

- Optimistic locking/version (to guard concurrent updates)

**Boundary**

- Public REST API boundary: `/api/orders/**`
- Internal boundary: Application use-cases (create/get/list/confirm/cancel/update)

**Dependency**

- Depends on: Validation (member/product), Payment Orchestration, Repository
- Must NOT depend directly on HTTP client/DB (clean boundary)

**Classification:** **Core**

---

### B. Order Validation (Supporting Domain)

**Responsibilities**

- Validate business rules at CREATE time (according to FR-02, FR-03)

  - Member exists + status ACTIVE
  - Product exists + status AVAILABLE
  - Stock availableQuantity >= requestedQty

- Convert external errors → domain validation errors

**Data**

- Does not own persistent data
- Uses DTO snapshot from external:

  - Member(id,status,grade)
  - Product(id,status,price)
  - ProductStock(availableQuantity,reservedQuantity,quantity)

**Boundary**

- Input: Order draft (memberId, items)
- Output: ValidationResult (pass/fail + reasons)

**Dependency**

- Depends on external service clients (MemberClient, ProductClient) via Ports
- Does not call Payment

**Classification:** **Supporting** (because it serves core order lifecycle)

---

### C. Payment Orchestration (Supporting Domain)

**Responsibilities**

- When creating order (POST /api/orders):

  - After successful validation, call Payment Service immediately.
  - Update Order status based on result (CONFIRMED/PENDING/FAILED).

- Enforce idempotency guard:

  - If order already has paymentId or status has progressed → do not create payment again

**Data**

- Payment reference in Order: paymentId
- Optional: PaymentSnapshot (payment status/transactionId) if saved (depending on scope)

**Boundary**

- Input: orderId (or Order aggregate)
- Output: updated Order state + paymentId

**Dependency**

- Depends on PaymentClient via Port
- Depends on Order repository (to update atomically + optimistic lock)

**Classification:** **Supporting**

---

### D. Persistence & Query (Generic / Infrastructure)

**Responsibilities**

- Store Order (CRUD DB)
- Pagination, sorting, filtering (status/memberId)
- Optimistic locking support

**Data**

- Order tables/entities

**Boundary**

- Port: OrderRepository (save/find/list/update status)
- Adapter: JPA/Hibernate implementation

**Dependency**

- Depends on DB/JPA
- Used by Application layer

**Classification:** **Generic** (infrastructure)

---

### E. External Service Integration (Generic / Infrastructure)

**Responsibilities**

- HTTP client calls to:

  - Member Service
  - Product Service (+ stock)
  - Payment Service

- Timeout/retry mapping (at client level)
- Parse external ErrorResponse {code,message}
- Emit correlationId header (propagate)

**Data**

- DTO according to OpenAPI specs (Member/Product/Stock/Payment)

**Boundary**

- Ports:

  - MemberClientPort.getMember(memberId)
  - ProductClientPort.getProduct(productId), getStock(productId)
  - PaymentClientPort.createPayment(request)

- Adapters: RestClient/WebClient + resilience

**Classification:** **Generic** (integration)

---

### F. Error Handling & Observability (Generic)

**Responsibilities**

- Global exception handler → standardize ErrorResponse (code/message/timestamp/path)
- Mapping table external error → HTTP status (FR-06)
- Logging:

  - orderId/memberId/productId
  - correlationId/requestId

**Boundary**

- Cross-cutting: filters/interceptors/advice

**Classification:** **Generic**

---

## 2) Boundary & Dependency Map (Overview)

### Public Boundary (API)

- Controllers: OrderController

  - call Application Use Cases
  - contain no business logic

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
- Domain does not depend on Spring/JPA/HTTP

---

## 3) Data flow according to main flows

### Flow 1: Create Order (POST /api/orders)

1. API `POST /orders`
2. CreateOrderUseCase:
   - **Validate Member**: If not exists (404), inactive/suspended (400), or service down (503) → Return error immediately, Order not created.
   - **Validate Product + Stock**: If not exists (404), discontinued (400), insufficient (400), or service down (503) → Return error immediately, Order not created.
   - **Create Order entity** with temporary status (for persistence).
   - **Call Payment Service (AUTOMATICALLY)**:
     - Payment **SUCCESS** → Update Order = `CONFIRMED`.
     - Payment **PENDING** → Update Order = `PENDING`.
     - Payment **FAILED** → Update Order = `FAILED`.
     - Payment Service **Timeout/503** → Can rollback/delete temporary order or return error 503 (depending on atomicity design).
3. Return `OrderResponse` with final status.

### Flow 2: Cancel Order (PUT /api/orders/{id})

1. API `PUT /orders/{id}` with body `{ "status": "CANCELLED" }`
2. CancelOrderUseCase:
   - **Check Current**:
     - If `CONFIRMED` → Return error 400 (Cannot cancel paid order).
     - If `CANCELLED` → Do nothing, return 200 OK (Idempotent).
     - If `PENDING` or `FAILED` → Update status to `CANCELLED`.
3. Return `OrderResponse`.

---

## 4) Core / Supporting / Generic (Summary)

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

## 5) "Critical Boundaries" to keep for ease of implementation & grading

- **Do not let Controller call external services directly** → always through Use Case
- **Do not mix payment logic into repository layer**
- **State transition lies in Domain** (Order aggregate method)
- **External error mapping lies in integration/adapter + app layer**, return clear domain error

---

### (AI support at which step + tell interviewer)

- **AI Support (Decomposition):** used AI to decompose blocks according to Clean Architecture, identify data ownership + dependency boundaries to avoid "spaghetti service".
- **Explain to interviewer:** "I separated core order lifecycle from validation/payment/integration for easier testing and reduced coupling; external parts are wrapped by ports/adapters for easy mocking/testing."

---
