# Apache Kafka

## 1. Overview

Apache Kafka is the event streaming platform used by the Distributed E-Commerce Platform for **asynchronous communication between microservices**.

The project uses Kafka to connect:

```text
Order Service
      ↓
Kafka
      ↓
Saga Orchestrator
      ↓
Kafka
      ↓
Inventory / Payment / Shipping
```

Instead of making every service directly call another service synchronously, services communicate through Kafka events.

---

# 2. Why Kafka Is Used

The application contains multiple independent microservices.

For example:

```text
Order Service
Inventory Service
Payment Service
Shipping Service
Notification Service
Saga Orchestrator
```

Without Kafka, the communication could look like:

```text
Order
  ↓ REST
Inventory
  ↓ REST
Payment
  ↓ REST
Shipping
```

This creates strong coupling between services.

With Kafka:

```text
Order
  ↓
Kafka
  ↓
Saga
  ↓
Kafka
  ↓
Inventory
```

Services are therefore more loosely coupled.

---

# 3. Kafka in This Project

Kafka is mainly responsible for:

* Asynchronous communication
* Event-driven architecture
* Saga workflow communication
* Reliable event delivery
* Decoupling microservices
* Retry handling
* Dead Letter Topics
* Event processing
* Failure isolation

The project uses Kafka together with:

```text
Kafka
+
Saga Pattern
+
Transactional Outbox
+
Retry
+
DLT
+
Idempotent Consumers
```

---

# 4. Basic Kafka Architecture

Kafka has several important concepts:

```text
Producer
    ↓
Topic
    ↓
Partition
    ↓
Consumer
```

For example:

```text
Order Service
     |
     | Producer
     v
order.created
     |
     | Consumer
     v
Saga Orchestrator
```

---

# 5. Kafka Producer

A producer sends messages to Kafka.

In this project, examples include:

```text
Order Service
Saga Orchestrator
Inventory Service
Payment Service
Shipping Service
```

For example:

```text
Order Service
      |
      | publish
      v
order.created
```

The producer does not need to know exactly which consumer will process the event.

---

# 6. Kafka Consumer

A consumer reads messages from Kafka.

For example:

```text
order.created
      ↓
Saga Orchestrator
```

The Saga Orchestrator consumes the event and decides the next action.

Another example:

```text
inventory.release
      ↓
Inventory Service
```

---

# 7. Kafka Topic

A topic is a named stream of events.

Examples from this project:

```text
order.created
inventory.reserve
inventory.reserved
payment.process
payment.success
payment.failed
shipping.create
shipping.created
order.confirm
order.cancel
```

Think of a topic as a logical channel.

For example:

```text
payment.success
```

contains payment-success events.

---

# 8. Topics Used in This Project

| Topic                | Producer               | Consumer  | Purpose           |
| -------------------- | ---------------------- | --------- | ----------------- |
| `order.created`      | Order Service / Outbox | Saga      | Starts Saga       |
| `inventory.reserve`  | Saga                   | Inventory | Reserve stock     |
| `inventory.reserved` | Inventory              | Saga      | Inventory success |
| `inventory.failed`   | Inventory              | Saga      | Inventory failure |
| `payment.process`    | Saga                   | Payment   | Process payment   |
| `payment.success`    | Payment                | Saga      | Payment success   |
| `payment.failed`     | Payment                | Saga      | Payment failure   |
| `inventory.release`  | Saga                   | Inventory | Release stock     |
| `inventory.released` | Inventory              | Saga      | Stock released    |
| `shipping.create`    | Saga                   | Shipping  | Create shipment   |
| `shipping.created`   | Shipping               | Saga      | Shipping created  |
| `order.confirm`      | Saga                   | Order     | Confirm order     |
| `order.cancel`       | Saga                   | Order     | Cancel order      |

---

# 9. Complete Kafka Event Flow

The successful order flow is:

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

Kafka connects each stage.

---

# 10. Successful Order Flow

### Step 1

Order Service creates the order.

```text
Order
Status = PENDING
```

It eventually publishes:

```text
order.created
```

---

### Step 2

Saga consumes:

```text
order.created
```

and publishes:

```text
inventory.reserve
```

