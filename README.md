# Distributed E-Commerce Platform

A production-style **distributed e-commerce platform** built using **Java, Spring Boot, Kafka, PostgreSQL, Docker, and microservices architecture**.

The project demonstrates real-world backend concepts including **Saga Orchestration, Transactional Outbox, Event-Driven Architecture, Idempotency, Retry/DLT, Optimistic Locking, JWT Security, Circuit Breaker, Testing, Docker, CI/CD, and AWS deployment architecture**.

---

## 🚀 Project Overview

This project simulates an e-commerce order processing system where an order passes through multiple independent services.

A typical order follows this flow:

```text
Client
   |
   v
API Gateway
   |
   v
Order Service
   |
   | order.created
   v
Kafka
   |
   v
Saga Orchestrator
   |
   +------> Inventory Service
   |
   +------> Payment Service
   |
   +------> Shipping Service
   |
   +------> Notification Service
```

The system uses **Saga Orchestration** to maintain consistency across distributed services without using a distributed database transaction.

---

# 🏗️ Architecture

```text
                         Client
                           |
                           v
                    API Gateway :8080
                           |
          ┌────────────────┼────────────────┐
          |                |                |
          v                v                v
       Order          Inventory         Payment
      :8081             :8082             :8083
          |                |                |
          └────────────────┼────────────────┘
                           |
                           v
                      Apache Kafka
                        :9092
                           |
                           v
                 Saga Orchestrator :8090
                           |
              ┌────────────┼────────────┐
              |            |            |
              v            v            v
          Inventory      Payment     Shipping
                                      :8084
                                         |
                                         v
                                  Notification
                                     :8086
```

Authentication is handled by:

```text
Client
   |
   v
Auth Service :8085
   |
   v
JWT
   |
   v
API Gateway
   |
   v
Protected Microservices
```

---

# 🧩 Microservices

| Service              | Port | Responsibility                         |
| -------------------- | ---: | -------------------------------------- |
| API Gateway          | 8080 | Routing, JWT validation, authorization |
| Order Service        | 8081 | Order creation and order lifecycle     |
| Inventory Service    | 8082 | Product stock and reservations         |
| Payment Service      | 8083 | Payment processing                     |
| Shipping Service     | 8084 | Shipment creation and tracking         |
| Auth Service         | 8085 | Registration, login and JWT            |
| Notification Service | 8086 | Order notifications                    |
| Saga Orchestrator    | 8090 | Distributed transaction orchestration  |

---

# 🔄 Order Processing Flow

## Successful Order

```text
1. Client creates order
        |
        v
2. API Gateway validates JWT
        |
        v
3. Order Service creates PENDING order
        |
        v
4. Transactional Outbox stores event
        |
        v
5. Outbox Publisher publishes order.created
        |
        v
6. Saga Orchestrator receives event
        |
        v
7. inventory.reserve
        |
        v
8. inventory.reserved
        |
        v
9. payment.process
        |
        v
10. payment.success
        |
        v
11. shipping.create
        |
        v
12. shipping.created
        |
        v
13. order.confirm
        |
        v
14. Order becomes CONFIRMED
        |
        v
15. Notification Service sends notification
```

---

# ❌ Failure and Compensation

If payment fails:

```text
Order
  |
  v
Inventory Reserved
  |
  v
Payment Failed
  |
  v
inventory.release
  |
  v
Inventory Releases Stock
  |
  v
inventory.released
  |
  v
order.cancel
  |
  v
Order CANCELLED
```

This is implemented using the **Saga Pattern with compensating transactions**.

---

# 📨 Kafka Topics

The application uses Kafka for asynchronous communication.

```text
order.created

inventory.reserve
inventory.reserved
inventory.failed
inventory.release
inventory.released

payment.process
payment.success
payment.failed

shipping.create
shipping.created

order.confirm
order.cancel
```

Kafka provides asynchronous communication between services and helps reduce tight coupling.

---

# 🗄️ Database Architecture

The project follows the **Database-per-Service** pattern.

