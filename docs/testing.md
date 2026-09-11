# Testing

## 1. Overview

Testing is an important part of the Distributed E-Commerce Platform because the application contains multiple microservices, asynchronous Kafka communication, database transactions, Saga orchestration, compensation logic, authentication, and transactional outbox processing.

The project uses multiple levels of testing:

```text
                 Testing Strategy
                       |
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
    Unit Tests    Integration Tests  E2E/API Tests
        |              |
   JUnit/Mockito   Testcontainers
                       |
                ┌──────┴──────┐
                ↓             ↓
             PostgreSQL      Kafka
```

The main technologies used are:

```text
JUnit 5
Mockito
Spring Boot Test
Testcontainers
PostgreSQL
Kafka
```

---

# 2. Testing Goals

The main goals of testing are:

* Verify individual business logic
* Verify database operations
* Verify Kafka producers and consumers
* Verify transactional behavior
* Verify idempotency
* Verify optimistic locking
* Verify Saga compensation
* Verify authentication and authorization
* Verify failure scenarios
* Reduce production defects
* Ensure microservices work correctly together

---

# 3. Testing Pyramid

The project follows the concept of a testing pyramid.

```text
                  /\
                 /  \
                / E2E\
               /------\
              /        \
             /Integration\
            /--------------\
           /                \
          /   Unit Tests    \
         /------------------\
```

Generally:

```text
More Unit Tests
      ↓
Fewer Integration Tests
      ↓
Very Few End-to-End Tests
```

Unit tests are fast and should cover most business logic.

Integration tests are slower but verify real infrastructure such as PostgreSQL and Kafka.

---

# 4. Unit Testing

Unit testing verifies a small part of the application independently.

For example:

```text
OrderService
     ↓
Test OrderService
     ↓
Mock Repository
     ↓
Verify Business Logic
```

Unit tests should not require a real database or Kafka broker.

---

# 5. JUnit 5

JUnit 5 is the primary testing framework.

Typical annotations include:

```java
@Test
@BeforeEach
@AfterEach
```

Example:

```java
@Test
void shouldCreateOrder() {
    // Arrange

    // Act

    // Assert
}
```

---

# 6. Arrange-Act-Assert

Tests follow the common:

```text
Arrange
   ↓
Act
   ↓
Assert
```

pattern.

Example:

```java
@Test
void shouldCreateOrder() {

    // Arrange
    CreateOrderRequest request =
            new CreateOrderRequest(
                    101L,
                    1L,
                    2,
                    new BigDecimal("79999")
            );

    // Act
    Order result = orderService.createOrder(request);

    // Assert
    assertNotNull(result);
}
```

---

# 7. Mockito

Mockito is used to mock dependencies.

For example:

```text
OrderService
     |
     +---- OrderRepository
     |
     +---- OutboxEventRepository
```

During a unit test:

```text
OrderService
     |
     +---- Mock OrderRepository
     |
     +---- Mock OutboxEventRepository
```

This allows us to test business logic without requiring PostgreSQL.

---

# 8. Why Mockito?

Suppose OrderService uses:

```java
orderRepository.save(order);
```

A unit test does not need a real PostgreSQL database.

Instead:

```java
when(orderRepository.save(any(Order.class)))
        .thenReturn(savedOrder);
```

We can then verify:

```java
verify(orderRepository).save(any(Order.class));
```

---

# 9. Order Service Testing

The Order Service contains important business logic.

Tests verify:

* Order creation
* Order status
* Repository interaction
* Outbox event creation
* Order confirmation
* Order cancellation
* Order not found scenarios

Example test cases:

```text
Create Order
     ↓
Order saved
     ↓
Outbox event created
```

and:

```text
Invalid Order ID
     ↓
ResourceNotFoundException
```

---

# 10. Transactional Outbox Testing

The Order Service uses the Transactional Outbox pattern.

The important guarantee is:

```text
Database Transaction
       |
       +---- Save Order
       |
       +---- Save Outbox Event
```

Both operations should happen as part of the same transaction.

Testing verifies that the outbox event is created when an order is created.

Expected:

```text
orders
   ↓
Order created

outbox_events
   ↓
OrderCreated event created
```

---

# 11. Outbox Publisher Testing

The Outbox Publisher periodically checks:

```text
PENDING
```

events.

It publishes them to:

```text
order.created
```

After successful publishing:

