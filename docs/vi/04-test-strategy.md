# 04-test-strategy.md

## Test Strategy: Order Microservice

This document outlines the testing strategy for the Order Microservice, covering Unit, Integration, and End-to-End testing layers to ensure robustness and correctness.

### 1. Test Levels

| Level                   | Scope                                     | Tools                          | Focus                                                      |
| :---------------------- | :---------------------------------------- | :----------------------------- | :--------------------------------------------------------- |
| **Unit Testing**        | Domain Entities, Service Layer            | JUnit 5, Mockito               | Business logic, state transitions, validation rules.       |
| **Integration Testing** | Controllers, Repository, External Clients | Spring Boot Test, WireMock, H2 | API contract, DB persistence, external service resilience. |
| **E2E / API Testing**   | Full Application flow                     | RestClient (via .http files)   | Real usage scenarios, complete flows.                      |

### 2. Tools & Frameworks

- **JUnit 5**: Standard testing framework.
- **Mockito**: Mocking dependencies (Repositories, Client Ports).
- **Spring Boot Test**: Integration testing context (`@SpringBootTest`, `@WebMvcTest`).
- **WireMock**: Mocking external HTTP services (Member, Product, Payment).
- **H2 Database**: In-memory database for integration tests.

---

## 3. Test Case Matrix

### 3.1. Domain Logic (Unit Tests)

| ID        | Component   | Scenario                  | Expected Result                                              |
| :-------- | :---------- | :------------------------ | :----------------------------------------------------------- |
| **UT-01** | `Order`     | Create new order          | Status=PENDING/CONFIRMED (depends on payment), fields valid. |
| **UT-02** | `Order`     | Cancel PENDING order      | Status=CANCELLED.                                            |
| **UT-03** | `Order`     | Cancel FAILED order       | Status=CANCELLED.                                            |
| **UT-04** | `Order`     | Cancel CONFIRMED order    | `DomainException` (Cannot cancel confirmed order).           |
| **UT-05** | `Order`     | Cancel CANCELLED order    | `DomainException` (Already cancelled).                       |
| **UT-06** | `OrderItem` | Create valid item         | Object created.                                              |
| **UT-07** | `OrderItem` | Create item with qty <= 0 | `DomainException`.                                           |

### 3.2. Service Layer (Unit Tests)

| ID        | Component            | Scenario                                    | Expected Result                                 |
| :-------- | :------------------- | :------------------------------------------ | :---------------------------------------------- |
| **UT-08** | `CreateOrderService` | Valid creation (Success Payment)            | Order saved, Status=CONFIRMED.                  |
| **UT-09** | `CreateOrderService` | Valid creation (Pending Payment)            | Order saved, Status=PENDING.                    |
| **UT-10** | `CreateOrderService` | Member Validation Fails (404/Inactive)      | `MemberValidationException`, no order saved.    |
| **UT-11** | `CreateOrderService` | Product Validation Fails (404/Discontinued) | `ProductValidationException`, no order saved.   |
| **UT-12** | `CreateOrderService` | Stock Validation Fails (Insufficient)       | `ProductValidationException`, no order saved.   |
| **UT-13** | `CreateOrderService` | Payment Fails                               | Order saved (FAILED), `PaymentFailedException`. |
| **UT-14** | `CancelOrderService` | Cancel existing order                       | Order status updated to CANCELLED.              |
| **UT-15** | `CancelOrderService` | Cancel non-existent order                   | `OrderNotFoundException`.                       |

### 3.3. API Integration (Controller + WireMock)

| ID        | Component              | Scenario                            | Expected Result                         |
| :-------- | :--------------------- | :---------------------------------- | :-------------------------------------- |
| **IT-01** | `POST /api/orders`     | Success Flow (Mock Ext Services)    | 201 Created, JSON response.             |
| **IT-02** | `POST /api/orders`     | Invalid Request (Missing fields)    | 400 Bad Request.                        |
| **IT-03** | `POST /api/orders`     | External Service Error (Member 404) | 404 Not Found (GlobalExceptionHandler). |
| **IT-04** | `GET /api/orders/{id}` | Order Exists                        | 200 OK, Application JSON.               |
| **IT-05** | `GET /api/orders/{id}` | Order Not Found                     | 404 Not Found.                          |
| **IT-06** | `GET /api/orders`      | List with default pagination        | 200 OK, Page object.                    |
| **IT-07** | `PUT /api/orders/{id}` | Cancel Success                      | 200 OK, Status=CANCELLED.               |
| **IT-08** | `PUT /api/orders/{id}` | Cancel Invalid Status               | 400 Bad Request.                        |
| **IT-09** | `POST /api/orders`     | Member Service Timeout (Resilience) | 503 Service Unavailable / Safe Fail.    |
| **IT-10** | `POST /api/orders`     | Product Service Error (Resilience)  | 503 Service Unavailable / Safe Fail.    |

---

## 4. Implementation Plan for Tests

1.  **Generate Unit Tests**:
    - `OrderTest.java` (Domain)
    - `CreateOrderServiceTest.java` (Service - Mockito)
    - `CancelOrderServiceTest.java` (Service - Mockito)
2.  **Generate Integration Tests**:
    - `OrderControllerTest.java` (@WebMvcTest - lighter weight) OR
    - `OrderApiIntegrationTest.java` (@SpringBootTest + WireMock - full flow & resilience). _Preferred for interview demo._