```text
Order Service
      |
      v
  order_db

Inventory Service
      |
      v
 inventory_db

Payment Service
      |
      v
 payment_db

Shipping Service
      |
      v
 shipping_db

Auth Service
      |
      v
 auth_db

Notification Service
      |
      v
 notification_db
```

A service does not directly modify another service's database.

---

# 🔐 Security

The project uses **Spring Security + JWT**.

Authentication flow:

```text
Username + Password
        |
        v
    Auth Service
        |
        v
      BCrypt
        |
        v
       JWT
        |
        v
    API Gateway
        |
        v
JWT Validation
        |
        v
Protected APIs
```

### Roles

```text
USER
ADMIN
```

Admin users have access to protected product management operations.

JWT secrets are externalized and are not stored in source code.

---

# 🛡️ Reliability Patterns

The project implements several production-oriented reliability patterns.

### Transactional Outbox

The business transaction and event creation are committed together.

```text
Database Transaction
       |
       +---- Order
       |
       +---- Outbox Event
```

This prevents the common problem where the database transaction succeeds but Kafka publishing fails.

---

### Retry

Failed event publishing is retried with increasing delay.

```text
Attempt 1
   |
   X
Attempt 2
   |
   X
Attempt 3
   |
   X
DLT
```

---

### Dead Letter Topic

Events that cannot be processed after retries are sent to a Dead Letter Topic.

```text
order.created
      |
      X
    Retry
      |
      X
    Retry
      |
      X
order.created.dlt
```

---

### Idempotency

Consumers are designed to safely handle duplicate events.

For example, inventory reservation uses the order ID to prevent the same order from reserving stock multiple times.

---

### Optimistic Locking

Inventory products use JPA optimistic locking:

```java
@Version
private Long version;
```

This helps prevent concurrent stock updates from silently overwriting each other.

---

### Circuit Breaker

Resilience4j can protect synchronous service communication.

```text
CLOSED
  |
  | failures
  v
OPEN
  |
  | timeout
  v
HALF_OPEN
```

This helps prevent cascading failures.

---

# 🧪 Testing

The project uses multiple testing strategies.

### Unit Testing

```text
JUnit 5
Mockito
```

Used for:

* Service logic
* Kafka consumers
* Kafka producers
* Saga logic
* Exception handling

### Integration Testing

```text
Spring Boot Test
Testcontainers
PostgreSQL
Kafka
```

Testcontainers provides real PostgreSQL and Kafka containers during integration tests.

Example:

```text
Test
 |
 +---- PostgreSQL Container
 |
 +---- Kafka Container
 |
 +---- Spring Boot Application
```

---

# 🐳 Docker

Every service can be containerized independently.

Example:

```text
Docker
│
├── api-gateway
├── order-service
├── inventory-service
├── payment-service
├── shipping-service
├── auth-service
├── notification-service
└── saga-orchestrator
```

Infrastructure:

```text
PostgreSQL
Kafka
```

can also be run using Docker Compose.

---

# 🐳 Run With Docker Compose

From the project root:

```bash
cd ~/Desktop/distrubuted-ecommerce
```

Build and start all services:

```bash
docker compose up -d --build
```

Check containers:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs --tail=50
```

Follow logs:

```bash
docker compose logs -f
```

Stop services:

```bash
docker compose down
```

---

# 🗄️ PostgreSQL

The local Docker environment uses PostgreSQL.

Databases:

```text
order_db
inventory_db
payment_db
shipping_db
auth_db
notification_db
```

Connect to PostgreSQL:

```bash
docker exec -it ecommerce-postgres psql -U postgres
```

List databases:

```sql
\l
```

Exit:

```sql
\q
```

---

# 📨 Kafka

Kafka runs on port:

```text
9092
```

List topics:

```bash
docker exec -it ecommerce-kafka \
/opt/kafka/bin/kafka-topics.sh \
--bootstrap-server localhost:9092 \
--list
```

---

# 📊 Monitoring

The application uses Spring Boot Actuator for application health and metrics.

Health endpoint:

```text
/actuator/health
```

Example:

```json
{
  "status": "UP"
}
```

The production architecture uses:

```text
Spring Boot Actuator
        |
        v
