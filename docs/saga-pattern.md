# Saga Pattern

## 1. Overview

The Distributed E-Commerce Platform uses the **Saga Orchestration Pattern** to manage distributed transactions across multiple microservices.

An e-commerce order is not handled by a single service.

A single order can involve:

```text
Order Service
      ↓
Inventory Service
      ↓
Payment Service
      ↓
Shipping Service
      ↓
Notification Service
```

Each service owns its own database.

Therefore, a single database transaction cannot safely cover the entire workflow.

The Saga Pattern solves this problem by breaking the distributed transaction into a sequence of **local transactions**.

If one operation fails, Saga executes **compensating actions** for the operations that have already completed.

---

# 2. Why Saga Is Required

Consider the following databases:

```text
order_db
inventory_db
payment_db
shipping_db
```

Suppose an order is created:

```text
Order → SUCCESS
Inventory → SUCCESS
Payment → FAILED
```

At this point:

```text
Order       = Created
Inventory   = Stock Reserved
Payment     = Failed
```

We cannot simply execute:

```text
ROLLBACK
```

because there is no single transaction covering all four databases.

Instead, we need a business-level compensation:

```text
Payment Failed
      ↓
Release Inventory
      ↓
Cancel Order
```

This is exactly what Saga provides.

---

# 3. What Is a Saga?

A Saga is a sequence of local transactions.

Each service performs its own transaction and then produces an event that triggers the next step.

Example:

```text
T1 → Order Creation
T2 → Inventory Reservation
T3 → Payment
T4 → Shipping
```

Successful execution:

```text
T1 → T2 → T3 → T4
```

If `T3` fails:

```text
T1 → T2 → T3 ❌
          ↓
        C2
          ↓
        C1
```

Where:

```text
C2 = Release Inventory
C1 = Cancel Order
```

These are compensating transactions.

---

# 4. Saga in This Project

Our project uses **Saga Orchestration**.

The central component is:

```text
Saga Orchestrator
```

It controls the workflow.

```text
                  Saga Orchestrator
                         |
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
      Inventory       Payment        Shipping
```

The services do not need to decide the entire workflow themselves.

The orchestrator decides what should happen next based on the event it receives.

---

# 5. Orchestration vs Choreography

There are two major ways to implement Saga.

## 5.1 Choreography

In choreography, there is no central coordinator.

Each service listens for events and decides what it should do next.

Example:

```text
Order
  |
  | order.created
  v
Inventory
  |
  | inventory.reserved
  v
Payment
  |
  | payment.success
  v
Shipping
```

The services effectively coordinate with each other through events.

### Problem

As the number of services increases, the event relationships can become difficult to understand.

```text
Order
 ↕
Inventory
 ↕
Payment
 ↕
Shipping
 ↕
Notification
```

This can create a complex event dependency graph.

---

# 6. Orchestration

In orchestration, a central Saga Orchestrator controls the workflow.

Our architecture uses:

```text
                    Saga
                 Orchestrator
                      |
       ┌──────────────┼──────────────┐
       ↓              ↓              ↓
   Inventory       Payment        Shipping
```

The Saga knows:

```text
After order.created
        ↓
reserve inventory

After inventory.reserved
        ↓
process payment

After payment.success
        ↓
create shipping

After shipping.created
        ↓
confirm order
```

---

# 7. Why Orchestration Was Chosen

Orchestration is useful for this project because the order workflow contains several business steps.

The complete workflow can be understood from one place:

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

It also makes compensation easier to understand:

```text
Payment Failed
      ↓
Release Inventory
      ↓
Cancel Order
```

The Saga Orchestrator owns the workflow logic without owning the individual service databases.

---

# 8. Important Principle

The Saga Orchestrator **does not directly modify other services' databases**.

For example, Saga should not do:

```text
Saga → inventory_db ❌
Saga → payment_db ❌
Saga → order_db ❌
```

Instead:

```text
Saga
  ↓
Kafka Event
  ↓
Inventory Service
  ↓
Inventory DB
```

