# Distributed E-Commerce Platform — Architecture

## 1. Overview

The Distributed E-Commerce Platform is a microservices-based backend system designed to handle the complete lifecycle of an e-commerce order.

The system follows an **event-driven architecture** using Apache Kafka and uses the **Saga Orchestration Pattern** to maintain consistency across multiple microservices.

The platform supports:

* User registration and authentication
* JWT-based authorization
* Product and inventory management
* Order creation
* Inventory reservation
* Payment processing
* Shipping creation
* Order confirmation and cancellation
* Event-driven communication
* Distributed transaction management
* Compensation and rollback
* Transactional Outbox
* Retry and Dead Letter Topic handling
* Idempotent event processing
* Optimistic locking for inventory
* Global exception handling
* Automated unit and integration testing
* PostgreSQL databases
* Docker-based deployment
* Monitoring and health checks

---

# 2. High-Level Architecture

```text
                         ┌───────────────────┐
                         │      Client       │
                         │ Postman / Frontend│
                         └─────────┬─────────┘
                                   │
                                   ▼
                         ┌───────────────────┐
                         │    API Gateway    │
                         │      :8080        │
                         │                   │
                         │ JWT Validation    │
                         │ Routing           │
                         │ Authorization     │
                         └─────────┬─────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
       ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
       │    Auth     │      │    Order    │      │  Inventory  │
       │   :8085     │      │   :8081     │      │   :8082     │
       └──────┬──────┘      └──────┬──────┘      └──────┬──────┘
              │                    │                    │
              ▼                    ▼                    ▼
       ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
       │   auth_db   │      │   order_db  │      │inventory_db │
       └─────────────┘      └─────────────┘      └─────────────┘


                         ┌──────────────────────┐
                         │        Kafka         │
                         │       :9092          │
                         │                      │
                         │ Event-driven         │
                         │ communication        │
                         └──────────┬───────────┘
                                    │
                    ┌───────────────┼────────────────┐
                    │               │                │
                    ▼               ▼                ▼
             ┌────────────┐  ┌────────────┐  ┌────────────┐
             │  Payment   │  │  Shipping  │  │   Saga     │
             │   :8083    │  │   :8084    │  │Orchestrator│
             └─────┬──────┘  └─────┬──────┘  │   :8090    │
                   │               │          └────────────┘
                   ▼               ▼
            ┌────────────┐  ┌────────────┐
            │ payment_db │  │shipping_db │
            └────────────┘  └────────────┘

                         ┌───────────────┐
                         │ Notification  │
                         │    :8086      │
                         └───────┬───────┘
                                 ▼
                         ┌───────────────┐
                         │notification_db│
                         └───────────────┘
```

---

# 3. Microservices

The system is divided into independent services.

| Service              | Port | Database        | Responsibility                         |
| -------------------- | ---: | --------------- | -------------------------------------- |
| API Gateway          | 8080 | —               | Routing, JWT validation, authorization |
| Order Service        | 8081 | order_db        | Order creation and lifecycle           |
| Inventory Service    | 8082 | inventory_db    | Product stock and reservations         |
| Payment Service      | 8083 | payment_db      | Payment processing                     |
| Shipping Service     | 8084 | shipping_db     | Shipment creation                      |
| Auth Service         | 8085 | auth_db         | Registration, login and JWT            |
| Notification Service | 8086 | notification_db | Order notifications                    |
| Saga Orchestrator    | 8090 | —               | Distributed transaction coordination   |
| Kafka                | 9092 | —               | Event communication                    |
| PostgreSQL           | 5432 | Multiple DBs    | Persistent storage                     |

---

# 4. Service Responsibilities

## 4.1 API Gateway

The API Gateway is the single entry point for external clients.

Responsibilities:

* Route requests to appropriate microservices
* Validate JWT tokens
* Check user roles
* Protect internal services from direct external access
* Provide a centralized entry point
* Handle cross-service routing

Example:

```text
POST /api/orders
        │
        ▼
API Gateway :8080
        │
        ▼
Order Service :8081
```

The client does not need to know the internal service addresses.

---

# 4.2 Auth Service

The Auth Service handles authentication and authorization-related operations.

Responsibilities:

* User registration
* User login
* Password hashing using BCrypt
* Role assignment
* JWT generation
* User lookup

Example:

```text
POST /api/auth/register
POST /api/auth/login
```

After successful login, the service returns a JWT token.

The client then sends:

```text
Authorization: Bearer <JWT_TOKEN>
```