---

### Step 3

Inventory consumes:

```text
inventory.reserve
```

If stock is available:

```text
inventory.reserved
```

is published.

---

### Step 4

Saga consumes:

```text
inventory.reserved
```

and publishes:

```text
payment.process
```

---

### Step 5

Payment consumes:

```text
payment.process
```

If payment succeeds:

```text
payment.success
```

is published.

---

### Step 6

Saga consumes:

```text
payment.success
```

and publishes:

```text
shipping.create
```

---

### Step 7

Shipping consumes:

```text
shipping.create
```

and publishes:

```text
shipping.created
```

---

### Step 8

Saga consumes:

```text
shipping.created
```

and publishes:

```text
order.confirm
```

---

### Step 9

Order Service consumes:

```text
order.confirm
```

and changes:

```text
PENDING
   ↓
CONFIRMED
```

---

# 11. Successful Flow Diagram

```text
                     Kafka
                       |
                       v
              order.created
                       |
                       v
              Saga Orchestrator
                       |
                       v
             inventory.reserve
                       |
                       v
               Inventory Service
                       |
                       v
             inventory.reserved
                       |
                       v
              Saga Orchestrator
                       |
                       v
               payment.process
                       |
                       v
                Payment Service
                       |
                       v
                payment.success
                       |
                       v
              Saga Orchestrator
                       |
                       v
               shipping.create
                       |
                       v
               Shipping Service
                       |
                       v
               shipping.created
                       |
                       v
              Saga Orchestrator
                       |
                       v
                 order.confirm
                       |
                       v
                 Order Service
```

---

# 12. Payment Failure Flow

Suppose payment fails.

The event sequence becomes:

```text
order.created
      ↓
inventory.reserve
      ↓
inventory.reserved
      ↓
payment.process
      ↓
payment.failed
```

Saga then starts compensation.

```text
payment.failed
      ↓
inventory.release
      ↓
inventory.released
      ↓
order.cancel
```

Final state:

```text
Order       = CANCELLED
Inventory   = RELEASED
Payment     = FAILED
```

---

# 13. Kafka and Saga

Kafka is the communication mechanism for the Saga.

The Saga itself decides:

```text
What happened?
      ↓
What should happen next?
```

Kafka transports the event.

Example:

```text
inventory.reserved
        ↓
      Kafka
        ↓
Saga Orchestrator
        ↓
payment.process
        ↓
      Kafka
        ↓
Payment Service
```

This keeps business workflow logic inside the Saga while Kafka handles event transport.

---

# 14. Kafka and Transactional Outbox

Kafka alone does not solve the following problem:

```text
Save Order
   ↓
Publish Kafka Event
```

Suppose:

```text
Database save = SUCCESS
Kafka publish = FAILURE
```

Then the database contains the order but Kafka never receives the event.

This creates inconsistency.

---

# 15. Transactional Outbox Solution

The Order Service uses the Transactional Outbox Pattern.

Instead of:

```text
Save Order
    ↓
Publish Kafka
```

it performs:

```text
BEGIN TRANSACTION
       |
       ├── Save Order
       |
       └── Save Outbox Event
       |
COMMIT
```

Both database operations succeed or fail together.

Then a separate publisher sends the outbox event to Kafka.

```text
Order DB
   |
   | Outbox
   v
Outbox Publisher
   |
   v
Kafka
   |
   v
order.created
```

---

# 16. Outbox Event Example

An outbox record contains information such as:

```text
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
```

The publisher later changes:

```text
PENDING
   ↓
PUBLISHED
```

after successfully publishing to Kafka.

---

# 17. Why Outbox Is Important

Without Outbox:

```text
DB SUCCESS
Kafka FAILURE
```

can create lost events.

With Outbox:

```text
DB transaction
      ↓
Order + Outbox
      ↓
Publisher
      ↓
Kafka
```

The event remains in the database until it is successfully published or moved to failure handling.

---

# 18. Kafka Consumer Groups

A consumer group allows multiple consumers to work together.

Example:

```text
Consumer Group:
saga-orchestrator-group
```

The Saga consumer belongs to this group.

Kafka uses consumer groups to distribute partitions among consumers.