```text
PENDING
   ↓
PUBLISHED
```

The test should verify:

* Pending events are selected
* Kafka publish is attempted
* Event becomes PUBLISHED
* `publishedAt` is set

---

# 12. Outbox Failure Testing

If Kafka publishing fails:

```text
PENDING
   ↓
Kafka failure
   ↓
Retry
```

The retry count is increased.

Example:

```text
retryCount = 0
     ↓
failure
     ↓
retryCount = 1
```

After the configured number of failures:

```text
PENDING
   ↓
Retry
   ↓
Retry
   ↓
Retry
   ↓
DLT
   ↓
FAILED
```

This behavior should be tested.

---

# 13. DLT Testing

The project uses a Dead Letter Topic:

```text
order.created.dlt
```

The purpose of the DLT is to prevent permanently failing events from being retried forever.

Expected behavior:

```text
Event
  ↓
Processing fails
  ↓
Retry
  ↓
Retry
  ↓
Retry
  ↓
DLT
```

The test should verify that the event is eventually marked as:

```text
FAILED
```

and sent to the DLT.

---

# 14. Inventory Service Testing

Inventory testing covers:

* Product creation
* Product retrieval
* Stock reservation
* Insufficient stock
* Reservation creation
* Idempotent reservation
* Stock release
* Idempotent release
* Optimistic locking

---

# 15. Inventory Reservation Test

Example:

```text
Initial Stock = 10
Quantity      = 3
```

After reservation:

```text
Stock = 7
```

Expected:

```java
assertEquals(7, product.getStock());
```

and:

```text
Reservation
status = RESERVED
```

---

# 16. Insufficient Stock Test

Suppose:

```text
Stock = 2
Requested = 5
```

Expected:

```text
Reservation = FAILED
Stock = 2
```

Stock should not become negative.

This is an important business rule.

---

# 17. Idempotency Testing

Kafka uses at-least-once delivery semantics in many practical setups, meaning a consumer can potentially receive the same event more than once.

The Inventory Service therefore handles duplicate reservation requests.

Example:

```text
orderId = 101
productId = 1
quantity = 2
```

First event:

```text
Reserve Stock
      ↓
Stock decreases
      ↓
Reservation = RESERVED
```

Duplicate event:

```text
Same orderId
      ↓
Existing reservation found
      ↓
Do NOT decrease stock again
```

Expected:

```text
Stock changes only once.
```

---

# 18. Idempotency Test

Test scenario:

```text
Initial stock = 10

First reservation
quantity = 3

Stock = 7

Duplicate reservation
quantity = 3

Stock should remain = 7
```

This protects the system against duplicate Kafka messages.

---

# 19. Inventory Release Testing

When payment fails:

```text
payment.failed
      ↓
inventory.release
      ↓
releaseStock()
```

Suppose:

```text
Initial stock = 10
Reserved = 3
```

After reservation:

```text
Stock = 7
```

After release:

```text
Stock = 10
```

The test should verify this compensation behavior.

---

# 20. Idempotent Release

The release operation should also be idempotent.

First release:

```text
RESERVED
   ↓
RELEASED
   ↓
Stock restored
```

Second release:

```text
RELEASED
   ↓
No additional stock restoration
```

This prevents:

```text
Stock = 10
     ↓
Release
Stock = 13
```

from accidentally happening.

---

# 21. Optimistic Locking Testing

The Product entity uses:

```java
@Version
private Long version;
```

This protects against concurrent stock updates.

Conceptually:

```text
Transaction A
version = 1

Transaction B
version = 1
```

Transaction A updates first:

```text
version = 2
```

Transaction B tries to update using:

```text
version = 1
```

The update fails.

---

# 22. Optimistic Lock Test

The test should simulate concurrent modification.

Expected result:

```text
OptimisticLockingFailureException
```

The application handles this as:

```text
HTTP 409 Conflict
```

with a message such as:

```text
Product stock was updated by another request.
Please try again.
```

---

# 23. Payment Service Testing

Payment tests cover:

* Payment processing
* Payment success
* Payment failure
* Payment event handling
* Event publishing

Example:

```text
payment.process
      ↓
Payment Service
      ↓
Process payment
      ↓
payment.success
```

Failure:

```text
payment.process
      ↓
Payment Service
      ↓
Payment failed
      ↓
payment.failed
```

---

