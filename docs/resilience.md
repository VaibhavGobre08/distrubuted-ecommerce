# Resilience and Fault Tolerance

## 1. Overview

In a microservices architecture, one service can fail while the other services are still running.

For example:

```text
Client
  ↓
API Gateway
  ↓
Order Service
  ↓
Order Service is DOWN ❌
```

If the Gateway continuously sends requests to the unavailable Order Service, it can cause:

* Request timeouts
* Thread/resource exhaustion
* Slow responses
* Cascading failures
* Poor user experience

The Distributed E-Commerce Platform uses **Resilience4j** to improve fault tolerance.

The main resilience mechanisms are:

```text
Circuit Breaker
Retry
TimeLimiter
Fallback
```

---

# 2. Why Resilience Is Required

Consider this architecture:

```text
Client
   ↓
API Gateway
   ↓
Order Service
   ↓
Inventory Service
   ↓
Payment Service
```

Suppose Payment Service becomes unavailable.

Without resilience:

```text
100 requests
     ↓
Payment Service
     ↓
100 timeouts
```

The upstream services keep waiting.

This can eventually affect the entire application.

With resilience:

```text
Payment Service unavailable
          ↓
Circuit Breaker
          ↓
Stop unnecessary requests
          ↓
Fast fallback response
```

This prevents failures from spreading.

---

# 3. Resilience4j

The project uses **Resilience4j**.

Resilience4j is a lightweight fault-tolerance library designed for Java applications.

It provides modules for:

```text
Circuit Breaker
Retry
Rate Limiter
Bulkhead
TimeLimiter
```

The project primarily focuses on:

```text
Circuit Breaker
Retry
TimeLimiter
Fallback
```

---

# 4. Resilience Architecture

The basic flow is:

```text
Client
  ↓
API Gateway
  ↓
Circuit Breaker
  ↓
Order Service
```

If the Order Service is healthy:

```text
Request
  ↓
Circuit Breaker
  ↓
Order Service
  ↓
Response
```

If the Order Service repeatedly fails:

```text
Request
  ↓
Circuit Breaker
  ↓
Fallback
```

---

# 5. Circuit Breaker

A Circuit Breaker protects the application from repeatedly calling a failing service.

It works similarly to an electrical circuit breaker.

When too many failures occur:

```text
Circuit CLOSED
      ↓
Too many failures
      ↓
Circuit OPEN
      ↓
Stop requests
```

After some time:

```text
OPEN
 ↓
HALF_OPEN
 ↓
Test request
 ↓
Success
 ↓
CLOSED
```

---

# 6. Circuit Breaker States

A Circuit Breaker has three important states.

```text
CLOSED
  ↓
OPEN
  ↓
HALF_OPEN
  ↓
CLOSED
```

---

# 7. CLOSED State

The normal state is:

```text
CLOSED
```

Requests are allowed to reach the service.

```text
Client
  ↓
Circuit Breaker
  ↓
Order Service
  ↓
Response
```

The Circuit Breaker monitors the requests.

It records information such as:

```text
Successful calls
Failed calls
Failure rate
```

---

# 8. OPEN State

If the failure rate crosses the configured threshold:

```text
CLOSED
   ↓
Failure threshold exceeded
   ↓
OPEN
```

In the OPEN state, requests are not sent to the failing service.

Instead:

```text
Client
  ↓
Circuit Breaker
  ↓
Fallback
```

This prevents repeated calls to an unhealthy service.

---

# 9. HALF_OPEN State

After the configured waiting period, the Circuit Breaker enters:

```text
HALF_OPEN
```

It allows a limited number of test requests.

For example:

```text
HALF_OPEN
    ↓
3 test requests
    ↓
Service healthy?
```

If successful:

```text
HALF_OPEN
    ↓
CLOSED
```

If failures continue:

```text
HALF_OPEN
    ↓
OPEN
```

---

# 10. Circuit Breaker State Diagram