for protected requests.

---

# 4.3 Order Service

The Order Service manages the order lifecycle.

Responsibilities:

* Create orders
* Retrieve orders
* Maintain order status
* Store order information
* Create transactional outbox events
* Consume order confirmation events
* Consume order cancellation events

Order states:

```text
PENDING
   │
   ├──────────────► CONFIRMED
   │
   └──────────────► CANCELLED
```

When an order is created, its initial status is:

```text
PENDING
```

---

# 4.4 Inventory Service

The Inventory Service manages product stock.

Responsibilities:

* Product creation
* Product retrieval
* Stock management
* Inventory reservation
* Inventory release
* Idempotency
* Optimistic locking

Example:

```text
Product
----------------
id
name
price
stock
version
```

The `version` field is used for optimistic locking.

This prevents concurrent transactions from incorrectly overwriting stock updates.

---

# 4.5 Payment Service

The Payment Service processes payments.

Responsibilities:

* Receive payment requests
* Process payment
* Maintain payment status
* Publish payment success/failure events

Payment states:

```text
PENDING
   │
   ├──────► SUCCESS
   │
   └──────► FAILED
```

In the current project, payment processing is simulated so that both successful and failure scenarios can be tested.

---

# 4.6 Shipping Service

The Shipping Service handles shipment creation.

Responsibilities:

* Create shipment
* Maintain shipping status
* Store shipping information
* Publish shipping-created events

Shipping states:

```text
PENDING
   │
   ▼
CREATED
   │
   ▼
SHIPPED
   │
   ▼
DELIVERED
```

---

# 4.7 Notification Service

The Notification Service handles order-related notifications.

It listens for events such as:

```text
order.confirm
order.cancel
```

and creates notification records.

Example:

```text
Order 101
   │
   ▼
Order Confirmed
   │
   ▼
Notification Service
   │
   ▼
Notification stored
```

---

# 4.8 Saga Orchestrator

The Saga Orchestrator coordinates the distributed transaction.

It does not directly perform database operations for other services.

Instead, it:

1. Receives events
2. Determines the next operation
3. Publishes commands/events
4. Handles failures
5. Starts compensation when required

Example:

```text
Order Created
      ↓
Reserve Inventory
      ↓
Process Payment
      ↓
Create Shipping
      ↓
Confirm Order
```

---

# 5. Database Architecture

The system follows the **Database-per-Service** pattern.

Each service owns its own database.

```text
Order Service       → order_db
Inventory Service   → inventory_db
Payment Service     → payment_db
Shipping Service    → shipping_db
Auth Service        → auth_db
Notification        → notification_db
```

Services should not directly query another service's database.

For example:

```text
Order Service ❌ → inventory_db
Order Service ❌ → payment_db

Order Service ✅ → Kafka → Inventory Service
```

This provides service isolation and makes each microservice independently maintainable.

---

# 6. Event-Driven Communication

The services communicate asynchronously through Apache Kafka.

Instead of tightly coupling services:

```text
Order → HTTP → Inventory → HTTP → Payment
```

the system uses events:

```text
Order
  │
  ▼
Kafka
  │
  ▼
Saga Orchestrator
  │
  ▼
Kafka
  │
  ▼
Inventory
```

This reduces direct dependency between services.

---

# 7. Kafka Topics

The major Kafka topics are:

```text
order.created
order.created.dlt

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

### Event flow

```text
order.created
      ↓
inventory.reserve
      ↓
inventory.reserved
      ↓
payment.process
      ↓
payment.success
      ↓
shipping.create
      ↓
shipping.created
      ↓
order.confirm
```

---

# 8. Complete Successful Order Flow

Consider:

```text
Order ID: 101
Product ID: 10
Quantity: 2
```

## Step 1 — Client creates order

```text
Client
  │
  │ POST /api/orders
  ▼
API Gateway
  │
  ▼
Order Service
```

Order Service creates:

```text
Order 101
Status = PENDING
```

---

## Step 2 — Transactional Outbox

Instead of directly publishing to Kafka during the database transaction, the Order Service stores an outbox event.

```text
orders
   │
   └── transaction ──► outbox_events
```

Example:

```text
event_type    = OrderCreated
aggregate_id  = 101
status        = PENDING
```

The Outbox Publisher later publishes the event to:

```text
order.created
```

---

## Step 3 — Saga receives event

Saga Orchestrator receives:

```text
order.created
```

and publishes:

```text
inventory.reserve
```

---

## Step 4 — Inventory reservation

Inventory Service:

```text
Check product
     ↓
