# 05-deployment.md

## Deployment Strategy

> **Mục đích**: Tài liệu hóa cách thức đóng gói và triển khai hệ thống trong môi trường Production.

---

## 1. Containerization (Docker)

Hệ thống được đóng gói bằng Docker sử dụng **Multi-stage build** để tối ưu hóa kích thước image và tính bảo mật.

### Dockerfile structure:

- **Build Stage**: Sử dụng `gradle:8-jdk17-alpine` để build source code và tạo file shadow JAR (hoặc executable JAR).
- **Run Stage**: Sử dụng `eclipse-temurin:17-jre-alpine` chỉ chứa môi trường thực thi Java, giúp giảm bề mặt tấn công.

---

## 2. Orchestration (Docker Compose)

Sử dụng Docker Compose để quản lý các dịch vụ đi kèm:

- **order-service**: Application chính.
- **postgres**: Cơ sở dữ liệu lưu trữ orders.

Trong môi trường thực tế, dịch vụ này sẽ được triển khai trên **Kubernetes (K8s)** với các tài nguyên mong muốn (Deployment, Service, Ingress, ConfigMap, Secret).

---

## 3. CI/CD Pipeline (Khuyến nghị)

Một pipeline tiêu chuẩn cho dự án này nên bao gồm:

1. **Checkout**: Lấy code mới nhất từ repository.
2. **Build & Test**: Chạy `./gradlew test` để đảm bảo chất lượng.
3. **Static Analysis**: Sử dụng SonarQube để quét lỗi code và bảo mật.
4. **Dockerize**: Build Docker image và push lên Private Registry (ví dụ: Amazon ECR, Google Artifact Registry).
5. **Deploy**: Cập nhật container mới lên môi trường Staging/Production sử dụng mô hình **Blue-Green** hoặc **Canary Deployment** để giảm thiểu downtime.

---

## 4. Monitoring & Observability

- **Logging**: Logs được thu thập thông qua ELK Stack hoặc Splunk.
- **Metrics**: Sử dụng Micrometer kết hợp với Prometheus và Grafana để theo dõi performance (CPU, Memory, Request latency).
- **Tracing**: Tích hợp OpenTelemetry hoặc Zipkin để theo dõi các yêu cầu đi qua nhiều microservices.

---

## 5. Security Considerations

- **Non-root user**: Chạy ứng dụng Java trong container bằng user không có quyền root.
- **Secrets Management**: Các thông tin nhạy cảm (DB Password, API Keys) được quản lý qua K8s Secrets hoặc HashiCorp Vault thay vì để lộ trong file config.
- **Network Policy**: Chỉ cho phép traffic từ API Gateway hoặc các dịch vụ hợp lệ trong cluster tiếp cận Order Service.