Conceptually:

```text
Topic
 ├── Partition 0
 ├── Partition 1
 └── Partition 2

Consumer Group
 ├── Consumer A
 ├── Consumer B
 └── Consumer C
```

Each partition is assigned to one consumer within a consumer group at a time.

---

# 19. Why Consumer Groups Matter

Consumer groups provide:

* Scalability
* Parallel processing
* Fault tolerance
* Load distribution

For example:

```text
inventory.reserve
        |
        v
inventory-service-group
        |
   ┌────┴────┐
   ↓         ↓
Consumer  Consumer
   A         B
```

If one consumer fails, Kafka can rebalance partitions among the remaining consumers.

---

# 20. Consumer Groups in This Project

Examples include:

```text
saga-orchestrator-group
inventory-service-group
payment-service-group
shipping-service-group
```

Each service can independently consume the events relevant to it.

---

# 21. Kafka Partitions

A Kafka topic can contain multiple partitions.

Example:

```text
order.created

Partition 0
Partition 1
Partition 2
```

Partitions allow Kafka to process events in parallel.

More partitions can increase throughput when consumers are scaled appropriately.

---

# 22. Ordering

Kafka guarantees ordering **within a partition**.

It does not provide global ordering across all partitions.

For example:

```text
Partition 0:

Order 101
Order 102
Order 103
```

These messages maintain their order.

But messages in:

```text
Partition 0
Partition 1
```

do not necessarily have a global ordering relationship.

---

# 23. Ordering in E-Commerce

For an order workflow, maintaining ordering for the same order can be important.

For example:

```text
Order 101:
inventory.reserve
payment.process
shipping.create
```

The events for the same order should ideally use the same partition key.

A common key is:

```text
orderId
```

Conceptually:

```text
Kafka Key = orderId
```

This allows events for the same order to be routed to the same partition.

---

# 24. Kafka Offset

An offset identifies a consumer's position in a partition.

Example:

```text
Partition 0

Offset 0
Offset 1
Offset 2
Offset 3
Offset 4
```

If a consumer has processed:

```text
Offset 0
Offset 1
Offset 2
```

its next message may be:

```text
Offset 3
```

Kafka stores consumer progress through offsets.

---

# 25. Offset and Failure

Suppose:

```text
Message Offset = 100
```

is being processed and the application crashes.

Depending on acknowledgement and consumer configuration, the message can be processed again.

This is why consumers should be designed to be **idempotent**.

---

# 26. Idempotent Consumers

A consumer should safely handle duplicate events.

Example:

```text
inventory.reserve
Order ID = 101
```

First processing:

```text
Stock 10 → 8
Reservation created
```

If the same event arrives again:

```text
Order ID = 101
```

the Inventory Service checks the existing reservation.

It should not reduce stock again.

Result:

```text
Stock remains 8
```

---

# 27. Why Duplicate Events Can Happen

Duplicates can occur because of:

* Consumer retries
* Application crashes
* Network failures
* Offset acknowledgement timing
* Producer retries
* At-least-once delivery

Therefore:

> Kafka consumers should not assume that every event will be received exactly once.

---

# 28. At-Least-Once Processing

The project follows an approach compatible with **at-least-once event processing**.

This means an event should not be silently lost, but it may potentially be delivered more than once.

Therefore:

```text
Reliable delivery
+
Idempotent consumer
```

is an important combination.

---

# 29. Retry

Temporary failures should not immediately cause permanent failure.

Example:

```text
Event
 ↓
Processing fails
 ↓
Retry
 ↓
Processing succeeds
```

For example:

```text
Attempt 1 → FAILED
Attempt 2 → FAILED
Attempt 3 → SUCCESS
```

The project also uses retry behavior around Outbox publishing.

---

# 30. Exponential Backoff

The Outbox publisher uses increasing retry delays.

Conceptually:

```text
Retry 1 → 2 seconds
Retry 2 → 4 seconds
Retry 3 → 8 seconds
```

This is called exponential backoff.

It prevents the system from continuously hammering an unavailable dependency.

---

# 31. Dead Letter Topic

If an event cannot be processed after the configured retries, it can be sent to a DLT.