The service that owns the database performs the local transaction.

---

# 9. Complete Saga Flow

The successful workflow is:

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

This can be viewed as:

```text
Order
  ↓
Inventory
  ↓
Payment
  ↓
Shipping
  ↓
Confirmed
```

---

# 10. Step 1 — Order Created

The client creates an order.

```http
POST /api/orders
```

Order Service creates:

```text
Order ID = 101
Status   = PENDING
```

The Transactional Outbox stores:

```text
OrderCreated
```

The Outbox Publisher publishes:

```text
order.created
```

to Kafka.

---

# 11. Step 2 — Saga Receives Order Created

The Saga Orchestrator consumes:

```text
order.created
```

The Saga determines the next step:

```text
Reserve Inventory
```

It publishes:

```text
inventory.reserve
```

---

# 12. Step 3 — Inventory Reservation

Inventory Service receives:

```text
inventory.reserve
```

It performs a local transaction:

```text
1. Find Product
2. Check Stock
3. Reduce Stock
4. Create Reservation
5. Commit Transaction
```

Example:

```text
Stock = 10
Requested = 2

Stock = 8
```

Reservation:

```text
orderId   = 101
productId = 10
quantity  = 2
status    = RESERVED
```

Then Inventory publishes:

```text
inventory.reserved
```

---

# 13. Step 4 — Saga Starts Payment

Saga receives:

```text
inventory.reserved
```

It publishes:

```text
payment.process
```

Payment Service processes the payment.

Possible outcomes:

```text
SUCCESS
FAILED
```

---

# 14. Successful Payment

If payment succeeds:

```text
payment.success
```

is published.

Saga receives it and starts the next step:

```text
shipping.create
```

---

# 15. Shipping Creation

Shipping Service receives:

```text
shipping.create
```

It creates the shipment.

Then publishes:

```text
shipping.created
```

Saga receives the event and knows the workflow has successfully completed.

It publishes:

```text
order.confirm
```

---

# 16. Final Order Confirmation

Order Service receives:

```text
order.confirm
```

The order changes:

```text
PENDING → CONFIRMED
```

The successful Saga is now complete.

---

# 17. Successful Saga Diagram

```text
                         ┌───────────────────┐
                         │   Saga            │
                         │   Orchestrator    │
                         └─────────┬─────────┘
                                   │
                         order.created
                                   │
                                   ▼
                         inventory.reserve
                                   │
                                   ▼
                         ┌───────────────────┐
                         │ Inventory Service │
                         └─────────┬─────────┘
                                   │
                         inventory.reserved
                                   │
                                   ▼
                         payment.process
                                   │
                                   ▼
                         ┌───────────────────┐
                         │ Payment Service   │
                         └─────────┬─────────┘
                                   │
                           payment.success
                                   │
                                   ▼
                         shipping.create
                                   │
                                   ▼
                         ┌───────────────────┐
                         │ Shipping Service  │
                         └─────────┬─────────┘
                                   │
                          shipping.created
                                   │
                                   ▼
                           order.confirm
                                   │
                                   ▼
                         ┌───────────────────┐
                         │  Order Service    │
                         │                   │
                         │ PENDING →         │
                         │ CONFIRMED         │
                         └───────────────────┘
```

---

# 18. Failure Scenario

The most important part of Saga is failure handling.

Consider:

```text
Order Created
      ↓
Inventory Reserved
      ↓
Payment Failed
```

At this point:

```text
Order     = PENDING
Inventory = RESERVED
Payment   = FAILED
```

We need compensation.

---

# 19. Compensation

Compensation means executing another business operation that reverses the effect of a previously completed operation.

For example:

```text
Reserve Inventory
       ↓
Payment Failed
       ↓
Release Inventory
```

The Saga publishes:

```text
inventory.release
```

Inventory Service restores the stock.

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

# 20. Compensation Flow

```text
Order Created
      ↓
Inventory Reserved
      ↓
Payment Failed
      ↓
┌─────────────────────┐
│ Compensation Starts │
└──────────┬──────────┘
           ↓
inventory.release
           ↓
Inventory Restores Stock
           ↓
inventory.released
           ↓
order.cancel
           ↓
Order CANCELLED
```