CloudWatch / Monitoring
```

Future observability improvements include distributed tracing and correlation IDs.

---

# ☁️ AWS Deployment Architecture

The project is designed to be deployed to AWS using:

```text
                    Internet
                       |
                       v
                 Route 53
                       |
                       v
            Application Load Balancer
                       |
                       v
                  API Gateway
                       |
                       v
                 ECS / Fargate
                       |
       ┌───────────────┼────────────────┐
       |               |                |
       v               v                v
    Order          Inventory         Payment
       |               |                |
       └───────────────┼────────────────┘
                       |
                       v
                  Amazon MSK
                     Kafka
                       |
                       v
               Saga Orchestrator
                       |
                       v
                 Amazon RDS
                  PostgreSQL
```

### AWS Services

| Requirement    | AWS Service     |
| -------------- | --------------- |
| Containers     | ECS / Fargate   |
| Docker Images  | ECR             |
| PostgreSQL     | RDS             |
| Kafka          | MSK             |
| Load Balancer  | ALB             |
| Secrets        | Secrets Manager |
| Logs           | CloudWatch      |
| DNS            | Route 53        |
| HTTPS          | ACM             |
| Access Control | IAM             |
| CI/CD          | GitHub Actions  |

---

# 🔄 CI/CD

The planned CI/CD pipeline:

```text
Developer
    |
    v
Git Push
    |
    v
GitHub
    |
    v
GitHub Actions
    |
    +---- Maven Build
    |
    +---- Unit Tests
    |
    +---- Integration Tests
    |
    +---- Docker Build
    |
    +---- Security Scan
    |
    v
Amazon ECR
    |
    v
Amazon ECS
    |
    v
Health Check
    |
    v