For example:

```text
order.created
      ↓
Retry
      ↓
Retry
      ↓
Retry
      ↓
order.created.dlt
```

The DLT allows the failed event to be investigated separately.

---

# 32. Why DLT Is Useful

Without a DLT:

```text
Bad Event
   ↓
Consumer fails
   ↓
Retry forever
```

This can block normal processing.

With a DLT:

```text
Bad Event
   ↓
Retry
   ↓
Retry limit reached
   ↓
DLT
   ↓
Normal processing continues
```

---

# 33. Kafka Failure Scenarios

## Scenario 1 — Kafka Unavailable

```text
Order Created
      ↓
Outbox Event
      ↓
Kafka unavailable
```

The event remains in the Outbox.

The publisher retries later.

---

## Scenario 2 — Consumer Crashes

```text
Kafka
  ↓
Consumer
  ↓
Crash
```

Kafka can reassign the partition after consumer group rebalancing.

The event may be processed again.

Therefore the consumer must be idempotent.

---

## Scenario 3 — Processing Fails

```text
Event
 ↓
Consumer
 ↓
Exception
```

The system can retry the event.

If retries are exhausted:

```text
DLT
```

---

# 34. Kafka and Database Transactions

Kafka transactions and database transactions are different concepts.

For example:

```text
PostgreSQL Transaction
```

controls database changes.

Kafka manages:

```text
Kafka Message / Offset
```

The project uses the **Transactional Outbox Pattern** to bridge reliable database state changes with event publishing.

This is an important distinction during interviews.

---

# 35. Kafka vs REST

## REST

```text
Service A
   |
   | HTTP Request
   v
Service B
```

Service A waits for Service B.

This is synchronous communication.

## Kafka

```text
Service A
   |
   | Event
   v
Kafka
   |
   v
Service B
```

Service A does not need to wait for Service B to finish processing.

This is asynchronous communication.

---

# 36. When REST Is Better

REST is useful when the caller needs an immediate response.

Example:

```text
GET /api/orders/101
```

The client expects:

```text
Order details
```

immediately.

---

# 37. When Kafka Is Better

Kafka is useful for asynchronous business events.

Examples:

```text
Order Created
Payment Completed
Inventory Reserved
Shipping Created
Notification Requested
```

These events do not always require an immediate synchronous response.

---

# 38. Kafka in Docker

The project runs Kafka through Docker Compose.

The Kafka container is:

```text
ecommerce-kafka
```

The application containers communicate with Kafka using:

```text
kafka:29092
```

The host machine can connect using:

```text
localhost:9092
```

This distinction is important.

---

# 39. Internal vs External Kafka Address

Inside Docker:

```text
kafka:29092
```

Example:

```text
Order Container
      ↓
kafka:29092
```

From the Mac host:

```text
localhost:9092
```

Example:

```text
Local Java Application
      ↓
localhost:9092
```

The project is configured with separate internal and external Kafka listeners.

---

# 40. Kafka KRaft

The project uses:

```text
Apache Kafka 4.0.0
```

with KRaft mode.

KRaft allows Kafka to operate without the older ZooKeeper dependency.

The Docker setup therefore does not require a separate ZooKeeper container.

Architecture:

```text
Kafka
  |
  └── KRaft Metadata Management
```

instead of:

```text
Kafka
  |
ZooKeeper
```

---

# 41. Checking Kafka

From the project root:

```bash
cd ~/Desktop/distrubuted-ecommerce
```

Check containers:

```bash
docker compose ps
```

Check Kafka logs:

```bash
docker compose logs kafka
```

Or:

```bash
docker logs ecommerce-kafka
```

---

# 42. List Kafka Topics

Run:

```bash
docker exec -it ecommerce-kafka \
/opt/kafka/bin/kafka-topics.sh \
--bootstrap-server localhost:9092 \
--list
```

You should see topics such as:

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

# 43. Describe a Topic

To inspect a topic:

```bash
docker exec -it ecommerce-kafka \
/opt/kafka/bin/kafka-topics.sh \
--bootstrap-server localhost:9092 \
--describe \
--topic order.created
```

This can show:

```text
Partition count
Replication factor
Leader
Replicas
ISR
```