---

# 21. Compensation Is Not Database Rollback

This distinction is extremely important in interviews.

A Saga does not perform:

```text
ROLLBACK DATABASE TRANSACTION
```

across all services.

Instead, it performs a **business compensation**.

For example:

```text
Original Operation:
Reserve 2 products

Compensation:
Release 2 products
```

Another example:

```text
Original:
Create Order

Compensation:
Cancel Order
```

---

# 22. Compensation Table

| Completed Operation | Failure                | Compensation           |
| ------------------- | ---------------------- | ---------------------- |
| Create Order        | Payment failure        | Cancel Order           |
| Reserve Inventory   | Payment failure        | Release Inventory      |
| Reserve Inventory   | Later failure          | Release Inventory      |
| Create Payment      | Later workflow failure | Payment refund*        |
| Create Shipping     | Later failure          | Shipment cancellation* |

`*` These can be extended in a production implementation.

---

# 23. Inventory Failure

Inventory itself can fail.

Example:

```text
order.created
      ↓
inventory.reserve
      ↓
Stock insufficient
```

Inventory publishes:

```text
inventory.failed
```

Saga receives it.

Since payment hasn't started yet, there is no payment compensation required.

The Saga can directly publish:

```text
order.cancel
```

Result:

```text
PENDING → CANCELLED
```

---

# 24. Inventory Failure Flow

```text
Order Created
      ↓
Reserve Inventory
      ↓
Stock Insufficient
      ↓
inventory.failed
      ↓
Saga
      ↓
order.cancel
      ↓
Order CANCELLED
```

---

# 25. Payment Failure

Payment failure occurs after inventory has already been reserved.

Therefore:

```text
Inventory → Must be released
```

Flow:

```text
payment.failed
      ↓
inventory.release
      ↓
inventory.released
      ↓
order.cancel
```

---

# 26. Shipping Failure

Shipping occurs after:

```text
Order
 ↓
Inventory
 ↓
Payment
```

If shipping fails, the compensation strategy becomes more important.

Conceptually:

```text
shipping failed
      ↓
refund payment
      ↓
release inventory
      ↓
cancel order
```

In a production system, a dedicated payment refund operation would normally be implemented.

The current project primarily demonstrates compensation for the payment-failure scenario.

---

# 27. Saga State

A production Saga can maintain state such as:

```text
Saga ID
Order ID
Current Step
Status
Created At
Updated At
```

Example:

```text
Saga ID     = SAGA-101
Order ID    = 101
Current Step = PAYMENT
Status      = IN_PROGRESS
```

The current implementation uses Kafka events and the orchestrator flow rather than a dedicated Saga database.

A Saga state store can be added later if persistent workflow state is required.

---

# 28. Idempotency in Saga

Saga consumers must be prepared for duplicate events.

Example:

```text
payment.success
payment.success
```

The Saga should not start shipping twice.

Similarly:

```text
inventory.reserved
inventory.reserved
```

should not trigger duplicate payment operations.

Idempotency is therefore an important property of event-driven Saga systems.

---

# 29. Idempotency in Inventory

The Inventory Service uses the order ID to identify an existing reservation.

Conceptually:

```text
findByOrderId(orderId)
```

If the reservation exists:

```text
Already processed
```

The service does not reserve the stock again.

---

# 30. Idempotency Example

Initial state:

```text
Stock = 10
```

First event:

```text
inventory.reserve
quantity = 2
```

Result:

```text
Stock = 8
```

Duplicate event:

```text
inventory.reserve
quantity = 2
```

The existing reservation is found.

Result:

```text
Stock = 8
```

not:

```text
Stock = 6
```

---

# 31. Saga and Kafka

Kafka is the transport layer for Saga events.

Saga does not need direct REST calls between every service.

Instead:

```text
Saga
  ↓
Kafka
  ↓
Service
```

This provides asynchronous communication.

