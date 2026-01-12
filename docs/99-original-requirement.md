# Thử thách Order Microservice

Chào mừng! Đây là bài kiểm tra dành cho lập trình viên backend được thiết kế để đánh giá kỹ năng xây dựng microservices của bạn.

## Thử thách

Nhiệm vụ của bạn là xây dựng một **Order Service** - một microservice xử lý việc quản lý đơn hàng đồng thời tích hợp với các dịch vụ bên ngoài (Member, Product, và Payment services).

**Giới hạn thời gian**: 4 giờ

Đừng lo lắng - chúng tôi không tìm kiếm sự hoàn hảo. Chúng tôi muốn xem cách bạn tiếp cận vấn đề, cấu trúc mã nguồn và xử lý các kịch bản microservice trong thực tế.

---

## Kiến trúc hệ thống

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

**Lưu ý**: Các dịch vụ bên ngoài (Member, Product, Payment) chỉ được cung cấp dưới dạng đặc tả OpenAPI.
Bạn sẽ cần **mock (giả lập) các dịch vụ này** trong quá trình triển khai.

---

## Yêu cầu

### Yêu cầu chức năng

Xây dựng các REST API cho quản lý Đơn hàng với các thao tác sau:

| Thao tác               | Endpoint               | Mô tả                                 |
| ---------------------- | ---------------------- | ------------------------------------- |
| Tạo đơn hàng           | `POST /api/orders`     | Tạo một đơn hàng mới                  |
| Lấy thông tin đơn hàng | `GET /api/orders/{id}` | Lấy chi tiết đơn hàng                 |
| Danh sách đơn hàng     | `GET /api/orders`      | Liệt kê các đơn hàng (có phân trang)  |
| Cập nhật đơn hàng      | `PUT /api/orders/{id}` | Cập nhật status đơn hàng/Hủy đơn hàng |

### Tích hợp dịch vụ bên ngoài

Khi tạo hoặc xử lý một đơn hàng, dịch vụ của bạn phải:

1. **Xác thực thành viên (Validate Member)** - Gọi Member Service để xác minh thành viên tồn tại và đang hoạt động.
2. **Kiểm tra sản phẩm (Check Product)** - Gọi Product Service để xác minh tính khả dụng của sản phẩm và tồn kho.
3. **Xử lý thanh toán (Process Payment)** - Gọi Payment Service nếu thành công thì update status đơn hàng thành xác nhận.

### Yêu cầu phi chức năng

- Xử lý lỗi phù hợp và thông báo lỗi có ý nghĩa.
- Validate dữ liệu đầu vào.
- Logging để debug và giám sát.
- Unit tests và/hoặc integration tests.

---

## Công nghệ sử dụng

### Bắt buộc

- **Java**: 17 hoặc cao hơn
- **Framework**: Spring Boot 3.x
- **Công cụ Build**: Gradle

### Tùy chọn của bạn

- Cơ sở dữ liệu (H2, PostgreSQL, MySQL, v.v.)
- HTTP Client (RestTemplate, WebClient, Feign, v.v.)
- Bất kỳ thư viện bổ sung nào bạn thấy hữu ích

---

## Đặc tả dịch vụ bên ngoài

Các đặc tả OpenAPI cho các dịch vụ bên ngoài nằm ở:

```
docs/api-specs/
├── member-service.yaml    # Member validation API
├── product-service.yaml   # Product & inventory API
└── payment-service.yaml   # Payment processing API
```

**Quan trọng**: Các dịch vụ này không thực sự tồn tại - bạn cần mock chúng trong các bài test và triển khai của mình. Hãy cân nhắc cách bạn xử lý:

- Dịch vụ không khả dụng
- Các kịch bản timeout
- Phản hồi lỗi

---

## Nội dung nộp bài

Tạo repository riêng của bạn và bao gồm:

1. **Mã nguồn (Source Code)**

   - Code sạch, cấu trúc tốt
   - Tổ chức package rõ ràng

2. **Tests**

   - Unit tests cho logic nghiệp vụ
   - Integration tests (tùy chọn nhưng được đánh giá cao)

3. **Tài liệu (Documentation)**

   - Tài liệu API (khuyến khích dùng Swagger/OpenAPI)
   - README ngắn gọn giải thích các quyết định thiết kế của bạn

4. **Cách chạy (How to Run)**
   - Hướng dẫn rõ ràng để build và chạy dịch vụ của bạn
   - Bất kỳ bước cài đặt nào được yêu cầu

---

## Tiêu chí đánh giá

Chúng tôi sẽ xem xét:

| Tiêu chí            | Điều chúng tôi tìm kiếm                                              |
| ------------------- | -------------------------------------------------------------------- |
| **Chất lượng code** | Code sạch, dễ đọc, nguyên lý SOLID                                   |
| **Kiến trúc**       | Phân tách lớp, quản lý dependency, mẫu thiết kế (design patterns)    |
| **Tích hợp MSA**    | Xử lý dịch vụ bên ngoài, xử lý lỗi, khả năng phục hồi (resilience)   |
| **Testing**         | Độ bao phủ test (test coverage), chất lượng test, chiến lược mocking |
| **Thiết kế API**    | Quy ước RESTful, mã trạng thái HTTP tin cậy                          |

### Điểm cộng

Những điều này là tùy chọn nhưng sẽ làm bài nộp của bạn nổi bật:

- Mẫu Circuit Breaker cho các cuộc gọi dịch vụ bên ngoài
- Cơ chế thử lại (Retry mechanism) với exponential backoff
- Các hook logging và monitoring toàn diện
- Hỗ trợ Docker
- Script migration cơ sở dữ liệu

---

## Bắt đầu

Repository này cung cấp một ứng dụng Spring Boot tối thiểu để bạn bắt đầu:

```bash
# Clone repository này để tham khảo
git clone <this-repo-url>

# Kiểm tra việc build
./gradlew build

# Chạy ứng dụng
./gradlew bootRun
```

Ứng dụng sẽ khởi chạy tại `http://localhost:8080`

Bây giờ hãy tạo repository riêng của bạn và bắt đầu xây dựng!

---

## Mẹo

- **Đừng suy nghĩ quá phức tạp** - Một giải pháp hoạt động với code sạch sẽ tốt hơn một giải pháp quá kỹ thuật nhưng chưa hoàn thành.
- **Quản lý thời gian** - Ưu tiên chức năng cốt lõi trước, sau đó thêm các cải tiến.
- **Thể hiện tư duy của bạn** - Comment và tài liệu giúp chúng tôi hiểu cách tiếp cận của bạn.
- **Test những gì quan trọng** - Tập trung vào test các logic nghiệp vụ quan trọng.

---

## Câu hỏi?

Nếu bạn có bất kỳ câu hỏi nào về các yêu cầu, vui lòng liên hệ với người phỏng vấn của bạn.

Chúc may mắn! Chúng tôi rất hào hứng để xem những gì bạn xây dựng.
