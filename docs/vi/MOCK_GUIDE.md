START APPLICATION WITH MOCK: external-services.mock: true in application.yml

## Mock Scenarios

### Member Service Mock

| Member ID | Kết quả            |
| --------- | ------------------ |
| `999`     | ❌ Not Found (404) |
| `998`     | ❌ INACTIVE (400)  |
| `997`     | ❌ SUSPENDED (400) |
| Others    | ✅ ACTIVE          |

### Product Service Mock

| Product ID | Kết quả                                        |
| ---------- | ---------------------------------------------- |
| `999`      | ❌ Not Found (404)                             |
| `998`      | ❌ DISCONTINUED (400)                          |
| `997`      | ❌ OUT_OF_STOCK (400)                          |
| `996`      | ❌ Insufficient Stock - only 2 available (400) |
| Others     | ✅ AVAILABLE with 950 stock                    |

### Payment Service Mock

Payment result dựa trên **totalAmount**:

| Total Amount                    | Payment Status | Order Status |
| ------------------------------- | -------------- | ------------ |
| Ending with `.99` (e.g., 29.99) | ❌ FAILED      | FAILED       |
| Ending with `.50` (e.g., 59.50) | ⏳ PENDING     | PENDING      |
| Others                          | ✅ COMPLETED   | CONFIRMED    |

## Test Examples

### ✅ Success Case (CONFIRMED)

```bash
POST /api/orders
{
  "memberId": 1,
  "items": [
    {"productId": 100, "quantity": 2}
  ],
  "paymentMethod": "CREDIT_CARD"
}
# Product price: 29.99 → Total: 59.98 → Payment COMPLETED → Order CONFIRMED
```

### ⏳ Pending Case

```bash
POST /api/orders
{
  "memberId": 1,
  "items": [
    {"productId": 100, "quantity": 1}
  ],
  "paymentMethod": "CREDIT_CARD"
}
# Product price: 29.99 → Total: 29.99 → Payment FAILED → Order FAILED
```

Để tạo PENDING, cần total amount ending với `.50`. Ví dụ: quantity = 2, price = 29.75 → total = 59.50

### ❌ Error Cases

```bash
# Member not found
POST /api/orders {"memberId": 999, ...}
→ 404 MEMBER_NOT_FOUND

# Member inactive
POST /api/orders {"memberId": 998, ...}
→ 400 MEMBER_NOT_ACTIVE

# Product not found
POST /api/orders {"memberId": 1, "items": [{"productId": 999, "quantity": 1}], ...}
→ 404 PRODUCT_NOT_FOUND

# Insufficient stock
POST /api/orders {"memberId": 1, "items": [{"productId": 996, "quantity": 10}], ...}
→ 400 INSUFFICIENT_STOCK
```

## Database

Mock profile sử dụng **H2 in-memory database**.

Access H2 Console:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:mockdb`
- Username: `sa`
- Password: (empty)

## Swagger UI

`http://localhost:8080/swagger-ui.html`