Example:

```text
Saga
  |
  | payment.process
  v
Kafka
  |
  v
Payment Service
```

---

# 32. Why Kafka Fits Saga

Kafka provides:

* Asynchronous communication
* Durable events
* Consumer groups
* Event replay capabilities
* Loose coupling
* Scalability
* Failure isolation

The Saga can therefore coordinate multiple services without requiring synchronous connections between them.

---

# 33. Saga and Transactional Outbox

The Saga architecture also benefits from the Transactional Outbox Pattern.

For example, Order Service performs:

```text
BEGIN
   |
   ├── Save Order
   |
   └── Save Outbox Event
   |
COMMIT
```

Then:

```text
Outbox Publisher
      ↓
Kafka
      ↓
Saga
```

This ensures that an order creation event is not lost after the database transaction succeeds.

---

# 34. Saga and Retry

Temporary failures can occur.

For example:

```text
Kafka temporarily unavailable
Payment temporarily unavailable
Network timeout
```

The system can retry failed operations.

For events published through the Outbox:

```text
Attempt 1
   ↓
Attempt 2
   ↓
Attempt 3
   ↓
DLT
```

This prevents transient failures from immediately causing permanent business failures.

---

# 35. Dead Letter Topics

When an event cannot be successfully processed after retries, it can be moved to a Dead Letter Topic.

Example:

```text
order.created
      |
      | repeated failure
      v
order.created.dlt
```

DLTs help operations teams investigate problematic messages without blocking the main event stream.

---

# 36. Saga Failure Matrix

| Failure Point         | Previous Successful Step    | Compensation                                       |
| --------------------- | --------------------------- | -------------------------------------------------- |
| Order creation        | None                        | None                                               |
| Inventory reservation | Order                       | Cancel Order                                       |
| Payment               | Order + Inventory           | Release Inventory + Cancel Order                   |
| Shipping              | Order + Inventory + Payment | Refund Payment + Release Inventory + Cancel Order* |
| Order confirmation    | All previous steps          | Operational recovery may be required               |

`*` Refund functionality can be added when payment compensation is implemented.

---

# 37. Why Not Use Two-Phase Commit?

Two-Phase Commit (2PC) could theoretically coordinate distributed transactions, but it introduces significant complexity and blocking behavior.

Saga is generally more suitable for microservices because:

```text
Each service
     ↓
Owns its transaction
     ↓
Publishes event
     ↓
Next service continues
```

This gives better service autonomy.

---

# 38. Saga vs 2PC

| Feature                  | Saga         | 2PC           |
| ------------------------ | ------------ | ------------- |
| Distributed transactions | Yes          | Yes           |
| Database locking         | Low          | Higher        |
| Service autonomy         | High         | Lower         |
| Failure handling         | Compensation | Rollback      |
| Microservices friendly   | Yes          | Less suitable |
| Long-running workflows   | Good         | Poorer fit    |
| Complexity               | Moderate     | High          |

---

# 39. Saga Guarantees

Saga does not provide traditional ACID atomicity across all services.

Instead, it aims for:

```text
Eventual Consistency
```

For example:

```text
Order PENDING
      ↓
Payment processing
      ↓
Payment failed
      ↓
Compensation
      ↓
Order CANCELLED
```

There may be a short period where the system is in an intermediate state.

Eventually, it reaches a consistent business state.

---

# 40. Eventual Consistency

During the workflow:

```text
Order       = PENDING
Inventory   = RESERVED
Payment     = PROCESSING
```

This is an intermediate state.

After the workflow:

```text
Order       = CONFIRMED
Inventory   = RESERVED
Payment     = SUCCESS
Shipping    = CREATED
```

or:

```text
Order       = CANCELLED
Inventory   = RELEASED
Payment     = FAILED
```

This is eventual consistency.

---

# 41. Complete Saga Architecture