```text
                  failures exceed threshold
           ┌─────────────────────────────────┐
           │                                 │
           ▼                                 │
       ┌────────┐                        ┌──────┐
       │ CLOSED │ ─────────────────────► │ OPEN │
       └────────┘                        └───┬──┘
           ▲                                │
           │                                │ wait duration
           │                                ▼
           │                           ┌───────────┐
           │                           │ HALF_OPEN │
           │                           └─────┬─────┘
           │                                 │
           │              ┌──────────────────┴──────────────┐
           │              │                                 │
        success         success                         failure
           │              │                                 │
           └──────────────┘                                 │
                                                          ┌──▼──┐
                                                          │OPEN │
                                                          └─────┘
```

---

# 11. Circuit Breaker in This Project

The API Gateway protects the Order Service.

Conceptually:

```text
Client
  ↓
API Gateway :8080
  ↓
Circuit Breaker
  ↓
Order Service :8081
```

If Order Service becomes unavailable:

```text
API Gateway
      ↓
Circuit Breaker
      ↓
Order Service ❌
```

The Circuit Breaker eventually opens.

Further requests are sent to:

```text
/fallback/order
```

---

# 12. Resilience4j Dependency

The API Gateway uses the Spring Cloud Circuit Breaker integration:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

This is appropriate because the API Gateway uses the reactive Spring Cloud Gateway stack.

---

# 13. Circuit Breaker Configuration

The Order Service Circuit Breaker can be configured using:

```properties
resilience4j.circuitbreaker.instances.order-service.register-health-indicator=true

resilience4j.circuitbreaker.instances.order-service.sliding-window-size=10

resilience4j.circuitbreaker.instances.order-service.minimum-number-of-calls=5

resilience4j.circuitbreaker.instances.order-service.failure-rate-threshold=50

resilience4j.circuitbreaker.instances.order-service.wait-duration-in-open-state=10s

resilience4j.circuitbreaker.instances.order-service.permitted-number-of-calls-in-half-open-state=3
```

---

# 14. Configuration Explanation

## Register Health Indicator

```properties
register-health-indicator=true
```

Allows the Circuit Breaker state to be exposed through Spring Boot Actuator health information.

---

## Sliding Window Size

```properties
sliding-window-size=10
```

The Circuit Breaker evaluates the recent 10 calls.

Example:

```text
10 recent calls
       ↓
5 failed
5 successful
       ↓
50% failure rate
```

---

## Minimum Number of Calls

```properties
minimum-number-of-calls=5
```

The Circuit Breaker waits until at least 5 calls have been recorded before calculating the failure rate.

This prevents the circuit from opening based on a very small sample.

---

## Failure Rate Threshold

```properties
failure-rate-threshold=50
```

If the failure rate reaches 50%, the Circuit Breaker can transition to OPEN.

Example:

```text
10 calls
5 failures
5 successes

Failure Rate = 50%
```

---

## Wait Duration

```properties
wait-duration-in-open-state=10s
```

After opening, the Circuit Breaker remains OPEN for 10 seconds before moving toward HALF_OPEN.

---

## Half-Open Calls

```properties
permitted-number-of-calls-in-half-open-state=3
```

When HALF_OPEN, up to 3 test calls can be allowed.

If these succeed, the Circuit Breaker can close.

---

# 15. Fallback

A fallback provides a controlled response when the downstream service is unavailable.

