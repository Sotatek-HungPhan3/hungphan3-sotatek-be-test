# 05-deployment.md

## Deployment Strategy

> **Purpose**: Document how to package and deploy the system in Production environment.

---

## 1. Containerization (Docker)

The system is packaged using Docker with **Multi-stage build** to optimize image size and security.

### Dockerfile structure:

- **Build Stage**: Uses `gradle:8-jdk17-alpine` to build source code and create shadow JAR (or executable JAR).
- **Run Stage**: Uses `eclipse-temurin:17-jre-alpine` containing only Java runtime environment, reducing attack surface.

---

## 2. Orchestration (Docker Compose)

Uses Docker Compose to manage accompanying services:

- **order-service**: Main Application.
- **postgres**: Database for storing orders.

In a real environment, this service would be deployed on **Kubernetes (K8s)** with desired resources (Deployment, Service, Ingress, ConfigMap, Secret).

---

## 3. CI/CD Pipeline (Recommended)

A standard pipeline for this project should include:

1.  **Checkout**: Get latest code from repository.
2.  **Build & Test**: Run `./gradlew test` to ensure quality.
3.  **Static Analysis**: Use SonarQube to scan for code errors and security issues.
4.  **Dockerize**: Build Docker image and push to Private Registry (e.g., Amazon ECR, Google Artifact Registry).
5.  **Deploy**: Update new container to Staging/Production environment using **Blue-Green** or **Canary Deployment** model to minimize downtime.

---

## 4. Monitoring & Observability

- **Logging**: Logs collected via ELK Stack or Splunk.
- **Metrics**: Use Micrometer combined with Prometheus and Grafana to track performance (CPU, Memory, Request latency).
- **Tracing**: Integrate OpenTelemetry or Zipkin to track requests passing through multiple microservices.

---

## 5. Security Considerations

- **Non-root user**: Run Java application in container with non-root user.
- **Secrets Management**: Sensitive info (DB Password, API Keys) managed via K8s Secrets or HashiCorp Vault instead of exposing in config files.
- **Network Policy**: Only allow traffic from API Gateway or valid services in cluster to access Order Service.