# 24. Payment Consumer Testing

The Payment consumer receives:

```text
payment.process
```

The test verifies that:

```text
Payment successful
      ↓
payment.success
```

or:

```text
Payment failed
      ↓
payment.failed
```

is published correctly.

---

# 25. Shipping Testing

Shipping tests verify:

* Shipping creation
* Shipping status
* Shipping event consumption
* `shipping.created` event publishing

Flow:

```text
shipping.create
      ↓
Shipping Service
      ↓
Create shipment
      ↓
shipping.created
```

---

# 26. Shipping Consumer Test

A Kafka event:

```json
{
  "orderId": 101,
  "customerId": 1
}
```

is consumed.

The test verifies:

```text
Event received
      ↓
Shipment created
      ↓
shipping.created published
```

---

# 27. Saga Testing

The Saga Orchestrator coordinates the distributed transaction.

Successful flow:

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

Tests should verify that the correct event is produced after each step.

---

# 28. Saga Compensation Testing

Payment failure:

```text
payment.failed
      ↓
inventory.release
      ↓
inventory.released
      ↓
order.cancel
```

This verifies compensation.

The test should ensure that a payment failure does not leave inventory permanently reserved.

---

# 29. Compensation Test

Example:

```text
Order
PENDING

Inventory
RESERVED

Payment
FAILED
```

Expected:

```text
Inventory
RELEASED

Order
CANCELLED
```

This is one of the most important distributed transaction tests in the project.

---

# 30. Kafka Testing

Kafka testing verifies actual event communication.

For unit tests:

```text
KafkaProducer
     ↓
Mock
```

For integration tests:

```text
Application
     ↓
Real Kafka Container
     ↓
Producer
     ↓
Topic
     ↓
Consumer
```

The second approach verifies actual Kafka behavior.

---

# 31. Testcontainers

Testcontainers allows tests to start real infrastructure using containers.

The project uses Testcontainers for:

```text
PostgreSQL
Kafka
```

Architecture:

```text
JUnit Test
    |
    +---- Testcontainers
             |
        ┌────┴────┐
        ↓         ↓
   PostgreSQL    Kafka
```

---

# 32. Why Testcontainers?

Using mocks for everything can hide infrastructure-related problems.

For example:

```text
Mock PostgreSQL
```

does not prove that the application works correctly with real PostgreSQL.

Testcontainers provides a real database environment.

---

# 33. PostgreSQL Integration Testing

A PostgreSQL container is started for integration tests.

Example concept:

```java
@Testcontainers
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");
}
```

The test then interacts with an actual PostgreSQL database.

---

# 34. PostgreSQL Test Flow

```text
JUnit
  ↓
Start PostgreSQL Container
  ↓
Spring Boot Application
  ↓
JPA/Hibernate
  ↓
PostgreSQL
  ↓
Execute Test
  ↓
Container Cleanup
```

---

# 35. Repository Integration Tests

Repository tests verify real database behavior.

For example:

```text
Save Order
   ↓
PostgreSQL
   ↓
Find Order
   ↓
Assert Result
```

This validates:

* Entity mapping
* Table mapping
* Columns
* Constraints
* Queries
* Database interaction

---

# 36. Outbox Integration Testing

A particularly important test is:

```text
Create Order
     ↓
Database Transaction
     ↓
orders table
     +
outbox_events table
```

The integration test verifies that both records exist.

Example:

```text
orders
id = 101

outbox_events
aggregateId = 101
status = PENDING
```

This validates the Transactional Outbox implementation against a real database.

---

# 37. Kafka Integration Testing with Testcontainers

The project also uses a real Kafka container.

Flow:

```text
JUnit
  ↓
Kafka Container
  ↓
Producer
  ↓
order.created
  ↓
Consumer
  ↓
Assert Event
```

This verifies that producer and consumer configuration works with a real Kafka broker.

---

# 38. Kafka Integration Test

Example scenario:

```text
Publish order.created
       ↓
Kafka
       ↓
Saga Consumer
       ↓
inventory.reserve
```

The test verifies that the expected downstream event is produced.

---

# 39. Consumer Group Testing

Kafka consumers use consumer groups.

For example:

```text
saga-orchestrator-group
```

The test should ensure that the expected consumer receives the event.

Important concepts tested include:

```text
Topic
Partition
Consumer Group
Offset
```

---

# 40. Testing At-Least-Once Behavior