Example:

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/order")
    public ResponseEntity<String> orderServiceFallback() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Order service is temporarily unavailable. Please try again later.");
    }
}
```

Instead of exposing an internal error or timeout to the user, the Gateway returns:

```text
HTTP 503 Service Unavailable
```

with a clear message.

---

# 16. Order Route with Circuit Breaker

The Order route can use:

```properties
spring.cloud.gateway.routes[0].id=order-service
spring.cloud.gateway.routes[0].uri=http://order-service:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/orders/**
spring.cloud.gateway.routes[0].filters[0]=CircuitBreaker=order-service,fallbackUri=forward:/fallback/order
```

The flow becomes:

```text
/api/orders/**
      ↓
API Gateway
      ↓
Circuit Breaker
      ↓
Order Service
```

If the Circuit Breaker opens:

```text
/api/orders/**
      ↓
Circuit Breaker OPEN
      ↓
/fallback/order
```

---

# 17. Retry

Circuit Breaker and Retry solve different problems.

Retry means:

> Try the operation again because the failure may be temporary.

Example:

```text
Request
   ↓
Failure
   ↓
Retry
   ↓
Success
```

For example:

```text
Attempt 1 → timeout
Attempt 2 → timeout
Attempt 3 → success
```

---

# 18. Why Retry Is Useful

Some failures are temporary:

```text
Network timeout
Temporary connection failure
Short-lived service overload
```

Retrying can allow the request to succeed without requiring user intervention.

---

# 19. Retry vs Circuit Breaker

These concepts are often asked together in interviews.

### Retry

```text
Failure
  ↓
Try again
```

### Circuit Breaker

```text
Repeated failures
  ↓
Stop calling service
```

Together:

```text
Temporary failure
      ↓
Retry
      ↓
Still failing
      ↓
Circuit Breaker
      ↓
OPEN
      ↓
Fallback
```

---

# 20. Retry Example

Suppose Payment Service temporarily fails.

```text
payment.process
      ↓
Failure
      ↓
Retry
      ↓
Failure
      ↓
Retry
      ↓
Success
```

The Saga can continue.

But if the dependency remains unavailable:

```text
Retry
  ↓
Retry
  ↓
Retry
  ↓
Circuit Breaker opens
```

---

# 21. Avoiding Excessive Retry

Retry must be configured carefully.

Bad design:

```text
Retry forever
```

This can increase load on an already unhealthy service.

Better:

```text
Limited retries
+
Backoff
+
Circuit Breaker
```

For example:

```text
Attempt 1
   ↓
wait
   ↓
Attempt 2
   ↓
wait longer
   ↓
Attempt 3
   ↓
Stop
```

---

# 22. Exponential Backoff

Instead of retrying immediately:

```text
Retry
Retry
Retry
```

we can progressively increase the delay:

```text
Retry 1 → 1 second
Retry 2 → 2 seconds
Retry 3 → 4 seconds
```

This is called exponential backoff.

It reduces pressure on an unhealthy service.

---

# 23. TimeLimiter

A TimeLimiter prevents an operation from waiting indefinitely.

Example:

```text
Gateway
   ↓
Order Service
   ↓
Waiting...
   ↓
Waiting...
   ↓
Waiting...
```

Without a timeout, resources may remain occupied for too long.

With a TimeLimiter:

```text
Request
   ↓
Maximum allowed duration
   ↓
Timeout
   ↓
Fallback
```

---

# 24. Why Timeouts Matter

Suppose Order Service becomes extremely slow.

```text
Request
  ↓
Order Service
  ↓
30 seconds
  ↓
60 seconds
```

Many requests can accumulate.

Eventually:

```text
Threads / connections / resources exhausted
```

A timeout limits how long the upstream service waits.

---

# 25. Circuit Breaker + Retry + Timeout

These mechanisms solve different problems.

```text
Retry
  ↓
Temporary failure

TimeLimiter
  ↓
Slow response

Circuit Breaker
  ↓
Repeated failures
```

Together:

```text
                 Request
                    |
                    v
                 Retry
                    |
                    v
               TimeLimiter
                    |
                    v
             Circuit Breaker
                    |
                    v
              Downstream
                    |
          ┌─────────┴─────────┐
          │                   │
       Success              Failure
          │                   │
          v                   v
       Response             Retry
                              |
                         too many failures
                              |
                              v
                       Circuit OPEN
                              |
                              v
                           Fallback
```

---

# 26. Cascading Failure

A cascading failure happens when one failing service causes other services to become unhealthy.

Example:

```text
Payment Service
      ↓
Very slow
      ↓
Order Service waits
      ↓
Gateway waits
      ↓
More requests accumulate
      ↓
System becomes overloaded
```

This is dangerous in microservices.

---

# 27. Preventing Cascading Failure

The project can reduce cascading failures using:

```text
Circuit Breaker
Retry
Timeout
Fallback
Bulkhead
```

The current resilience focus is:

```text
Circuit Breaker
Retry
TimeLimiter
Fallback
```

Bulkhead can be added later if required.

---

# 28. Failure Scenario — Order Service Down

Suppose:

```text
Order Service = DOWN
```

Client sends:

```http
GET /api/orders/101
```

Without Circuit Breaker:

```text
Client
 ↓
Gateway
 ↓
Order Service ❌
 ↓
Timeout
```

Repeated requests continue doing the same thing.

---

# 29. With Circuit Breaker

Initially:

```text
CLOSED
```

Requests are sent normally.

After repeated failures:

```text
CLOSED
   ↓
Failure threshold
   ↓
OPEN
```

Now:

```text
Client
 ↓
Gateway
 ↓
Circuit Breaker OPEN
 ↓
Fallback
```

The Gateway does not repeatedly call the unavailable service.

---

# 30. Recovery

Suppose Order Service becomes healthy again.

After:

```text
wait-duration-in-open-state
```

the Circuit Breaker moves to:

```text
HALF_OPEN
```

A limited number of requests are allowed.

If:

```text
3 test requests
3 successful
```

then:

```text
HALF_OPEN
    ↓
CLOSED
```

Normal traffic resumes.

---

# 31. Failure Scenario — Temporary Network Problem

Suppose the service is healthy but one request experiences a temporary network problem.

```text
Request
  ↓
Network failure
  ↓
Retry
  ↓
Success
```

The Circuit Breaker does not necessarily need to open.

This is why Retry and Circuit Breaker complement each other.

---

# 32. Failure Scenario — Slow Service

Suppose:

```text
Order Service
    ↓
Response takes 30 seconds
```

The TimeLimiter can terminate the waiting operation after the configured timeout.

Conceptually:

```text
Request
  ↓
TimeLimiter
  ↓
Timeout exceeded
  ↓
Failure / Fallback
```

This protects upstream resources.

---

# 33. Failure Scenario — Persistent Failure

Suppose every request fails:

```text
Request 1 → FAILED
Request 2 → FAILED
Request 3 → FAILED
Request 4 → FAILED
Request 5 → FAILED
```

Once the configured failure threshold is reached:

```text
Circuit OPEN
```

Further requests fail fast.

---

# 34. Fail Fast

One major benefit of a Circuit Breaker is **fail fast**.

Without Circuit Breaker:

```text
Request
 ↓
Wait 30 seconds
 ↓
Timeout
```

With Circuit Breaker OPEN:

```text
Request
 ↓
Circuit Breaker
 ↓
Fallback immediately
```

This improves response time during outages.

---

# 35. Resilience and Saga

Resilience is especially important for the Saga workflow.

Example:

```text
Order
 ↓
Inventory
 ↓
Payment
```

If Payment is unavailable, the system should not continuously send requests forever.

Instead:

```text
Payment unavailable
       ↓
Retry
       ↓
Still unavailable
       ↓
Failure handling
       ↓
Compensation
```

The exact retry/compensation behavior depends on whether the failure is temporary or permanent.

---

# 36. Resilience and Kafka

Kafka also provides resilience through:

```text
Durable events
Consumer groups
Retries
Offsets
DLTs
```

The architecture therefore has multiple resilience layers:

```text
                  Application
                      |
             ┌────────┴────────┐
             ↓                 ↓
        Resilience4j         Kafka
             |                 |
      ┌──────┼──────┐      ┌───┼───┐
      ↓      ↓      ↓      ↓   ↓   ↓
   Retry  Timeout Circuit  Retry Offset DLT
                  Breaker
```

---

# 37. Resilience Layers in This Project

The project uses or plans the following reliability mechanisms:

| Layer       | Mechanism            | Purpose                            |
| ----------- | -------------------- | ---------------------------------- |
| API Gateway | Circuit Breaker      | Protect downstream services        |
| API Gateway | Fallback             | Return controlled response         |
| Service     | Retry                | Handle temporary failures          |
| Service     | TimeLimiter          | Prevent long waits                 |
| Kafka       | Consumer retry       | Handle processing failures         |
| Kafka       | DLT                  | Isolate permanently failing events |
| Database    | Transactional Outbox | Prevent event loss                 |
| Inventory   | Idempotency          | Prevent duplicate reservations     |
| Inventory   | Optimistic Locking   | Protect concurrent stock updates   |
| Saga        | Compensation         | Recover from business failures     |

---

# 38. Resilience vs Compensation

These are different concepts.

### Resilience

Handles technical failures.

Examples:

```text
Network timeout
Service unavailable
Temporary connection failure
```

Mechanisms:

```text
Retry
Circuit Breaker
TimeLimiter
```

### Compensation

Handles business workflow failure.

Example:

```text
Payment Failed
      ↓
Release Inventory
      ↓
Cancel Order
```

Mechanism:

```text
Saga Compensation
```

---

# 39. Important Difference

Remember:

```text
Retry ≠ Compensation
```

Retry says:

> The operation may succeed if I try again.

Compensation says:

> A previous business operation succeeded, but the overall workflow failed, so I need to reverse its business effect.

---

# 40. Monitoring Circuit Breakers

Spring Boot Actuator can expose health information.

Conceptually:

```text
Actuator
   ↓
Circuit Breaker Metrics
   ↓
Prometheus
   ↓
Grafana
```

Useful metrics include:

```text
Number of successful calls
Number of failed calls
Failure rate
Circuit state
Slow calls
```

Since monitoring is already implemented in the project, these metrics can be used to visualize resilience behavior.

---

# 41. Testing Circuit Breaker

A basic test scenario:

### Step 1

Stop Order Service.

```text
Order Service = DOWN
```

### Step 2

Send multiple requests:

```http
GET /api/orders/1
GET /api/orders/1
GET /api/orders/1
...
```

### Step 3

Observe failures.

After the threshold:

```text
Circuit = OPEN
```

### Step 4

Send another request.

It should reach:

```text
/fallback/order
```

and return:

```text
503 Service Unavailable
```

---

# 42. Testing Recovery

Start Order Service again.

Wait for:

```text
wait-duration-in-open-state
```

The Circuit Breaker moves toward:

```text
HALF_OPEN
```

Send test requests.

If successful:

```text
HALF_OPEN
     ↓
CLOSED
```

Normal requests resume.

---

# 43. Testing Retry

Simulate a temporary failure.

Expected:

```text
Attempt 1 → failure
Attempt 2 → failure
Attempt 3 → success
```

Verify that the operation eventually succeeds without requiring another client request.

---

# 44. Testing TimeLimiter

Create a deliberately slow downstream operation.

For example:

```text
Order Service
    ↓
sleep / delayed response
```

If the response exceeds the configured limit:

```text
TimeLimiter
     ↓
Timeout
     ↓
Fallback / Error
```

The Gateway should not wait indefinitely.

---

# 45. Production Considerations

In a production environment, resilience settings should be based on real traffic.

Avoid blindly using:

```text
Retry = 10
Timeout = 60 seconds
```

Instead consider:

```text
Expected latency
Traffic volume
Failure rate
Service capacity
Business requirements
```

For example, an interactive API may need:

```text
Timeout = short
Retries = limited
Fallback = fast
```

while a background operation may tolerate longer processing.

---

# 46. What Circuit Breaker Does Not Solve

Circuit Breaker does not fix the underlying service.

If:

```text
Order Service
     ↓
Database
     ↓
Database unavailable
```

the Circuit Breaker only protects callers from repeatedly hitting the failing service.

The underlying problem still needs to be investigated.

---

# 47. Common Mistakes

### Mistake 1 — Retry Forever

Bad:

```text
while failure:
    retry()
```

This can overload the system.

---

### Mistake 2 — Very Long Timeout

A 5-minute timeout can still consume resources.

---

### Mistake 3 — Circuit Breaker Without Monitoring

You should know:

```text
Why did it open?
How long was it open?
Did recovery succeed?
```

---

### Mistake 4 — Retrying Non-Idempotent Operations

Be careful retrying operations that can create duplicate business effects.

For example:

```text
Create Payment
Create Order
```

should be designed with idempotency before aggressive retries.

---

# 48. Interview Explanation

A strong interview answer:

> "I use Resilience4j to make the microservices architecture fault tolerant. At the API Gateway, I use a Circuit Breaker around downstream services. When failures cross the configured threshold, the circuit moves from CLOSED to OPEN and requests are failed fast using a fallback instead of continuously calling the unhealthy service. After a wait period, the circuit moves to HALF_OPEN and allows a few test requests to determine whether the service has recovered. Retry handles temporary failures, while TimeLimiter prevents requests from waiting indefinitely. Together these mechanisms help prevent cascading failures."

---

# 49. Common Interview Questions

### Q1. What is a Circuit Breaker?

A design pattern that prevents repeated calls to an unhealthy service after failures cross a configured threshold.

### Q2. What are the Circuit Breaker states?

```text
CLOSED
OPEN
HALF_OPEN
```

### Q3. What happens in CLOSED?

Requests are allowed normally and failures are monitored.

### Q4. What happens in OPEN?

Requests are rejected quickly and usually redirected to a fallback.

### Q5. What happens in HALF_OPEN?

A limited number of test requests are allowed to determine whether the service has recovered.

### Q6. What is Retry?

Retry attempts the failed operation again, usually for temporary failures.

### Q7. What is TimeLimiter?

It limits how long an operation is allowed to run before timing out.

### Q8. Retry vs Circuit Breaker?

Retry handles potentially temporary failures by trying again.

Circuit Breaker prevents repeated calls when failures are persistent.

### Q9. What is a fallback?

A controlled alternative response when the downstream operation cannot be completed.

### Q10. What is cascading failure?

A failure in one service causes dependent services to become overloaded or fail as well.

### Q11. Why shouldn't we retry everything?

Retries can increase load and can cause duplicate business operations if the operation is not idempotent.

### Q12. How does Circuit Breaker improve performance during an outage?

It fails fast instead of repeatedly waiting for an unavailable service.

---

# 50. Final Mental Model

Remember the resilience architecture as:

```text
                         REQUEST
                            |
                            v
                         RETRY
                            |
                            v
                       TIMELIMITER
                            |
                            v
                     CIRCUIT BREAKER
                            |
                    ┌───────┴────────┐
                    │                │
                 CLOSED             OPEN
                    │                │
                    v                v
              DOWNSTREAM          FALLBACK
                    │
              ┌─────┴─────┐
              │           │
           SUCCESS      FAILURE
              │           │
              v           v
           RESPONSE      RETRY
```

The important distinction is:

```text
Retry
→ Try again

TimeLimiter
→ Don't wait forever

Circuit Breaker
→ Stop calling an unhealthy service

Fallback
→ Return a controlled response

Saga Compensation
→ Reverse a completed business operation
```

Together, these patterns make the Distributed E-Commerce Platform more resilient:

```text
                    MICROSERVICES
                         |
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
       Kafka          Database       Gateway
          |              |              |
       Retry          Outbox       Circuit Breaker
          |              |              |
        DLT         Idempotency     TimeLimiter
          |              |              |
          └──────────────┼──────────────┘
                         ↓
                  Fault-Tolerant
                    Architecture
```

---

# 51. Key Takeaway

The main goal of resilience is not to prevent every failure.

Failures are expected in distributed systems.

The goal is to make sure:

```text
One service fails
      ↓
Other services remain available
      ↓
Requests fail gracefully
      ↓
Temporary failures can recover
      ↓
Business failures can be compensated
```

That is the purpose of:

```text
Resilience4j
+
Kafka
+
Transactional Outbox
+
Saga
+
Idempotency
+
Optimistic Locking
+
Retry
+
DLT
```

This combination provides the foundation for a reliable event-driven microservices system.
