# Order Request Flow

## 1. Overview

This document explains the complete lifecycle of an order in the Distributed E-Commerce Platform.

The order workflow uses:

* REST APIs
* API Gateway
* JWT authentication
* Order Service
* Transactional Outbox
* Apache Kafka
* Saga Orchestrator
* Inventory Service
* Payment Service
* Shipping Service
* Notification Service
* Idempotency
* Retry and Dead Letter Topics
* Compensation

The system supports both:

1. **Successful order processing**
2. **Failure and compensation**

---

# 2. Complete Order Flow

The high-level flow is:

```text
Client
   |
   | POST /api/orders
   v
API Gateway
   |
   | JWT validation
   v
Order Service
   |
   | Create Order
   | Save Outbox Event
   v
Order Database
   |
   v
Outbox Publisher
   |
   | order.created
   v
Kafka
   |
   v
Saga Orchestrator
   |
   | inventory.reserve
   v
Kafka
   |
   v
Inventory Service
   |
   | Reserve Stock
   v
Inventory Database
   |
   | inventory.reserved
   v
Saga Orchestrator
   |
   | payment.process
   v
Payment Service
   |
   | payment.success
   v
Saga Orchestrator
   |
   | shipping.create
   v
Shipping Service
   |
   | shipping.created
   v
Saga Orchestrator
   |
   | order.confirm
   v
Order Service
   |
   v
Order CONFIRMED
```

---

# 3. Example Request

Assume the client wants to purchase:

```json
{
    "customerId": 101,
    "productId": 10,
    "quantity": 2,
    "totalAmount": 1599.98
}
```

The client sends:

```http
POST /api/orders
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

---

# 4. Step 1 — Client Sends Request

The client does not directly call the Order Service.

Instead, the request goes through the API Gateway.

```text
Client
   |
   | POST /api/orders
   v
API Gateway :8080
```

The gateway acts as the entry point to the system.

---

# 5. Step 2 — JWT Validation

The API Gateway checks whether the request contains a valid JWT.

Example:

```text
Authorization: Bearer eyJhbGciOiJIUzI1Ni...
```

The gateway validates:

* Token presence
* Token signature
* Token expiration
* Username
* Role

If the token is invalid:

```text
HTTP 401 Unauthorized
```

The request does not reach the Order Service.

If the token is valid:

```text
API Gateway
     |
     v