---

# 44. Produce a Test Message

You can manually publish a message:

```bash
docker exec -it ecommerce-kafka \
/opt/kafka/bin/kafka-console-producer.sh \
--bootstrap-server localhost:9092 \
--topic order.created
```

Then enter:

```json
{"orderId":101,"customerId":1,"productId":10,"quantity":2,"totalAmount":79999}
```

Press Enter.

---

# 45. Consume a Test Message

You can read messages using:

```bash
docker exec -it ecommerce-kafka \
/opt/kafka/bin/kafka-console-consumer.sh \
--bootstrap-server localhost:9092 \
--topic order.created \
--from-beginning
```

This is useful for debugging Kafka communication.

---

# 46. Debugging Kafka

When an event is not reaching a service, check in this order:

```text
1. Is Kafka running?
        ↓
2. Does the topic exist?
        ↓
3. Is the producer publishing?
        ↓
4. Is the consumer running?
        ↓
5. Is the consumer subscribed to the correct topic?
        ↓
6. Is the consumer group correct?
        ↓
7. Are there exceptions in service logs?
        ↓
8. Is the message stuck in retry/DLT?
```

---

# 47. Kafka Logs

For example:

```bash
docker compose logs -f saga-orchestrator
```

Inventory:

```bash
docker compose logs -f inventory-service
```

Payment:

```bash
docker compose logs -f payment-service
```

Shipping:

```bash
docker compose logs -f shipping-service
```

These logs help verify the event flow.

---

# 48. Kafka Testing Strategy

Kafka should be tested at multiple levels.

## Unit Tests

Test individual producer/consumer logic using mocks.

Example:

```text
Consumer receives event
       ↓
Service method called
       ↓
Expected event published
```

## Integration Tests

Use a real Kafka instance with Testcontainers.

Example:

```text
Test
 ↓
Kafka Container
 ↓
Producer
 ↓
Consumer
 ↓
Assertion
```

This gives more realistic testing.

---

# 49. Testcontainers Kafka

The project already uses Testcontainers for integration testing.

A Kafka integration test can verify:

```text
Producer
   ↓
Kafka
   ↓
Consumer
```

instead of mocking Kafka completely.

This helps catch configuration and serialization problems that unit tests may miss.

---

# 50. Kafka Serialization

Events must be converted into a format that can be transported through Kafka.

The project uses JSON-style event payloads.

Example:

```json
{
  "orderId": 101,
  "customerId": 1,
  "productId": 10,
  "quantity": 2,
  "totalAmount": 79999
}
```

JSON is easy to inspect during development and debugging.

---

# 51. Event Design

Kafka events should contain the information required by the consumer.

For example:

```text
inventory.reserve
```

needs information such as:

```text
orderId
productId
quantity
```

A good event should avoid unnecessary service-specific internal data.

---

# 52. Event Naming

The project follows an event-oriented naming style:

```text
order.created
inventory.reserved
payment.success
shipping.created
```

These names represent something that happened.

Commands are represented separately:

```text
inventory.reserve
payment.process
shipping.create
order.confirm
```

This distinction is useful:

```text
Command:
"Please do this."

Event:
"This has happened."
```

---

# 53. Command vs Event

Example:

```text
inventory.reserve
```

means:

> Please reserve inventory.

Whereas:

```text
inventory.reserved
```

means:

> Inventory reservation completed successfully.

Similarly:

```text
payment.process
```

means:

> Process this payment.

While:

```text
payment.success
```

means:

> Payment processing succeeded.

---

# 54. Kafka Mental Model

The easiest way to remember Kafka is:

```text
Producer
   ↓
Topic
   ↓
Partition
   ↓
Consumer Group
   ↓
Consumer
   ↓
Business Logic
```

For this project:

```text
Order Service
   ↓
order.created
   ↓
Saga Orchestrator
   ↓
inventory.reserve
   ↓
Inventory Service
```

---

# 55. Complete Architecture

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
                   SAGA ORCHESTRATOR
                            |
             ┌──────────────┼──────────────┐
             |              |              |
             v              v              v
        INVENTORY         PAYMENT       SHIPPING
             |              |              |
             └──────────────┼──────────────┘
                            |
                            v
                     ORDER CONFIRM/CANCEL
                            |
                            v
                    NOTIFICATION SERVICE