Production
```

---

# 📁 Project Structure

```text
distrubuted-ecommerce/
│
├── api-gateway/
│
├── order-service/
│
├── inventory-service/
│
├── payment-service/
│
├── shipping-service/
│
├── auth-service/
│
├── notification-service/
│
├── saga-orchestrator/
│
├── postgres/
│   └── init.sql
│
├── docs/
│   ├── architecture.md
│   ├── request-flow.md
│   ├── saga-pattern.md
│   ├── kafka.md
│   ├── resilience.md
│   ├── security.md
│   ├── testing.md
│   ├── ci-cd.md
│   └── aws-deployment.md
│
├── docker-compose.yml
├── .env
├── .gitignore
└── README.md
```

---

# 🛠️ Technology Stack

## Backend

```text
Java 17
Spring Boot
Spring Data JPA
Spring Security
Spring Cloud Gateway
Spring Kafka
Spring Boot Actuator
Resilience4j
```

## Database

```text
PostgreSQL
```

## Messaging

```text
Apache Kafka
```

## Security

```text
JWT
BCrypt
Spring Security
```

## Testing

```text
JUnit 5
Mockito
Testcontainers
```

## DevOps

```text
Docker
Docker Compose
GitHub Actions
AWS ECS
AWS ECR
AWS RDS
AWS MSK
CloudWatch
```

---

# 📚 Design Patterns and Concepts

This project demonstrates:

* Microservices Architecture
* Event-Driven Architecture
* Saga Orchestration
* Transactional Outbox
* Eventual Consistency
* Idempotency
* Retry Pattern
* Dead Letter Topic
* Circuit Breaker
* Optimistic Locking
* Database-per-Service
* API Gateway Pattern
* JWT Authentication
* Role-Based Authorization
* Containerization
* CI/CD
* Cloud Deployment

---

# 🔍 Important Design Decisions

## Why Microservices?

Each business capability can be developed, deployed and scaled independently.

---

## Why Kafka?

Kafka provides asynchronous, reliable and scalable communication between services.

---

## Why Saga?

A distributed transaction cannot rely on a single database transaction.

Saga breaks the business transaction into smaller local transactions with compensation for failures.

---

## Why Transactional Outbox?

It prevents the database update and event publication from becoming inconsistent.

---

## Why Database-per-Service?

It maintains service ownership and reduces coupling between services.

---

## Why API Gateway?

It provides a single entry point for clients and centralizes:

* Routing
* JWT validation
* Authorization
* Cross-cutting concerns

---

# 📖 Documentation

Detailed documentation is available in the `docs/` directory.

| Document            | Description                         |
| ------------------- | ----------------------------------- |
| `architecture.md`   | Overall system architecture         |
| `request-flow.md`   | Complete request/order flow         |
| `saga-pattern.md`   | Saga orchestration and compensation |
| `kafka.md`          | Kafka architecture and topics       |
| `resilience.md`     | Retry and Circuit Breaker           |
| `security.md`       | JWT and Spring Security             |
| `testing.md`        | JUnit, Mockito and Testcontainers   |
| `ci-cd.md`          | CI/CD pipeline                      |
| `aws-deployment.md` | AWS deployment architecture         |

---

# 🚀 Quick Start

## 1. Clone the repository

```bash
git clone <your-repository-url>
```

```bash
cd distrubuted-ecommerce
```

## 2. Configure environment

Create a `.env` file:

```env
JWT_SECRET=your-secure-secret
```

Never commit `.env` to Git.

---

## 3. Build services

Build each Spring Boot service:

```bash
./mvnw clean package
```

---

## 4. Start infrastructure and services

```bash
docker compose up -d --build
```

---

## 5. Verify services

```bash
docker compose ps
```

Check the gateway:

```text
http://localhost:8080
```

Check Actuator:

```text
http://localhost:8081/actuator/health
```

---

# 🧪 Running Tests

Run all tests:

```bash
./mvnw test
```

Run tests for a specific service:

```bash
cd order-service
./mvnw test
```

Integration tests use Testcontainers where configured.

---

# 📌 Example API Flow

### Register

```http
POST /api/auth/register
```

### Login

```http
POST /api/auth/login
```

Response:

```json
{
  "token": "JWT_TOKEN"
}
```

Use the token:

```http
Authorization: Bearer JWT_TOKEN
```

### Create Order

```http
POST /api/orders
```

Example request:

```json
{
  "customerId": 101,
  "productId": 1,
  "quantity": 2,
  "totalAmount": 79999
}
```

The request then enters the Saga workflow.

---

# 🎯 Project Goals

The primary goals of this project are to demonstrate practical knowledge of:

```text
Java Backend Development
        +
Spring Boot
        +
Microservices
        +
Kafka
        +
Distributed Transactions
        +
Security
        +
Testing
        +
Docker
        +
CI/CD
        +
AWS
```

The project is intentionally designed around real-world distributed-system problems rather than only CRUD operations.

---

# 🔮 Future Enhancements

Planned improvements:

* Distributed tracing
* Correlation IDs
* OpenTelemetry
* Swagger / OpenAPI
* Redis caching
* Performance testing
* Load testing
* Terraform
* AWS production deployment
* API versioning
* Contract testing
* Blue/Green deployment
* Canary deployment
* Kubernetes / EKS
* Advanced observability

---

# 👨‍💻 Author

**Vaibhav Gobre**

Software Developer | Java | Spring Boot | Microservices

Interested in:

```text
Java Backend Development
Spring Boot
Microservices
Distributed Systems
Kafka
Cloud & AWS
System Design
```

---

# ⭐ Key Interview Takeaway

This project demonstrates a complete distributed order-processing system.

The most important architectural flow is:

```text
Client
  ↓
API Gateway
  ↓
Order Service
  ↓
Transactional Outbox
  ↓
Kafka
  ↓
Saga Orchestrator
  ↓
Inventory
  ↓
Payment
  ↓
Shipping
  ↓
Order Confirmation
  ↓
Notification
```

If any step fails, the Saga executes a compensating action to maintain business consistency.

The application is containerized with Docker, tested using JUnit/Mockito/Testcontainers, protected using JWT/Spring Security, designed for CI/CD, and prepared for AWS deployment using ECS, ECR, RDS and MSK.