Check stock
     ↓
Decrease stock
     ↓
Create reservation
```

Example:

```text
Stock before = 10
Quantity     = 2

Stock after  = 8
```

Then it publishes:

```text
inventory.reserved
```

---

## Step 5 — Payment

Saga receives:

```text
inventory.reserved
```

and publishes:

```text
payment.process
```

Payment Service processes the payment.

If successful:

```text
payment.success
```

---

## Step 6 — Shipping

Saga receives:

```text
payment.success
```

and publishes:

```text
shipping.create
```

Shipping Service creates the shipment and publishes:

```text
shipping.created
```

---

## Step 7 — Order confirmation

Saga receives:

```text
shipping.created
```

and publishes:

```text
order.confirm
```

Order Service updates:

```text
PENDING → CONFIRMED
```

---

# 9. Complete Successful Flow

```text
                    Client
                      │
                      ▼
                API Gateway
                      │
                      ▼
                Order Service
                      │
                      ▼
               Outbox Event
                      │
                      ▼
                order.created
                      │
                      ▼
              Saga Orchestrator
                      │
                      ▼
             inventory.reserve
                      │
                      ▼
             Inventory Service
                      │
                      ▼
            inventory.reserved
                      │
                      ▼
              Saga Orchestrator
                      │
                      ▼
              payment.process
                      │
                      ▼
              Payment Service
                      │
                      ▼
               payment.success
                      │
                      ▼
              Saga Orchestrator
                      │
                      ▼
              shipping.create
                      │
                      ▼
              Shipping Service
                      │
                      ▼
              shipping.created
                      │
                      ▼
              Saga Orchestrator
                      │
                      ▼
                order.confirm
                      │
                      ▼
                Order Service
                      │
                      ▼
               Order CONFIRMED
```

---

# 10. Failure and Compensation Flow

A distributed transaction can fail at any stage.

For example:

```text
Order Created
      ↓
Inventory Reserved
      ↓
Payment FAILED
```

At this point, inventory has already been reserved.

The system must compensate for the completed operation.

Saga publishes:

```text
inventory.release
```

Inventory releases the stock.

Then:

```text
inventory.released
```

is published.

Saga then publishes:

```text
order.cancel
```

Order Service changes:

```text
PENDING → CANCELLED
```

---

# 11. Compensation Flow

```text
Order Created
      ↓
Inventory Reserved
      ↓
Payment Failed
      ↓
Inventory Release
      ↓
Inventory Released
      ↓
Order Cancel
      ↓
Order CANCELLED
```

This is the key reason the Saga Pattern is used.

---

# 12. Transactional Outbox

The Transactional Outbox Pattern solves the dual-write problem.

Without Outbox:

```text
Database Save
     │
     ├── SUCCESS
     │
     ▼
Kafka Publish
     │
     └── FAILURE ❌
```

The database says the order exists, but Kafka never receives the event.

This creates an inconsistent system.

With Outbox:

```text
Database Transaction
       │
       ├── Save Order
       │
       └── Save Outbox Event
              │
              ▼
          COMMIT
              │
              ▼
       Outbox Publisher
              │
              ▼
            Kafka
```

The order and its event are committed in the same database transaction.

---

# 13. Retry and Dead Letter Topic

If publishing an event fails, the system retries.

Example:

```text
Attempt 1 → Failed
Attempt 2 → Failed
Attempt 3 → Failed
```

After the configured number of attempts, the event is sent to:

```text
order.created.dlt
```

This prevents permanently failed events from blocking normal processing.

---

# 14. Idempotency

Kafka can potentially deliver an event more than once.

For example:

```text
inventory.reserve
inventory.reserve
```

Without idempotency:

```text
Stock = 10

First event  → 10 → 8
Duplicate    → 8  → 6   ❌
```

With idempotency, the service detects that the order already has a reservation.

```text
First event
    ↓
Create reservation
    ↓
RESERVED

Duplicate event
    ↓
Find existing reservation
    ↓
Do nothing
```

Therefore:

```text
Stock = 10
First event → 8
Duplicate  → 8
```

---

# 15. Optimistic Locking

Inventory stock can be accessed concurrently.

The `Product` entity contains:

```java
@Version
private Long version;
```

Hibernate uses this value to detect concurrent updates.

Conceptually:

```text
Request A
version = 1

Request B
version = 1

A updates product
version = 2

