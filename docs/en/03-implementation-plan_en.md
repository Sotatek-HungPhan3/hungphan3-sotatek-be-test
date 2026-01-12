# 03-implementation-plan.md

## Implementation Plan: Order Microservice (2 hours)

> **Tech Stack Confirmed**: PostgreSQL + Docker, RestClient, WireMock

---

## Phase 1: Setup (10 minutes)

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

Database configuration, external service URLs, timeout settings.

---

## Phase 2: Core (Domain + Application) - 50 minutes

### Domain Layer (`com.sotatek.order.domain`)

| File                 | Content                                                               |
| -------------------- | --------------------------------------------------------------------- |
| `Order.java`         | Aggregate: id, memberId, items, status, paymentId + state transitions |
| `OrderItem.java`     | Value object: productId, name, price, quantity                        |
| `OrderStatus.java`   | Enum: CONFIRMED, PENDING, FAILED, CANCELLED                           |
| `PaymentMethod.java` | Enum: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER                          |

### Application Layer (`com.sotatek.order.application`)

| File                      | Content                                     |
| ------------------------- | ------------------------------------------- |
| `CreateOrderUseCase.java` | Validate → Create → Payment → Update status |
| `GetOrderUseCase.java`    | Find by ID                                  |
| `ListOrdersUseCase.java`  | Pagination                                  |
| `CancelOrderUseCase.java` | Validate + update status                    |

---

## Phase 3: API + Infrastructure - 40 minutes

### Infrastructure Layer (`com.sotatek.order.infrastructure`)

| File                      | Content                                    |
| ------------------------- | ------------------------------------------ |
| `OrderEntity.java`        | JPA entity + @Version (optimistic locking) |
| `OrderJpaRepository.java` | Spring Data interface                      |
| `MemberClient.java`       | RestClient → Member Service                |
| `ProductClient.java`      | RestClient → Product Service               |
| `PaymentClient.java`      | RestClient → Payment Service               |

### API Layer (`com.sotatek.order.api`)

| File                          | Content                                |
| ----------------------------- | -------------------------------------- |
| `OrderController.java`        | 4 endpoints (POST, GET, GET list, PUT) |
| `CreateOrderRequest.java`     | memberId, items[], paymentMethod       |
| `OrderResponse.java`          | id, status, totalAmount, items, ...    |
| `GlobalExceptionHandler.java` | Error mapping according to FR-06       |

---

## Phase 4: Tests - 20 minutes

> See details at: [04-test-strategy.md](../vi/04-test-strategy.md)

| Test                           | Scope                                  |
| ------------------------------ | -------------------------------------- |
| `OrderTest.java`               | Domain state machine transitions       |
| `OrderApiIntegrationTest.java` | Happy path + error cases with WireMock |

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
# Run tests
./gradlew test

# Run app
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

| Phase                | Time        |
| -------------------- | ----------- |
| Setup                | 10 minutes  |
| Core (Domain + App)  | 50 minutes  |
| API + Infrastructure | 40 minutes  |
| Tests                | 20 minutes  |
| **Total**            | **2 hours** |

---

## AI Collaboration Notes

> Notes for interviewer regarding AI usage:

- **AI Support (Analysis)**: Analyze requirements, identify gaps
- **AI Support (Design)**: Separate layers according to Clean Architecture
- **AI Support (Implementation)**: Generate code skeleton
- **Human Decision**: Tech stack choices, trade-offs, business rules
