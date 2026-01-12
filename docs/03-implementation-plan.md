# 03-implementation-plan.md

## Implementation Plan: Order Microservice (2 giờ)

> **Tech Stack Confirmed**: PostgreSQL + Docker, RestClient, WireMock

---

## Phase 1: Setup (10 phút)

### [MODIFY] build.gradle

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'org.postgresql:postgresql'
    runtimeOnly 'com.h2database:h2'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.wiremock:wiremock-standalone:3.3.1'
}
```

### [NEW] docker-compose.yml

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: orderdb
      POSTGRES_USER: order
      POSTGRES_PASSWORD: order123
    ports:
      - "5432:5432"
```

### [NEW] application.yml

Cấu hình database, external service URLs, timeout settings.

---

## Phase 2: Core (Domain + Application) - 50 phút

### Domain Layer (`com.sotatek.order.domain`)

| File                 | Nội dung                                                              |
| -------------------- | --------------------------------------------------------------------- |
| `Order.java`         | Aggregate: id, memberId, items, status, paymentId + state transitions |
| `OrderItem.java`     | Value object: productId, name, price, quantity                        |
| `OrderStatus.java`   | Enum: CONFIRMED, PENDING, FAILED, CANCELLED                           |
| `PaymentMethod.java` | Enum: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER                          |

### Application Layer (`com.sotatek.order.application`)

| File                      | Nội dung                                    |
| ------------------------- | ------------------------------------------- |
| `CreateOrderUseCase.java` | Validate → Create → Payment → Update status |
| `GetOrderUseCase.java`    | Find by ID                                  |
| `ListOrdersUseCase.java`  | Pagination                                  |
| `CancelOrderUseCase.java` | Validate + update status                    |

---

## Phase 3: API + Infrastructure - 40 phút

### Infrastructure Layer (`com.sotatek.order.infrastructure`)

| File                      | Nội dung                                   |
| ------------------------- | ------------------------------------------ |
| `OrderEntity.java`        | JPA entity + @Version (optimistic locking) |
| `OrderJpaRepository.java` | Spring Data interface                      |
| `MemberClient.java`       | RestClient → Member Service                |
| `ProductClient.java`      | RestClient → Product Service               |
| `PaymentClient.java`      | RestClient → Payment Service               |

### API Layer (`com.sotatek.order.api`)

| File                          | Nội dung                               |
| ----------------------------- | -------------------------------------- |
| `OrderController.java`        | 4 endpoints (POST, GET, GET list, PUT) |
| `CreateOrderRequest.java`     | memberId, items[], paymentMethod       |
| `OrderResponse.java`          | id, status, totalAmount, items, ...    |
| `GlobalExceptionHandler.java` | Error mapping theo FR-06               |

---

## Phase 4: Tests - 20 phút

| Test                           | Scope                                 |
| ------------------------------ | ------------------------------------- |
| `OrderTest.java`               | Domain state machine transitions      |
| `OrderApiIntegrationTest.java` | Happy path + error cases với WireMock |

---

## Package Structure

```
src/main/java/com/sotatek/order/
├── domain/
│   ├── model/          # Order, OrderItem, OrderStatus, PaymentMethod
│   └── exception/      # Domain exceptions
├── application/
│   ├── port/
│   │   ├── in/         # Use case interfaces
│   │   └── out/        # Repository & client ports
│   ├── service/        # Use case implementations
│   └── dto/            # External service DTOs
├── infrastructure/
│   ├── persistence/    # JPA entities, adapters
│   └── client/         # HTTP client adapters
├── api/
│   ├── controller/     # REST controllers
│   ├── dto/            # API request/response DTOs
│   └── exception/      # Global exception handler
└── config/             # Spring configurations
```

---

## Verification

```bash
# Chạy tests
./gradlew test

# Chạy app
docker-compose up -d
./gradlew bootRun

# Swagger UI
http://localhost:8080/swagger-ui.html
```

### Priority Test Cases

| #   | Scenario                           | Expected  |
| --- | ---------------------------------- | --------- |
| 1   | POST /api/orders valid → CONFIRMED | 200 OK    |
| 2   | GET /api/orders/{id}               | 200 OK    |
| 3   | GET /api/orders (pagination)       | 200 OK    |
| 4   | PUT cancel PENDING order           | 200 OK    |
| 5   | Invalid member/product             | 400/404   |
| 6   | Cancel CONFIRMED order             | 400 Error |

---

## Timeline

| Phase                | Thời gian |
| -------------------- | --------- |
| Setup                | 10 phút   |
| Core (Domain + App)  | 50 phút   |
| API + Infrastructure | 40 phút   |
| Tests                | 20 phút   |
| **Total**            | **2 giờ** |

---

## AI Collaboration Notes

> Ghi chú cho interviewer về việc sử dụng AI:

- **AI hỗ trợ Analysis**: Phân tích requirements, xác định gaps
- **AI hỗ trợ Design**: Tách layers theo Clean Architecture
- **AI hỗ trợ Implementation**: Generate code skeleton
- **Human Decision**: Tech stack choices, trade-offs, business rules