B tries update using version = 1
        ↓
Version mismatch
        ↓
Optimistic locking exception
        ↓
HTTP 409 Conflict
```

This prevents lost updates.

---

# 16. Security Architecture

The security flow is:

```text
Client
  │
  │ Login
  ▼
Auth Service
  │
  │ JWT
  ▼
Client
  │
  │ Authorization: Bearer JWT
  ▼
API Gateway
  │
  │ Validate JWT
  ▼
Microservice
```

JWT contains information such as:

```text
subject = username
role    = USER / ADMIN
issuedAt
expiration
```

Passwords are stored using BCrypt rather than plain text.

---

# 17. Role-Based Authorization

The application supports roles such as:

```text
USER
ADMIN
```

The API Gateway can use the JWT role to protect sensitive operations.

For example:

```text
GET /api/products
        ↓
USER / ADMIN

POST /api/products
        ↓
ADMIN
```

This prevents unauthorized users from performing administrative operations.

---

# 18. API Gateway Responsibilities

The API Gateway acts as the boundary between external clients and internal services.

```text
                     Internet
                        │
                        ▼
                 ┌─────────────┐
                 │ API Gateway │
                 └──────┬──────┘
                        │
          ┌─────────────┼──────────────┐
          │             │              │
          ▼             ▼              ▼
       Order        Inventory       Payment
```

Benefits:

* Centralized authentication
* Centralized routing
* Service abstraction
* Security boundary
* Future rate limiting
* Future circuit breaker
* Future request tracing

---

# 19. Testing Architecture

The project uses multiple testing levels.

## Unit Testing

JUnit + Mockito are used to test business logic independently.

Example:

```text
InventoryServiceTest
PaymentServiceTest
OrderServiceTest
```

---

## Integration Testing

Integration tests verify interactions with real infrastructure.

Examples:

```text
PostgreSQL
Kafka
Spring Boot
Repositories
```

---

## Testcontainers

Testcontainers provides temporary containers during tests.

For example:

```text
Test
 │
 ├── PostgreSQL Container
 │
 └── Kafka Container
```

This provides a more realistic environment than mocking the database or Kafka.

---

# 20. Docker Architecture

The application can be started using Docker Compose.

```text
                    Docker Compose
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
    PostgreSQL          Kafka          API Gateway
        │                 │                 │
        └────────────┬────┴───────┬─────────┘
                     │            │
                     ▼            ▼
                  Services     Saga
```

Each service has its own Docker container.

The containers communicate using Docker's internal network.

For example:

```text
order-service → postgres:5432
order-service → kafka:29092
```

while external clients use:

```text
localhost:8080
localhost:8081
...
```

---

# 21. Environment Configuration

Sensitive configuration such as JWT secrets should not be hardcoded into source code.

The application uses environment variables.

Example:

```text
JWT_SECRET
```

Docker Compose reads the value from the environment.

This prevents sensitive credentials from being committed to Git.

The `.env` file is excluded using:

```text
.env
```

inside `.gitignore`.

---

# 22. Service Communication Model

The system uses two communication styles.

## Synchronous

Used mainly when a client needs an immediate response.

```text
Client
  ↓
API Gateway
  ↓
REST API
  ↓
Service
```

## Asynchronous

Used for distributed business operations.

```text
Service
   ↓
Kafka
   ↓
Saga
   ↓
Kafka
   ↓
Another Service
```

This combination gives the application both immediate API responses and loosely coupled event processing.

---

# 23. Why Microservices?

A monolithic e-commerce application could contain:

```text
User
Order
Payment
Inventory
Shipping
Notification
```

inside one application.

However, the microservices architecture separates these responsibilities.

Benefits:

* Independent deployment
* Independent scaling
* Service isolation
* Technology flexibility
* Fault isolation
* Clear ownership
* Easier maintenance

For example, inventory can be scaled independently when product traffic increases.

---

# 24. Why Kafka?

Kafka is used because the system contains several asynchronous business events.

For example:

```text
Order Created
      ↓
Inventory
      ↓
Payment
      ↓
Shipping
```

Using Kafka provides:

* Asynchronous communication
* Loose coupling
* Event persistence
* Consumer groups
* Retry capability
* Dead Letter Topics
* Scalability

---

# 25. Why Saga?

A traditional database transaction cannot easily span:

```text
Order DB
Inventory DB
Payment DB
Shipping DB
```

because each service owns a separate database.

Saga solves this by breaking one large distributed transaction into multiple local transactions.

```text
Order Transaction
       ↓