Order Service
```

---

# 6. Step 3 — Gateway Routes Request

The gateway has a route similar to:

```properties
spring.cloud.gateway.routes[0].id=order-service
spring.cloud.gateway.routes[0].uri=http://order-service:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/orders/**
```

Therefore:

```text
/api/orders/**
```

is forwarded to:

```text
http://order-service:8081
```

The client does not need to know the internal service address.

---

# 7. Step 4 — Order Controller

The Order Service receives the request.

The controller accepts:

```text
CreateOrderRequest
```

Example:

```java
public record CreateOrderRequest(
        @NotNull Long customerId,
        @NotNull Long productId,
        @NotNull @Positive Integer quantity,
        @NotNull @Positive BigDecimal totalAmount
) {}
```

Validation happens before the business logic.

For example:

```text
quantity = 0
```

is invalid because:

```java
@Positive
```

requires a value greater than zero.

The API returns:

```text
400 Bad Request
```

for invalid input.

---

# 8. Step 5 — Order Service Creates Order

The Order Service creates a new Order object.

The initial state is:

```text
PENDING
```

Example:

```text
Order
-------------------------
id           = 101
customerId   = 101
productId    = 10
quantity     = 2
totalAmount  = 1599.98
status       = PENDING
createdAt    = current time
```

The order is saved into:

```text
order_db
```

---

# 9. Step 6 — Transactional Outbox

After creating the order, the service creates an Outbox Event.

This happens inside the same database transaction.

Conceptually:

```text
BEGIN TRANSACTION

INSERT INTO orders
        ↓
INSERT INTO outbox_events
        ↓
COMMIT
```

The important point is that both operations succeed or fail together.

---

# 10. Why Outbox Is Needed

A dangerous implementation would be:

```text
Save Order
    ↓
Publish Kafka Event
```

Imagine:

```text
Order saved successfully
       ↓
Application crashes
       ↓
Kafka event not published
```

Now:

```text
Database → Order exists
Kafka    → No event
```

The distributed workflow never starts.

The Transactional Outbox solves this.

---

# 11. Outbox Record

The Outbox table contains information such as:

```text
id
eventType
aggregateType
aggregateId
payload
status
createdAt
publishedAt
retryCount
nextRetryAt
```

Example:

```text
eventType     = OrderCreated
aggregateType = Order
aggregateId   = 101
status        = PENDING
retryCount    = 0
```

The payload contains order information:

```json
{
    "orderId": 101,
    "customerId": 101,
    "productId": 10,
    "quantity": 2,
    "totalAmount": 1599.98
}
```

---

# 12. Step 7 — Outbox Publisher

The Outbox Publisher runs periodically.

It searches for:

```text
status = PENDING
```

and events whose retry time has arrived.

It then publishes the event to Kafka.

Topic:

```text
order.created
```

Flow:

```text
outbox_events
      |
      | PENDING
      v
Outbox Publisher
      |
      v
Kafka
      |
      | order.created
      v
Saga Orchestrator
```

After successful publishing:

```text
status = PUBLISHED
```

and:

```text
publishedAt = current time
```

---

# 13. Step 8 — Saga Orchestrator Receives `order.created`

The Saga Orchestrator consumes:

```text
order.created
```

The Saga now knows that a new order has been created.

The next step is inventory reservation.

It publishes:

```text
inventory.reserve
```

---

# 14. Step 9 — Inventory Reservation

Inventory Service consumes:

```text
inventory.reserve
```

The event contains information such as:

```json
{
    "orderId": 101,
    "productId": 10,
    "quantity": 2
}
```

Inventory Service retrieves the product.

Example:

```text
Product ID = 10
Stock      = 10
Requested  = 2
```

The service checks:

```text
10 >= 2
```

Therefore, the reservation can proceed.

---

# 15. Step 10 — Inventory Stock Update

The stock is reduced:

```text
Before:

Stock = 10

After:

Stock = 8
```

The service also creates an inventory reservation:

```text
orderId   = 101
productId = 10
quantity  = 2
status    = RESERVED
```

The reservation is stored in:

```text
inventory_db
```

---

# 16. Step 11 — Inventory Idempotency

Kafka events can potentially be delivered more than once.

For example:

```text
inventory.reserve
inventory.reserve
```

Without idempotency:

```text
Stock = 10

First event:
10 → 8

Duplicate event:
8 → 6
```

This would be incorrect.

The Inventory Service therefore checks whether a reservation already exists for the order.

```text
findByOrderId(orderId)
```

If a reservation already exists:

```text
Do not reserve stock again
```

Therefore:

```text
First event:
10 → 8

Duplicate event:
8 → 8
```

This makes the operation idempotent.

---

# 17. Step 12 — Optimistic Locking

The Product entity contains:

```java
@Version
private Long version;
```

This protects stock from concurrent updates.

Example:

```text
Product stock = 10
Version = 1
```

Two requests try to update the same product.

```text
Request A → Version 1
Request B → Version 1
```

Request A updates successfully:

```text
Version 1 → Version 2
```

Request B still tries using:

```text
Version 1
```

Hibernate detects the conflict.

The update fails instead of silently overwriting the newer value.

The application can return:

```text
409 Conflict
```

---

# 18. Step 13 — Inventory Success

If inventory reservation succeeds, Inventory Service publishes:

```text
inventory.reserved
```

Flow:

```text
Inventory Service
       |
       | inventory.reserved
       v
Kafka
       |
       v
Saga Orchestrator
```

---

# 19. Step 14 — Saga Starts Payment

Saga receives:

```text
inventory.reserved
```

The next step is payment.

Saga publishes:

```text
payment.process
```

Example:

```json
{
    "orderId": 101,
    "customerId": 101,
    "amount": 1599.98
}
```

---

# 20. Step 15 — Payment Processing

Payment Service consumes:

```text
payment.process
```

It processes the payment.

Possible results:

```text
SUCCESS
FAILED
```

---

# 21. Successful Payment

If payment succeeds:

```text
payment.success
```

is published.

Flow:

```text
Payment Service
      |
      | payment.success
      v
Kafka
      |
      v
Saga Orchestrator
```

---

# 22. Failed Payment

If payment fails:

```text
payment.failed
```

is published.

The Saga does not continue to shipping.

Instead, it starts compensation.

```text
payment.failed
      |
      v
inventory.release
      |
      v
Inventory Service
```

More details about compensation are covered later in this document.

---

# 23. Step 16 — Saga Starts Shipping

When payment succeeds:

```text
payment.success
```

Saga publishes:

```text
shipping.create
```

Example:

```json
{
    "orderId": 101,
    "customerId": 101,
    "address": "Customer Address"
}
```

---

# 24. Step 17 — Shipping Service

Shipping Service consumes:

```text
shipping.create
```

It creates a shipment.

Initial status:

```text
CREATED
```

The shipment is stored in:

```text
shipping_db
```

The service then publishes:

```text
shipping.created
```

---

# 25. Step 18 — Order Confirmation

Saga receives:

```text
shipping.created
```

Now the complete order workflow has succeeded.

Saga publishes:

```text
order.confirm
```

Order Service consumes:

```text
order.confirm
```

and updates:

```text
PENDING → CONFIRMED
```

The final order state becomes:

```text
CONFIRMED
```

---

# 26. Step 19 — Notification

The confirmation event can also be consumed by the Notification Service.

The Notification Service creates a notification record.

Example:

```text
Order 101
Status: SENT
Message: Order confirmed
```

This allows notification processing to remain independent from the main order transaction.

---

# 27. Complete Successful Flow

```text
Client
   |
   | POST /api/orders
   v
API Gateway
   |
   | JWT validation
   v
Order Service
   |
   | Save Order
   | Save Outbox Event
   v
Order DB
   |
   v
Outbox Publisher
   |
   | order.created
   v
Kafka
   |
   v
Saga Orchestrator
   |
   | inventory.reserve
   v
Kafka
   |
   v
Inventory Service
   |
   | Check stock
   | Reserve stock
   | Create reservation
   |
   | inventory.reserved
   v
Kafka
   |
   v
Saga Orchestrator
   |
   | payment.process
   v
Kafka
   |
   v
Payment Service
   |
   | payment.success
   v
Kafka
   |
   v
Saga Orchestrator
   |
   | shipping.create
   v
Kafka
   |
   v
Shipping Service
   |
   | shipping.created
   v
Kafka
   |
   v
Saga Orchestrator
   |
   | order.confirm
   v
Order Service
   |
   v
Order = CONFIRMED
```

---

# 28. Failure Flow — Payment Failure

Now consider a failure.

```text
Order Created
      ↓
Inventory Reserved
      ↓
Payment Failed
```

At this point:

```text
Order = PENDING
Inventory = RESERVED
Payment = FAILED
```

We cannot leave inventory reserved forever.

The Saga therefore starts compensation.

---

# 29. Step 1 — Payment Failure Event

Payment Service publishes:

```text
payment.failed
```

Saga consumes the event.

```text
payment.failed
       |
       v
Saga Orchestrator
```

---

# 30. Step 2 — Release Inventory

Saga publishes:

```text
inventory.release
```

Inventory Service consumes it.

It finds the reservation:

```text
orderId = 101
status  = RESERVED
```

It restores the stock.

Example:

```text
Before:

Stock = 8

After:

Stock = 10
```

---

# 31. Step 3 — Mark Reservation Released

The reservation status changes:

```text
RESERVED → RELEASED
```

Then Inventory Service publishes:

```text
inventory.released
```

---

# 32. Step 4 — Cancel Order

Saga receives:

```text
inventory.released
```

and publishes:

```text
order.cancel
```

Order Service consumes the event.

The order status becomes:

```text
PENDING → CANCELLED
```

---

# 33. Complete Compensation Flow

```text
Order Created
      |
      v
Inventory Reserved
      |
      v
Payment Failed
      |
      v
Saga Orchestrator
      |
      | inventory.release
      v
Inventory Service
      |
      | Restore Stock
      |
      | inventory.released
      v
Saga Orchestrator
      |
      | order.cancel
      v
Order Service
      |
      v
Order CANCELLED
```

---

# 34. Why Compensation Is Required

There is no single database transaction covering:

```text
order_db
inventory_db
payment_db
shipping_db
```

Therefore, the system cannot simply perform:

```text
ROLLBACK
```

across all services.

Instead, Saga uses business-level compensation.

For example:

```text
Reserve Inventory
        ↓
Payment Fails
        ↓
Release Inventory
```

The compensation is the reverse business operation.

---

# 35. Retry Mechanism

Temporary failures can occur.

For example:

```text
Kafka unavailable
Network failure
Temporary service failure
```

The Outbox Publisher does not immediately lose the event.

It retries publishing.

Example:

```text
Attempt 1 → Failed
Attempt 2 → Failed
Attempt 3 → Failed
```

The retry delay increases between attempts.

Conceptually:

```text
Retry 1
  ↓
Retry 2
  ↓
Retry 3
```

---

# 36. Dead Letter Topic

If an event continues to fail after the configured retries, it is sent to a Dead Letter Topic.

For example:

```text
order.created
      |
      | repeated failure
      v
order.created.dlt
```

This allows failed events to be isolated for investigation or later reprocessing.

---

# 37. What Happens If Order Creation Fails?

If validation fails before the order is created:

```text
Client
   |
   v
Order Service
   |
   v
Validation Failure
   |
   v
400 Bad Request
```

No Kafka workflow is started.

---

# 38. What Happens If Order Is Not Found?

If a request tries to access an order that does not exist:

```text
GET /api/orders/99999
```

The service throws:

```text
ResourceNotFoundException
```

The global exception handler returns:

```text
404 Not Found
```

---

# 39. What Happens During Concurrent Inventory Updates?

If two requests attempt to modify the same product:

```text
Request A
     |
     ├── Product Version 1
     |
     v
Stock Updated
     |
     v
Version 2


Request B
     |
     ├── Product Version 1
     |
     v
Version conflict
     |
     v
409 Conflict
```

This protects against lost updates.

---

# 40. Order State Machine

The order follows this lifecycle:

```text
                 ┌───────────────┐
                 │    PENDING    │
                 └───────┬───────┘
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
        ┌───────────┐         ┌───────────┐
        │ CONFIRMED │         │ CANCELLED │
        └───────────┘         └───────────┘
```

Successful flow:

```text
PENDING → CONFIRMED
```

Failure flow:

```text
PENDING → CANCELLED
```

---

# 41. Inventory State

A reservation can have states such as:

```text
RESERVED
   |
   v
RELEASED
```

Successful order:

```text
RESERVED
```

Failed payment:

```text
RESERVED → RELEASED
```

---

# 42. Payment State

```text
              ┌─────────┐
              │ PENDING │
              └────┬────┘
                   │
             ┌─────┴─────┐
             ▼           ▼
         SUCCESS       FAILED
```

---

# 43. Shipping State

Shipping progresses independently:

```text
PENDING
   ↓
CREATED
   ↓
SHIPPED
   ↓
DELIVERED
```

The current Saga workflow is primarily responsible for reaching the `CREATED` state.

---

# 44. Event Flow Summary

| Event                | Producer     | Consumer           | Purpose                |
| -------------------- | ------------ | ------------------ | ---------------------- |
| `order.created`      | Order/Outbox | Saga               | Start order workflow   |
| `inventory.reserve`  | Saga         | Inventory          | Reserve stock          |
| `inventory.reserved` | Inventory    | Saga               | Reservation successful |
| `inventory.failed`   | Inventory    | Saga               | Reservation failed     |
| `payment.process`    | Saga         | Payment            | Process payment        |
| `payment.success`    | Payment      | Saga               | Payment successful     |
| `payment.failed`     | Payment      | Saga               | Start compensation     |
| `inventory.release`  | Saga         | Inventory          | Release reserved stock |
| `inventory.released` | Inventory    | Saga               | Compensation completed |
| `shipping.create`    | Saga         | Shipping           | Create shipment        |
| `shipping.created`   | Shipping     | Saga               | Shipment created       |
| `order.confirm`      | Saga         | Order/Notification | Confirm order          |
| `order.cancel`       | Saga         | Order/Notification | Cancel order           |

---

# 45. End-to-End Mental Model

When explaining this project, remember the workflow as:

```text
CREATE
  ↓
OUTBOX
  ↓
KAFKA
  ↓
SAGA
  ↓
INVENTORY
  ↓
PAYMENT
  ↓
SHIPPING
  ↓
CONFIRM
```

Failure:

```text
PAYMENT FAILURE
      ↓
COMPENSATE
      ↓
RELEASE INVENTORY
      ↓
CANCEL ORDER
```

---

# 46. Interview Explanation

A good interview explanation is:

> "When a client creates an order, the request first goes through the API Gateway where the JWT is validated. The Order Service creates the order with PENDING status and stores an OrderCreated event in the transactional outbox within the same database transaction. The Outbox Publisher publishes that event to Kafka. The Saga Orchestrator consumes it and starts the distributed workflow by requesting inventory reservation. Once inventory is reserved, the Saga requests payment. If payment succeeds, it requests shipping creation and finally confirms the order. If payment fails, the Saga starts compensation by releasing the reserved inventory and then cancelling the order. Idempotency prevents duplicate Kafka events from reserving stock multiple times, while optimistic locking protects concurrent inventory updates."

---

# 47. Key Interview Questions

### Why don't you directly publish Kafka events after saving the order?

Because a failure between the database commit and Kafka publish could create an inconsistent state. The Transactional Outbox stores the event in the same database transaction and publishes it asynchronously.

### Why did you use Saga?

Because the order workflow spans multiple independent databases and services. A traditional local database transaction cannot safely cover all of them.

### What happens if payment fails?

The Saga publishes `inventory.release`. Inventory restores the reserved stock and publishes `inventory.released`. The Saga then publishes `order.cancel`.

### What happens if Kafka delivers the same inventory event twice?

Inventory checks whether a reservation already exists for the order. If it does, it does not reserve the stock again.

### How do you handle concurrent stock updates?

The Product entity uses JPA `@Version` optimistic locking. If another transaction changes the product before the current transaction updates it, Hibernate detects the version conflict.

### Why is Kafka used instead of REST for the Saga workflow?

Kafka provides asynchronous communication and decouples the services. Services don't have to remain synchronously connected throughout the entire distributed workflow.

### What happens when an event repeatedly fails?

The Outbox Publisher retries the event. After the configured retry attempts are exhausted, the event is moved to a Dead Letter Topic for investigation or reprocessing.

---

# 48. Final Request Flow

The entire system can be remembered as:

```text
                 CLIENT
                    |
                    v
             API GATEWAY
                    |
              JWT VALIDATION
                    |
                    v
             ORDER SERVICE
                    |
          ┌─────────┴─────────┐
          │                   │
       orders             outbox_events
          │                   │
          └─────────┬─────────┘
                    |
                    v
                  KAFKA
                    |
                    v
             SAGA ORCHESTRATOR
                    |
                    v
          INVENTORY RESERVATION
                    |
                    v
                PAYMENT
               /       \
          SUCCESS      FAILED
             |            |
             v            v
         SHIPPING     RELEASE STOCK
             |            |
             v            v
       ORDER CONFIRM   ORDER CANCEL
             |
             v
       NOTIFICATION
```

The core principle is:

**The Saga coordinates the business workflow, Kafka transports events, the Transactional Outbox guarantees reliable event publication, compensation handles failures, idempotency handles duplicate events, and optimistic locking protects concurrent inventory updates.**
