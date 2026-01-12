# Order Microservice Challenge

Welcome! This is a test for backend developers designed to evaluate your microservices building skills.

## Challenge

Your task is to build an **Order Service** - a microservice handling order management while integrating with external services (Member, Product, and Payment services).

**Time Limit**: 4 hours

Don't worry - we are not looking for perfection. We want to see how you approach the problem, structure source code, and handle real-world microservice scenarios.

---

## System Architecture

```
                                    ┌─────────────────┐
                                    │  Member Service │
                                    │    (External)   │
                                    └────────┬────────┘
                                             │
┌──────────┐      ┌─────────────────┐       │        ┌─────────────────┐
│  Client  │─────▶│  Order Service  │───────┼───────▶│ Product Service │
└──────────┘      │   (Your Task)   │       │        │    (External)   │
                  └────────┬────────┘       │        └─────────────────┘
                           │                │
                           │                │        ┌─────────────────┐
                           │                └───────▶│ Payment Service │
                           │                         │    (External)   │
                           ▼                         └─────────────────┘
                  ┌─────────────────┐
                  │    Database     │
                  │  (Your Choice)  │
                  └─────────────────┘
```

**Note**: External services (Member, Product, Payment) are only provided as OpenAPI specifications.
You will need to **mock these services** during implementation.

---

## Requirements

### Functional Requirements

Build REST APIs for Order management with the following operations:

| Operation      | Endpoint               | Description                      |
| -------------- | ---------------------- | -------------------------------- |
| Create Order   | `POST /api/orders`     | Create a new order               |
| Get Order Info | `GET /api/orders/{id}` | Get order details                |
| List Orders    | `GET /api/orders`      | List orders (with pagination)    |
| Update Order   | `PUT /api/orders/{id}` | Update order status/Cancel order |

### External Service Integration

When creating or processing an order, your service must:

1.  **Validate Member** - Call Member Service to verify member exists and is active.
2.  **Check Product** - Call Product Service to verify product availability and stock.
3.  **Process Payment** - Call Payment Service; if successful, update order status to confirmed.

### Non-functional Requirements

- Handle errors appropriately and provide meaningful error messages.
- Validate input data.
- Logging for debugging and monitoring.
- Unit tests and/or integration tests.

---

## Technology Stack

### Mandatory

- **Java**: 17 or higher
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle

### Your Choice

- Database (H2, PostgreSQL, MySQL, etc.)
- HTTP Client (RestTemplate, WebClient, Feign, etc.)
- Any additional libraries you find useful

---

## External Service Specifications

OpenAPI specifications for external services are located at:

```
docs/api-specs/
├── member-service.yaml    # Member validation API
├── product-service.yaml   # Product & inventory API
└── payment-service.yaml   # Payment processing API
```

**Important**: These services do not actually exist - you need to mock them in your tests and implementation. Consider how you handle:

- Service unavailability
- Timeout scenarios
- Error responses

---

## Submission Content

Create your own repository and include:

1.  **Source Code**

    - Clean code, well-structured
    - Clear package organization

2.  **Tests**

    - Unit tests for business logic
    - Integration tests (optional but highly appreciated)

3.  **Documentation**

    - API Documentation (Swagger/OpenAPI encouraged)
    - Concise README explaining your design decisions

4.  **How to Run**
    - Clear instructions to build and run your service
    - Any required installation steps

---

## Evaluation Criteria

We will review:

| Criteria            | What we look for                                         |
| ------------------- | -------------------------------------------------------- |
| **Code Quality**    | Clean code, readable, SOLID principles                   |
| **Architecture**    | Layer separation, dependency management, design patterns |
| **MSA Integration** | External service handling, error handling, resilience    |
| **Testing**         | Test coverage, test quality, mocking strategy            |
| **API Design**      | RESTful conventions, reliable HTTP status codes          |

### Bonus Points

These are optional but will make your submission stand out:

- Circuit Breaker pattern for external service calls
- Retry mechanism with exponential backoff
- Comprehensive logging and monitoring hooks
- Docker support
- Database migration scripts

---

## Getting Started

This repository provides a minimal Spring Boot application to get you started:

```bash
# Clone this repository for reference
git clone <this-repo-url>

# Check build
./gradlew build

# Run application
./gradlew bootRun
```

The application will start at `http://localhost:8080`

Now create your own repository and start building!

---

## Tips

- **Don't overcomplicate** - A working solution with clean code is better than an over-engineered but unfinished one.
- **Time management** - Prioritize core functionality first, then add improvements.
- **Show your thinking** - Comments and documentation help us understand your approach.
- **Test what matters** - Focus on testing critical business logic.

---

## Questions?

If you have any questions about the requirements, please contact your interviewer.

Good luck! We are excited to see what you build.