Inventory Transaction
       ↓
Payment Transaction
       ↓
Shipping Transaction
```

If something fails, compensation is performed.

---

# 26. Why Saga Orchestration?

There are two common Saga approaches:

```text
1. Choreography
2. Orchestration
```

This project uses **orchestration**.

The Saga Orchestrator knows the workflow:

```text
Order
  ↓
Inventory
  ↓
Payment
  ↓
Shipping
  ↓
Confirmation
```

This makes the business flow easier to understand and control compared with having every service independently decide what happens next.

---

# 27. Failure Isolation

A major goal of the architecture is preventing one service failure from corrupting the complete order workflow.

For example:

```text
Payment Service DOWN
       │
       ▼
Payment fails
       │
       ▼
Saga detects failure
       │
       ▼
Inventory Release
       │
       ▼
Order Cancelled
```

The system therefore reaches a consistent business state even when part of the system fails.

---

# 28. Current Architecture Summary

The complete architecture can be summarized as:

```text
                         CLIENT
                           │
                           ▼
                    ┌─────────────┐
                    │ API GATEWAY │
                    │    :8080    │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
       AUTH             ORDER           INVENTORY
       :8085            :8081             :8082
          │                │                │
       auth_db          order_db        inventory_db
                           │
                           │
                    Transactional
                       Outbox
                           │
                           ▼
                    ┌─────────────┐
                    │    KAFKA    │
                    │    :9092    │
                    └──────┬──────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │      SAGA       │
                  │  ORCHESTRATOR   │
                  │     :8090       │
                  └───────┬─────────┘
                          │
              ┌───────────┼────────────┐
              │           │            │
              ▼           ▼            ▼
           PAYMENT      SHIPPING    NOTIFICATION
            :8083         :8084         :8086
              │             │             │
          payment_db    shipping_db   notification_db
```

---

# 29. Key Design Patterns Used

The project demonstrates the following important backend patterns:

| Pattern                   | Purpose                            |
| ------------------------- | ---------------------------------- |
| Microservices             | Service decomposition              |
| API Gateway               | Single entry point                 |
| Saga Orchestration        | Distributed transaction management |
| Transactional Outbox      | Reliable event publishing          |
| Event-Driven Architecture | Asynchronous communication         |
| Idempotency               | Safe duplicate event processing    |
| Retry                     | Temporary failure recovery         |
| Dead Letter Topic         | Failed event isolation             |
| Optimistic Locking        | Concurrent stock protection        |
| Database-per-Service      | Data ownership                     |
| Global Exception Handling | Consistent API errors              |
| JWT                       | Authentication                     |
| Role-Based Authorization  | Access control                     |
| Testcontainers            | Real infrastructure testing        |
| Docker                    | Containerization                   |

---

# 30. Interview Explanation

A concise explanation of the architecture is:

> "I built a distributed e-commerce platform using Spring Boot microservices. The system has separate services for orders, inventory, payments, shipping, authentication and notifications, with PostgreSQL following the database-per-service pattern. Kafka is used for asynchronous communication, and I implemented Saga orchestration to manage the distributed order workflow and compensation when failures occur. To make event publishing reliable, I used the Transactional Outbox pattern with retry and Dead Letter Topics, and I added idempotency and optimistic locking to handle duplicate events and concurrent inventory updates. The services are secured using JWT through an API Gateway and containerized using Docker."

---

# 31. Architecture Goals

The main goals of the system are:

1. **Reliability**

   * Prevent lost events
   * Handle failures
   * Support retries

2. **Consistency**

   * Saga compensation
   * Transactional Outbox
   * Idempotency
   * Optimistic locking

3. **Scalability**

   * Independent services
   * Kafka-based asynchronous processing
   * Database-per-service

4. **Security**

   * JWT
   * BCrypt
   * Role-based authorization
   * Externalized secrets

5. **Maintainability**

   * Clear service boundaries
   * Centralized gateway
   * Independent databases
   * Automated testing

---

# 32. Future Improvements

The following improvements can be added to the platform:

* Resilience4j Circuit Breaker
* Retry and TimeLimiter
* Distributed tracing
* Correlation IDs
* Swagger/OpenAPI
* CI/CD pipeline
* AWS deployment
* Performance testing
* Load testing
* Rate limiting
* Centralized logging
* Advanced monitoring
* Kubernetes deployment

These can be introduced without changing the fundamental architecture.