Because duplicate event processing is possible, consumers should be idempotent.

Testing should include:

```text
Event
 ↓
Process
 ↓
Same Event Again
 ↓
Process Again
```

Expected:

```text
No duplicate business effect
```

For Inventory:

```text
Stock should only decrease once.
```

---

# 41. Global Exception Testing

The Order Service contains a global exception handler.

Tests should verify:

### Resource not found

```text
GET /api/orders/999
```

Expected:

```text
404 NOT_FOUND
```

### Invalid request

Expected:

```text
400 BAD_REQUEST
```

### Unexpected exception

Expected:

```text
500 INTERNAL_SERVER_ERROR
```

---

# 42. Validation Testing

The project validates request fields.

For example:

```java
@NotNull
Long customerId
```

and:

```java
@Positive
Integer quantity
```

Invalid request:

```json
{
  "customerId": null,
  "productId": 1,
  "quantity": -2
}
```

should return:

```text
400 Bad Request
```

---

# 43. API Gateway Security Testing

Security tests should cover:

```text
Public endpoint
      ↓
Allowed without JWT
```

Example:

```text
POST /api/auth/login
```

Protected endpoint:

```text
GET /api/orders/1
```

without JWT:

```text
401 Unauthorized
```

---

# 44. JWT Testing

JWT tests should cover:

```text
Valid JWT
Invalid JWT
Expired JWT
Missing JWT
```

Expected:

```text
Valid token
   ↓
Request allowed

Invalid token
   ↓
401

Expired token
   ↓
401
```

---

# 45. Role Authorization Testing

Test:

```text
USER → normal protected endpoint
```

and:

```text
USER → ADMIN endpoint
```

Expected:

```text
USER
 ↓
Admin API
 ↓
403 Forbidden
```

Admin:

```text
ADMIN
 ↓
Admin API
 ↓
Allowed
```

---

# 46. Test Data

Tests should use predictable test data.

Example:

```text
customerId = 101
productId = 1
quantity = 2
totalAmount = 79999
```

This makes failures easier to understand.

Avoid relying on production data.

---

# 47. Test Isolation

Tests should not depend on each other.

Bad:

```text
Test A creates order 1

Test B assumes order 1 exists
```

Instead:

```text
Test A
Independent

Test B
Independent
```

Each test should prepare its required data.

---

# 48. Unit vs Integration Tests

| Feature           | Unit Test | Integration Test |
| ----------------- | --------- | ---------------- |
| Service logic     | Yes       | Yes              |
| Repository        | Mocked    | Real             |
| PostgreSQL        | No        | Yes              |
| Kafka             | Mocked    | Real             |
| Speed             | Fast      | Slower           |
| Infrastructure    | No        | Yes              |
| Business logic    | Yes       | Yes              |
| External behavior | Limited   | Strong           |

---

# 49. What Should Be Mocked?

Good candidates for mocking:

```text
Repository
Kafka Producer
External API
Other service clients
```

when testing isolated business logic.

For example:

```text
OrderService
    ↓
Mock Repository
```

---

# 50. What Should Not Always Be Mocked?

For integration tests, use real infrastructure for:

```text
PostgreSQL
Kafka
```

because these are critical parts of the application architecture.

---

# 51. Test Naming

Tests should have descriptive names.

Good:

```java
shouldCreateOrderAndOutboxEvent()
```

```java
shouldReturnFalseWhenStockIsInsufficient()
```

```java
shouldNotReserveStockTwiceForSameOrder()
```

```java
shouldReleaseReservedStock()
```

Bad:

```java
test1()
test2()
testMethod()
```

---

# 52. Testing Failure Scenarios

Distributed systems must test failures, not just successful flows.

Important scenarios:

```text
Database unavailable
Kafka unavailable
Payment fails
Inventory unavailable
Duplicate event
Invalid JWT
Expired JWT
Insufficient stock
Optimistic locking conflict
Outbox publishing failure
```

---

# 53. Failure Scenario Example

Suppose Kafka is unavailable:

```text
Order created
     ↓
Outbox event saved
     ↓
Kafka unavailable
     ↓
Publishing fails
     ↓
Retry
```

The important property is:

> The order is not lost just because Kafka was temporarily unavailable.

The outbox event remains available for retry.

---

# 54. Testing Eventual Consistency

The architecture uses asynchronous communication.

