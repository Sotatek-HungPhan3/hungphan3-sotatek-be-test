# Order Microservice Submission

This is the implementation of the Order Service challenge for the Musinsa Senior Backend Engineer interview.

## 🚀 Overview

The Order Service manages the lifecycle of customer orders, integrating with external Member, Product, and Payment services. It is built with **Clean Architecture** principles to ensure maintainability, testability, and scalability.

## 🏗️ Architecture & Design Decisions

### Clean Architecture (Hexagonal)

The project is organized into layers to isolate business logic from infrastructure:

- **Domain Layer**: Contains Entities (`Order`, `OrderItem`), Enums, and Domain Services. It holds the core business rules and state machine (e.g., valid status transitions).
- **Application Layer**: Contains Use Cases (`CreateOrderService`, `CancelOrderService`) and Ports (interfaces for external communication).
- **API/Web Layer**: REST Controllers handling HTTP requests and DTO mapping.
- **Infrastructure Layer**: Implementation of Ports (Database repositories, HTTP clients for external MSA).

### Key Decisions:

1. **Domain-Driven Design (DDD) flavor**: The `Order` class is an Aggregate Root that protects its invariants (e.g., an order cannot be cancelled if it's already confirmed).
2. **Resilience**:
   - Configured **Read Timeouts** (5s) for all external service calls using `JdkClientHttpRequestFactory`.
   - **Graceful Error Handling**: Custom `GlobalExceptionHandler` maps domain/external errors to standard RESTful responses.
3. **External Service Mocking**:
   - Used `ConditionalOnProperty` to switch between `MockMemberClient` (standalone/dev) and `MemberClientAdapter` (production).
   - **WireMock** is used for rigorous integration testing of MSA scenarios (timeouts, 5xx errors).
4. **Consistency**: Transactions are managed at the Application layer (`@Transactional`) to ensure ACIDity during order creation and status updates.

## 📚 Design & Process Documentation

For a deeper dive into the engineering process, please refer to the documents in `docs/en`:

- **[Problem Understanding](docs/en/00-problem-understanding_en.md)**: Analysis of the requirements, bottlenecks, and assumptions.
- **[Requirements Analysis](docs/en/01-requirements_en.md)**: Detailed functional and non-functional requirements.
- **[System Design & Decomposition](docs/en/02-problem-decomposition_en.md)**: Domain breakdown and Clean Architecture boundaries.
- **[Implementation Plan](docs/en/03-implementation-plan_en.md)**: Step-by-step development plan.
- **[Test Strategy](docs/en/04-test-strategy_en.md)**: Comprehensive testing approach (Unit, Integration, WireMock).
- **[Deployment Guide](docs/en/05-deployment_en.md)**: Containerization and deployment strategy.
- **[Mock Guide](docs/en/MOCK_GUIDE_en.md)**: Values and scenarios for mocking external services (Member/Product/Payment).
- **[Original Requirements](docs/en/99-original-requirement_en.md)**: The original challenge text.

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.2**
- **Spring Data JPA**
- **Database**: PostgreSQL (Production), H2 (Testing)
- **SpringDoc OpenAPI** (Swagger UI)
- **WireMock** (Testing MSA integration)

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation at:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Docs (JSON)**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## 🏃 How to Run

### Prerequisites

- Docker (optional, for PostgreSQL)
- JDK 17+

### 1. Database Setup (Optional if using Docker Compose)

By default, the application is configured to use PostgreSQL. You can start one using Docker:

```bash
docker run --name order-db -e POSTGRES_DB=orderdb -e POSTGRES_USER=order -e POSTGRES_PASSWORD=order123 -p 5432:5432 -d postgres
```

### 2. Build & Run (Manual)

```bash
# Build the project
./gradlew build

# Run binary
java -jar build/libs/order-service-0.0.1-SNAPSHOT.jar

# Or run using Gradle
./gradlew bootRun
```

### 3. Run with Docker Compose (Recommended)

You can run the entire stack (Database + Application) with a single command:

```bash
docker-compose up --build
```

The application will be available at `http://localhost:8080`.

### 4. Run Tests

```bash
./gradlew test
```

## 🧪 Testing Strategy

- **Unit Tests**: Focus on the `Order` aggregate and business logic in the Domain layer.
- **Integration Tests**:
  - **`OrderApiIntegrationTest`**: End-to-end tests for all REST endpoints.
  - Uses **WireMock** to simulate external service behaviors (Member validation, Stock check, Payment processing).
  - Covers success paths, validation errors, and MSA failure scenarios (timeouts, 503).

---

_Developed as part of a technical assessment._