```

---

# 56. Important Design Principles

The Kafka implementation follows these principles:

### 1. Loose Coupling

Services communicate through events instead of directly depending on each other.

### 2. Asynchronous Processing

Services can process events independently.

### 3. Idempotency

Consumers should safely process duplicate events.

### 4. Retry

Temporary failures can be retried.

### 5. DLT

Messages that repeatedly fail can be isolated.

### 6. Transactional Outbox

Database changes and event creation are made reliable.

### 7. Eventual Consistency

The complete business transaction reaches consistency over time.

---

# 57. Interview Explanation

A concise interview answer:

> "I used Kafka as the event backbone of my microservices architecture. Services communicate asynchronously using topics such as `order.created`, `inventory.reserve`, `payment.process`, and `shipping.create`. The Saga Orchestrator consumes events and coordinates the distributed order workflow. I also used the Transactional Outbox pattern in the Order Service so that an order and its corresponding event are persisted atomically before the event is published to Kafka. Consumers are designed to be idempotent because event processing can be at-least-once, and retry plus Dead Letter Topics are used for failure handling."

---

# 58. Common Interview Questions

### Q1. What is Kafka?

Kafka is a distributed event streaming platform used for high-throughput, durable, asynchronous communication.

### Q2. What is a Kafka topic?

A topic is a named stream/channel where producers publish records and consumers read them.

### Q3. What is a partition?

A partition is an ordered sequence of records within a Kafka topic. Partitions provide scalability and parallel processing.

### Q4. What is a consumer group?

A consumer group is a group of consumers that jointly consume partitions of a topic.

### Q5. Does Kafka guarantee ordering?

Kafka guarantees ordering within a partition, not globally across all partitions.

### Q6. Why use orderId as a Kafka key?

It can ensure events for the same order are routed to the same partition, preserving their relative ordering.

### Q7. What happens if a consumer crashes?

Kafka can reassign its partitions to another consumer in the same consumer group. Depending on acknowledgement/offset behavior, an event may be processed again.

### Q8. Why do we need idempotency?

Because at-least-once processing can result in duplicate event delivery.

### Q9. What is a DLT?

A Dead Letter Topic stores messages that could not be successfully processed after the configured retry attempts.

### Q10. Why use Kafka instead of REST?

Kafka provides asynchronous, decoupled, durable event communication and is well suited for event-driven workflows.

### Q11. Why use Transactional Outbox with Kafka?

It prevents the situation where the database transaction succeeds but publishing the corresponding Kafka event fails.

### Q12. Is Kafka a database?

No. Kafka is primarily a distributed event streaming platform/log, not a replacement for the service's transactional database.

### Q13. Does Kafka guarantee exactly-once processing automatically?

No. Exactly-once semantics involve additional Kafka/application configuration and design. Consumers should still be designed carefully for duplicate processing.

### Q14. How does Kafka help Saga?

Kafka transports the commands and events between the Saga Orchestrator and participating microservices.

---

# 59. Final Mental Model

Remember the project like this:

```text
                    DATABASE
                       |
                 Local Transaction
                       |
                       v
                  OUTBOX EVENT
                       |
                       v
                     KAFKA
                       |
                       v
               SAGA ORCHESTRATOR
                       |
                       v
                NEXT COMMAND
                       |
                       v
                     KAFKA
                       |
                       v
                 NEXT SERVICE
```

And the complete business workflow:

```text
Order Created
     ↓
Kafka
     ↓
Saga
     ↓
Reserve Inventory
     ↓
Kafka
     ↓
Payment
     ↓
Kafka
     ↓
Shipping
     ↓
Kafka
     ↓
Confirm Order
```

If something fails:

```text
Failure
   ↓
Saga
   ↓
Compensation
   ↓
Release / Cancel / Refund
```

The core architecture is therefore:

```text
Kafka
  +
Saga
  +
Transactional Outbox
  +
Idempotency
  +
Retry
  +
DLT
  =
Reliable Event-Driven Microservices
```