Therefore, this assumption is incorrect:

```text
Create Order
    ↓
Immediately
    ↓
Everything confirmed
```

Instead:

```text
Create Order
    ↓
PENDING
    ↓
Kafka Events
    ↓
Inventory
    ↓
Payment
    ↓
Shipping
    ↓
CONFIRMED
```

Integration tests should account for asynchronous processing.

---

# 55. Asynchronous Testing

When testing Kafka consumers, avoid assuming that an event is processed immediately.

Conceptually:

```text
Publish Event
     ↓
Wait for Consumer
     ↓
Verify Result
```

Tests can use polling/wait mechanisms instead of arbitrary long sleeps.

This makes tests more reliable.

---

# 56. Testcontainers Lifecycle

The general lifecycle is:

```text
Test starts
    ↓
Container starts
    ↓
Spring connects
    ↓
Test executes
    ↓
Assertions
    ↓
Container stops
```

The container provides an isolated infrastructure environment for the test.

---

# 57. Testing Database Constraints

The project uses database constraints such as:

```text
Unique order reservation
```

For example:

```text
uk_inventory_reservation_order
```

The test should verify that duplicate reservation records for the same order cannot be created.

This provides another layer of protection beyond application-level idempotency.

---

# 58. Testing Database Indexes

Indexes are primarily performance features, so they are usually verified through database/schema inspection and performance testing rather than simple unit assertions.

Important indexes include:

```text
orders.customerId
orders.status

payments.orderId

shipping.orderId

notifications.orderId
```

The Inventory Reservation `orderId` unique constraint already creates a unique index in PostgreSQL, so an additional normal index is unnecessary.

---

# 59. Testing Optimistic Locking

The inventory Product entity contains:

```java
@Version
private Long version;
```

Testing should simulate two concurrent updates.

Conceptually:

```text
Transaction A       Transaction B
     |                   |
 version 1            version 1
     |                   |
 update                 update
     |                   |
 version 2            conflict
                         |
                         v
                Optimistic Lock Error
```

This prevents silent lost updates.

---

# 60. Test Coverage

Coverage helps identify untested code.

Important areas to cover include:

```text
Service logic
Exception paths
Kafka consumers
Kafka producers
Database operations
Compensation logic
Security
Validation
```

However:

> High code coverage does not automatically mean high-quality testing.

Tests should validate meaningful business behavior.

---

# 61. Important Business Scenarios

The most important scenarios in this project are:

### Successful Order

```text
Order
 ↓
Inventory Reserved
 ↓
Payment Success
 ↓
Shipping Created
 ↓
Order Confirmed
```

### Payment Failure

```text
Order
 ↓
Inventory Reserved
 ↓
Payment Failed
 ↓
Inventory Released
 ↓
Order Cancelled
```

### Inventory Failure

```text
Order
 ↓
Inventory Failed
 ↓
Order Cancelled
```

### Duplicate Event

```text
Event
 ↓
Processed

Same Event
 ↓
No duplicate effect
```

---

# 62. Test Execution

For Maven-based services, the standard command is:

```bash
mvn test
```

For a specific test:

```bash
mvn -Dtest=OrderServiceTest test
```

For an integration test:

```bash
mvn -Dtest=OrderServiceIntegrationTest test
```

Depending on the Maven/Surefire configuration, integration tests may also be separated using a naming convention or dedicated Maven profile.

---

# 63. Running All Tests

From an individual service:

```bash
cd order-service
mvn test
```

Similarly:

```bash
cd inventory-service
mvn test
```

and for other services:

```text
payment-service
shipping-service
auth-service
notification-service
api-gateway
saga-orchestrator
```

---

# 64. Docker-Based Testing

For infrastructure-level validation:

```bash
docker compose up -d
```

Then verify:

```bash
docker compose ps
```

and logs:

```bash
docker compose logs --tail=50
```

This validates that the complete environment can start together.

---

# 65. Test Strategy for This Project

The final testing strategy is:

```text
                     Tests
                       |
          ┌────────────┴────────────┐
          |                         |
      Unit Tests              Integration Tests
          |                         |
      JUnit 5                   Testcontainers
          |                         |
      Mockito              ┌────────┴────────┐
          |                 |                 |
     Business Logic     PostgreSQL          Kafka
          |                 |                 |
          └─────────────────┴─────────────────┘
                            |
                       Distributed Flow
                            |
                     Saga + Compensation
```