```text
                         CLIENT
                            |
                            v
                      API GATEWAY
                            |
                            v
                      ORDER SERVICE
                            |
                            v
                    TRANSACTIONAL OUTBOX
                            |
                            v
                          KAFKA
                            |
                            v
                  ┌────────────────────┐
                  │ SAGA ORCHESTRATOR  │
                  └─────────┬──────────┘
                            |
             ┌──────────────┼──────────────┐
             |              |              |
             v              v              v
        INVENTORY        PAYMENT        SHIPPING
             |              |              |
             └──────────────┼──────────────┘
                            |
                            v
                    ORDER CONFIRM/CANCEL
```

---

# 42. Core Saga Topics

The current workflow uses these important topics:

```text
order.created

inventory.reserve
inventory.reserved
inventory.failed

payment.process
payment.success
payment.failed

inventory.release
inventory.released

shipping.create
shipping.created

order.confirm
order.cancel
```

---

# 43. Complete Successful Event Sequence

```text
1. order.created
2. inventory.reserve
3. inventory.reserved
4. payment.process
5. payment.success
6. shipping.create
7. shipping.created
8. order.confirm
```

---

# 44. Complete Payment Failure Event Sequence

```text
1. order.created
2. inventory.reserve
3. inventory.reserved
4. payment.process
5. payment.failed
6. inventory.release
7. inventory.released
8. order.cancel
```

---

# 45. Saga Orchestrator Mental Model

When explaining the orchestrator, think:

```text
                    EVENT
                      |
                      v
             Saga Orchestrator
                      |
              What happened?
                      |
                      v
             What happens next?
                      |
                      v
               Publish Event
```

For example:

```text
inventory.reserved
        ↓
What happens next?
        ↓
payment.process
```

Another example:

```text
payment.failed
        ↓
What happens next?
        ↓
inventory.release
```

---

# 46. Interview Explanation

A strong interview explanation is:

> "I used the Saga Orchestration pattern because the order workflow spans multiple independent services and databases. The Saga Orchestrator coordinates the workflow using Kafka events. When an order is created, the Saga requests inventory reservation. After inventory succeeds, it requests payment, and after payment succeeds, it requests shipping. Finally, the order is confirmed. If payment fails after inventory has been reserved, the Saga executes compensation by releasing the inventory and then cancelling the order. Each service performs its own local transaction, while Kafka provides asynchronous communication and the Transactional Outbox ensures reliable event publishing."

---

# 47. Common Interview Questions

### Q1. Why did you choose Saga?

Because the order workflow spans multiple independent databases, and a single ACID transaction cannot cover the entire distributed workflow.

### Q2. Which Saga approach did you use?

Saga Orchestration.

### Q3. Who controls the workflow?

The Saga Orchestrator.

### Q4. Does Saga directly access service databases?

No. Each service owns its database and performs its own local transaction.

### Q5. What happens when payment fails?

The Saga publishes `inventory.release`. After inventory releases the stock, it publishes `inventory.released`, and Saga then publishes `order.cancel`.

### Q6. Is Saga the same as rollback?

No. Saga uses **compensating business transactions**, not a distributed database rollback.

### Q7. Does Saga provide strong consistency?

No. Saga provides eventual consistency across services.

### Q8. How do you handle duplicate events?

Using idempotent consumers. For example, Inventory checks whether a reservation already exists for the order.

### Q9. Why Kafka?

Kafka provides asynchronous, durable, loosely coupled event communication between services.

### Q10. What happens if an event keeps failing?

The system retries the event, and after the configured retry attempts, it can be moved to a Dead Letter Topic.

---

# 48. Key Takeaways

The most important concepts in this project's Saga implementation are:

```text
Microservices
     ↓
Independent Databases
     ↓
No Global Transaction
     ↓
Saga Orchestration
     ↓
Kafka Events
     ↓
Local Transactions
     ↓
Compensation on Failure
     ↓
Eventual Consistency
```

The complete successful workflow is:

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

The major failure workflow is:

```text
Payment Failure
      ↓
Release Inventory
      ↓
Cancel Order
```

The key architectural principle is:

> **Saga coordinates the distributed business transaction, but each microservice remains responsible for its own local transaction and database.**