---

# 66. Recommended Test Layers

## Layer 1 — Unit Tests

Use:

```text
JUnit
Mockito
```

Test:

```text
Services
Business Rules
Exception Paths
```

---

## Layer 2 — Integration Tests

Use:

```text
Spring Boot Test
Testcontainers
PostgreSQL
Kafka
```

Test:

```text
Database
Kafka
Repositories
Consumers
Outbox
```

---

## Layer 3 — API/Security Tests

Test:

```text
HTTP APIs
Validation
JWT
Roles
Error responses
```

---

## Layer 4 — Distributed Flow Tests

Test:

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

and:

```text
Failure
 ↓
Compensation
 ↓
Final state
```

---

# 67. What Testing Provides

The testing strategy provides confidence in:

```text
Business Logic
       ↓
Database
       ↓
Kafka
       ↓
Distributed Transactions
       ↓
Failure Recovery
       ↓
Security
```

This is particularly important because bugs in distributed systems can be difficult to reproduce manually.

---

# 68. Interview Explanation

A strong interview answer:

> "I used a layered testing strategy for the distributed e-commerce platform. For unit testing, I used JUnit 5 and Mockito to test service-layer business logic in isolation. For integration testing, I used Testcontainers with real PostgreSQL and Kafka containers to verify database operations and event-driven communication. I also tested the Transactional Outbox, Kafka consumers, idempotency, optimistic locking, Saga compensation, and failure scenarios. For example, I verified that duplicate inventory events do not reduce stock twice and that when payment fails, the reserved inventory is released and the order is eventually cancelled."

---

# 69. Common Interview Questions

### Q1. Why use Mockito?

To isolate the class being tested from its dependencies.

### Q2. Unit test vs integration test?

A unit test tests a component in isolation, usually with mocked dependencies.

An integration test verifies that multiple components or real infrastructure work together.

### Q3. Why Testcontainers?

It provides real infrastructure such as PostgreSQL and Kafka in isolated containers during tests.

### Q4. Why not mock PostgreSQL?

Mocking PostgreSQL does not validate actual SQL, schema, transactions, constraints, or database behavior.

### Q5. Why test Kafka with a real broker?

Because producer/consumer configuration, serialization, topics, offsets, and broker behavior cannot be fully validated with simple mocks.

### Q6. How do you test duplicate Kafka events?

Send the same event more than once and verify that the business operation occurs only once.

### Q7. How do you test Saga compensation?

Trigger a failure such as payment failure and verify that the corresponding compensating action is executed.

Example:

```text
payment.failed
      ↓
inventory.release
      ↓
inventory.released
      ↓
order.cancel
```

### Q8. What is Testcontainers?

A Java testing library that runs disposable Docker containers for integration testing.

### Q9. How do you test optimistic locking?

Simulate concurrent updates and verify that a stale transaction receives an optimistic locking failure.

### Q10. How do you test the Transactional Outbox?

Create an order and verify that both the order and its corresponding outbox event are persisted correctly in the same database transaction.

---

# 70. Final Testing Mental Model

Remember the project testing strategy like this:

```text
                    CODE
                      |
              ┌───────┴───────┐
              |               |
           UNIT            INTEGRATION
              |               |
        JUnit + Mockito   Testcontainers
                              |
                       ┌──────┴──────┐
                       |             |
                   PostgreSQL       Kafka
                       |             |
                       └──────┬──────┘
                              |
                       Distributed Flow
                              |
                  ┌───────────┴───────────┐
                  |                       |
                SUCCESS                 FAILURE
                  |                       |
             Order Confirmed         Compensation
                                          |
                                     Order Cancelled
```

The key testing concepts are:

```text
JUnit 5
   ↓
Unit Testing

Mockito
   ↓
Mock Dependencies

Testcontainers
   ↓
Real Infrastructure

PostgreSQL
   ↓
Database Integration

Kafka
   ↓
Event Integration

Idempotency
   ↓
Duplicate Event Safety

Optimistic Locking
   ↓
Concurrent Update Safety

Transactional Outbox
   ↓
Reliable Event Publishing

Saga Testing
   ↓
Distributed Failure Recovery

JWT Testing
   ↓
Authentication & Authorization
```

This testing approach gives the project coverage across both **individual microservice logic** and the **distributed system as a whole**.
