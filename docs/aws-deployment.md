# AWS Deployment

## 1. Overview

The Distributed E-Commerce Platform is designed as a cloud-ready microservices application.

The application consists of multiple Spring Boot microservices communicating through REST APIs and Apache Kafka.

The application can be deployed to AWS using containerized services.

### Main AWS Services

| Application Requirement | AWS Service               |
| ----------------------- | ------------------------- |
| Docker images           | Amazon ECR                |
| Microservices           | Amazon ECS / Fargate      |
| PostgreSQL              | Amazon RDS                |
| Kafka                   | Amazon MSK                |
| Load balancing          | Application Load Balancer |
| Secrets                 | AWS Secrets Manager       |
| Logs                    | Amazon CloudWatch         |
| Monitoring              | Amazon CloudWatch         |
| DNS                     | Amazon Route 53           |
| HTTPS                   | AWS Certificate Manager   |
| IAM                     | AWS IAM                   |
| CI/CD                   | GitHub Actions            |

---

# 2. AWS Architecture

The recommended production architecture is:

```text
                         Internet
                            |
                            v
                    Route 53 / DNS
                            |
                            v
                 Application Load Balancer
                            |
                            v
                      API Gateway
                       ECS Service
                            |
          ┌─────────────────┼─────────────────┐
          |                 |                 |
          v                 v                 v
   Order Service      Inventory Service   Payment Service
      ECS/Fargate         ECS/Fargate        ECS/Fargate
          |                 |                 |
          └─────────────────┼─────────────────┘
                            |
                            v
                       Amazon MSK
                      Apache Kafka
                            |
                            v
                    Saga Orchestrator
                       ECS/Fargate
                            |
          ┌─────────────────┼──────────────────┐
          |                 |                  |
          v                 v                  v
     Shipping          Notification        Other Services
      Service             Service
          |
          v
     Amazon RDS
      PostgreSQL
```

Supporting infrastructure:

```text
                    AWS Cloud
                       |
          ┌────────────┼────────────┐
          |            |            |
          v            v            v
      CloudWatch   Secrets Manager  IAM
          |
          v
        Logs
```

---

# 3. Why AWS?

AWS provides managed infrastructure for deploying and operating distributed applications.

Instead of manually managing servers, the application can use managed AWS services.

For example:

* ECS manages containers.
* Fargate removes server management.
* RDS manages PostgreSQL.
* MSK manages Kafka infrastructure.
* CloudWatch manages logs and monitoring.
* ECR stores Docker images.

This allows developers to focus primarily on application development rather than infrastructure management.

---

# 4. Containerization

Every microservice is packaged as a Docker image.

Example:

```text
order-service
inventory-service
payment-service
shipping-service
auth-service
notification-service
saga-orchestrator
api-gateway
```

Each service has its own Docker image.

Example:

```text
order-service:1.0
inventory-service:1.0
payment-service:1.0
```

These images are pushed to Amazon ECR.

---

# 5. Amazon ECR

Amazon Elastic Container Registry (ECR) is used to store Docker images.

Example repository structure:

```text
AWS ECR
│
├── ecommerce/order-service
├── ecommerce/inventory-service
├── ecommerce/payment-service
├── ecommerce/shipping-service
├── ecommerce/auth-service
├── ecommerce/notification-service
├── ecommerce/saga-orchestrator
└── ecommerce/api-gateway
```

## Docker Build

Example:

```bash
cd order-service

./mvnw clean package -DskipTests

docker build -t order-service .
```

Tag the image:

```bash
docker tag order-service:latest \
<aws-account-id>.dkr.ecr.<region>.amazonaws.com/ecommerce/order-service:latest
```

Authenticate Docker with ECR:

```bash
aws ecr get-login-password --region <region> | \
docker login --username AWS \
--password-stdin <aws-account-id>.dkr.ecr.<region>.amazonaws.com
```

Push:

```bash
docker push \
<aws-account-id>.dkr.ecr.<region>.amazonaws.com/ecommerce/order-service:latest
```

The same process is followed for the other services.

---

# 6. Amazon ECS

Amazon Elastic Container Service (ECS) runs the Docker containers.

For this project, ECS with Fargate is recommended.

### Why Fargate?

Fargate is serverless container infrastructure.

We don't need to manage:

* EC2 servers
* operating system updates
* server provisioning
* container host maintenance

AWS manages the underlying infrastructure.

---

# 7. ECS Cluster

Create one ECS cluster:

```text
ecommerce-cluster
```

Inside the cluster:

```text
ECS Cluster
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

Each service runs as an ECS service.

---

# 8. ECS Task Definition

A task definition describes how a container should run.

Example:

```text
Container:
    order-service

Image:
    ECR/order-service:latest

CPU:
    0.5 vCPU

Memory:
    1 GB

Port:
    8081
```

Environment variables:

```text
SPRING_PROFILES_ACTIVE=aws

DB_URL=jdbc:postgresql://<rds-endpoint>:5432/order_db

KAFKA_BOOTSTRAP_SERVERS=<msk-bootstrap-server>

JWT_SECRET=<secret-from-secrets-manager>
```

---

# 9. Service Discovery

Microservices need to communicate with each other.

Inside AWS, services should communicate using private networking rather than public internet addresses.

Example:

```text
API Gateway
     |
     v
order-service:8081
     |
     v
inventory-service:8082
```

AWS Cloud Map or an internal load balancer can be used for service discovery.

For a production implementation, service discovery should be preferred over hard-coded public IP addresses.

---

# 10. Amazon RDS

The application uses PostgreSQL.

Instead of running PostgreSQL manually on EC2, use Amazon RDS for PostgreSQL.

Architecture:

```text
ECS
 |
 +---- order-service ------> RDS order_db
 |
 +---- inventory-service --> RDS inventory_db
 |
 +---- payment-service ----> RDS payment_db
 |
 +---- shipping-service ---> RDS shipping_db
 |
 +---- auth-service -------> RDS auth_db
 |
 +---- notification -------> RDS notification_db
```

The database-per-service architecture is maintained.

---

# 11. Database-per-Service

Each service owns its database.

```text
Order Service
     |
     +--> order_db

Inventory Service
     |
     +--> inventory_db

Payment Service
     |
     +--> payment_db

Shipping Service
     |
     +--> shipping_db

Auth Service
     |
     +--> auth_db

Notification Service
     |
     +--> notification_db
```

One service should not directly modify another service's database.

Communication between services happens through APIs or Kafka events.

---

# 12. Amazon MSK

The project uses Apache Kafka for asynchronous communication.

AWS Managed Streaming for Apache Kafka (Amazon MSK) can be used.

Architecture:

```text
Order Service
      |
      v
   Kafka/MSK
      |
      v
Saga Orchestrator
      |
      +----> Inventory
      |
      +----> Payment
      |
      +----> Shipping
```

Kafka topics remain conceptually the same:

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

---

# 13. Kafka Configuration

Local development:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

Docker:

```properties
spring.kafka.bootstrap-servers=kafka:29092
```

AWS:

```properties
spring.kafka.bootstrap-servers=<MSK-bootstrap-server>
```

The application should receive this value through an environment variable.

Example:

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
```

This avoids hard-coding environment-specific infrastructure.

---

# 14. VPC

AWS resources should be placed inside a VPC.

Example:

```text
                         VPC
                          |
          ┌───────────────┴───────────────┐
          |                               |
     Public Subnets                 Private Subnets
          |                               |
          v                               |
   Application Load                     ECS
      Balancer                           |
                                          +---- RDS
                                          |
                                          +---- MSK
```

### Public Subnet

The Application Load Balancer can be placed in public subnets.

### Private Subnet

Application containers, databases and Kafka should generally remain private.

---

# 15. Security Groups

Security Groups control network traffic.

Example:

```text
Internet
   |
   v
ALB Security Group
   |
   v
ECS Security Group
   |
   ├── RDS Security Group
   |
   └── MSK Security Group
```

Example rules:

### ALB

Allow:

```text
80
443
```

from the internet.

### ECS

Allow application ports only from the ALB or internal services.

### RDS

Allow:

```text
5432
```

only from ECS services that require database access.

### Kafka/MSK

Allow Kafka traffic only from authorized application services.

---

# 16. Application Load Balancer

The Application Load Balancer provides a public entry point.

```text
Client
  |
  v
https://api.example.com
  |
  v
Application Load Balancer
  |
  v
API Gateway
```

The ALB performs health checks and routes traffic to healthy targets.

---

# 17. API Gateway

The existing Spring Cloud Gateway remains responsible for application-level routing.

Example:

```text
/api/auth/**          → Auth Service
/api/orders/**        → Order Service
/api/products/**      → Inventory Service
/api/payment/**       → Payment Service
/api/shipping/**      → Shipping Service
/api/notifications/** → Notification Service
```

AWS ALB handles infrastructure-level traffic.

Spring Cloud Gateway handles application-level routing and JWT authorization.

---

# 18. JWT Secret

The JWT secret should never be committed to Git.

Local:

```env
JWT_SECRET=local-secret
```

AWS:

```text
AWS Secrets Manager
        |
        v
    JWT_SECRET
        |
        v
    ECS Task
```

The application reads:

```properties
jwt.secret=${JWT_SECRET}
```

This keeps sensitive credentials outside the source code.

---

# 19. AWS Secrets Manager

Secrets that can be stored in Secrets Manager include:

```text
JWT_SECRET
DATABASE_PASSWORD
KAFKA_CREDENTIALS
OTHER_API_KEYS
```

Example:

```text
ecommerce/prod/jwt-secret
ecommerce/prod/database
ecommerce/prod/kafka
```

ECS tasks can retrieve secrets through IAM permissions.

---

# 20. IAM

IAM controls AWS permissions.

The ECS application should not use an AWS root account.

Instead, create IAM roles.

Example:

```text
ECS Task Execution Role
        |
        +--> Pull images from ECR
        +--> Write logs to CloudWatch

ECS Task Role
        |
        +--> Read Secrets Manager
        +--> Access required AWS resources
```

Use least-privilege permissions.

---

# 21. Environment Configuration

The application should support separate environments.

```text
application-local.properties
application-docker.properties
application-aws.properties
```

Example:

### Local

```properties
DB_URL=jdbc:postgresql://localhost:5432/order_db
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Docker

```properties
DB_URL=jdbc:postgresql://postgres:5432/order_db
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

### AWS

```properties
DB_URL=jdbc:postgresql://<rds-endpoint>:5432/order_db
KAFKA_BOOTSTRAP_SERVERS=<msk-endpoint>
```

The Java application code remains unchanged.

---

# 22. CloudWatch Logging

Each ECS service sends application logs to CloudWatch.

Example:

```text
CloudWatch
│
├── /ecs/api-gateway
├── /ecs/order-service
├── /ecs/inventory-service
├── /ecs/payment-service
├── /ecs/shipping-service
├── /ecs/auth-service
├── /ecs/notification-service
└── /ecs/saga-orchestrator
```

Logs can be used to investigate:

* application errors
* Kafka failures
* database errors
* authentication failures
* slow requests
* deployment issues

---

# 23. Monitoring

CloudWatch can monitor:

```text
CPU Utilization
Memory Utilization
Request Count
HTTP 4xx
HTTP 5xx
Container Restarts
Database Connections
Kafka Metrics
```

Application-level metrics can also be exposed through Spring Boot Actuator.

Prometheus/Grafana can be added for more advanced monitoring.

---

# 24. Health Checks

Spring Boot Actuator provides health endpoints.

Example:

```text
/actuator/health
```

Response:

```json
{
  "status": "UP"
}
```

ECS can use health checks to determine whether a container is healthy.

Example:

```text
ECS
 |
 +--> Container
        |
        +--> /actuator/health
```

If the service becomes unhealthy, ECS can replace the task.

---

# 25. Auto Scaling

ECS services can scale horizontally.

Example:

```text
Normal Traffic

Order Service
   |
   +--> Task 1


High Traffic

Order Service
   |
   +--> Task 1
   +--> Task 2
   +--> Task 3
```

Scaling can be based on:

```text
CPU
Memory
Request count
Custom CloudWatch metrics
```

For example:

```text
CPU > 70%
      |
      v
Increase ECS task count
```

---

# 26. CI/CD Pipeline

GitHub Actions can automate deployment.

Complete flow:

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
    +--> Compile
    |
    +--> Unit Tests
    |
    +--> Integration Tests
    |
    +--> Testcontainers
    |
    +--> Maven Package
    |
    +--> Docker Build
    |
    +--> Docker Scan
    |
    +--> Push to ECR
    |
    +--> Update ECS
    |
    v
AWS ECS
```

---

# 27. Example CI/CD Deployment

Example GitHub Actions stages:

```yaml
name: Build and Deploy

on:
  push:
    branches:
      - main

jobs:

  build:
    runs-on: ubuntu-latest

    steps:

      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Build
        run: mvn clean package

      - name: Run Tests
        run: mvn test

      - name: Build Docker Image
        run: docker build -t order-service ./order-service
```

The production workflow would then:

```text
Docker Build
     ↓
ECR Login
     ↓
Docker Push
     ↓
ECS Deployment
     ↓
Health Check
     ↓
Smoke Test
```

---

# 28. GitHub Secrets

Never put AWS credentials directly into the workflow.

Use GitHub Secrets or, preferably, GitHub OIDC with an AWS IAM role.

Potential configuration:

```text
AWS_REGION
ECR_REPOSITORY
ECS_CLUSTER
ECS_SERVICE
```

For production, avoid long-lived AWS access keys when OIDC can be used.

---

# 29. Deployment Strategy

A deployment should follow:

```text
Build
  ↓
Test
  ↓
Create Docker Image
  ↓
Push Image
  ↓
Deploy New ECS Task
  ↓
Health Check
  ↓
Traffic Shift
  ↓
Old Task Removed
```

This minimizes downtime.

---

# 30. Rolling Deployment

ECS can perform rolling deployments.

Example:

```text
Before:

Task v1
Task v1
Task v1


Deployment:

Task v1
Task v1
Task v2


After:

Task v2
Task v2
Task v2
```

If the new version becomes unhealthy, deployment can be stopped or rolled back.

---

# 31. Database Migration

Database schema changes should not depend on manually running SQL commands in production.

A migration framework such as Flyway or Liquibase can be introduced.

Example:

```text
Application
    |
    v
Flyway
    |
    v
RDS PostgreSQL
```

Migration files:

```text
V1__create_orders.sql
V2__add_order_status.sql
V3__create_outbox.sql
```

This gives the database a controlled migration history.

---

# 32. Domain and HTTPS

A production API can use a domain:

```text
https://api.example.com
```

Route 53 manages DNS.

AWS Certificate Manager provides the TLS certificate.

Flow:

```text
Client
  |
  v
api.example.com
  |
  v
Route 53
  |
  v
ALB
  |
  v
HTTPS
  |
  v
API Gateway
```

HTTPS should be used for production traffic.

---

# 33. AWS Failure Handling

The architecture should continue to handle distributed failures.

Example:

```text
Order
  |
  v
Inventory
  |
  v
Payment
  |
  X
Payment Failed
```

Saga compensation:

```text
Payment Failed
      |
      v
inventory.release
      |
      v
Inventory releases stock
      |
      v
inventory.released
      |
      v
Order CANCELLED
      |
      v
Notification
```

AWS infrastructure does not replace application-level compensation.

The Saga pattern remains responsible for business transaction consistency.

---

# 34. Kafka Failure

If Kafka temporarily becomes unavailable:

```text
Service
   |
   X
Kafka unavailable
```

The Transactional Outbox protects the event.

The event remains in:

```text
outbox_event
```

The publisher retries later.

After repeated failures:

```text
Retry
  ↓
Retry
  ↓
Retry
  ↓
DLT
```

This prevents an event from being lost simply because Kafka was temporarily unavailable.

---

# 35. Database Failure

If RDS becomes temporarily unavailable:

```text
Application
     |
     X
    RDS
```

The service should:

* fail gracefully
* retry where appropriate
* expose unhealthy status
* log the error
* avoid corrupting business state

Database backups and Multi-AZ configuration should be considered for production.

---

# 36. Circuit Breaker

Resilience4j protects synchronous communication.

Example:

```text
Order Service
     |
     v
Inventory Service
     |
     X
Inventory unavailable
```

Circuit breaker:

```text
CLOSED
   |
   | failures
   v
OPEN
   |
   | wait
   v
HALF_OPEN
   |
   +---- success → CLOSED
   |
   +---- failure → OPEN
```

This prevents cascading failures.

Kafka and Saga handle asynchronous business workflows, while Circuit Breaker protects synchronous calls.

---

# 37. Security Architecture

The production security flow:

```text
Client
   |
   | JWT
   v
ALB
   |
   v
API Gateway
   |
   v
JWT Validation
   |
   v
Spring Security
   |
   v
Microservice
```

Authentication:

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
```

Authorization:

```text
USER
  |
  +--> Normal APIs


ADMIN
  |
  +--> Product create/update/delete
```

---

# 38. AWS Network Architecture

Recommended structure:

```text
VPC
│
├── Public Subnet
│   ├── Application Load Balancer
│   └── NAT Gateway
│
├── Private Subnet
│   ├── ECS
│   ├── API Gateway
│   └── Saga
│
└── Private Database Subnet
    ├── RDS
    └── MSK
```

The database should not be publicly accessible.

---

# 39. Production Request Flow

A complete request looks like:

```text
User
 |
 | HTTPS
 v
Route 53
 |
 v
Application Load Balancer
 |
 v
API Gateway
 |
 | JWT validation
 v
Order Service
 |
 | Transaction
 +----> PostgreSQL
 |
 +----> Outbox
          |
          v
        Kafka
          |
          v
    Saga Orchestrator
          |
     ┌────┼────┐
     ↓    ↓    ↓
 Inventory Payment Shipping
     |
     v
 Kafka Events
     |
     v
Order Confirmation
     |
     v
Notification
```

---

# 40. AWS Deployment Steps

## Step 1 — Create AWS Account

Create an AWS account and configure billing alerts.

Do not use the root account for application deployment.

---

## Step 2 — Create IAM User/Role

Create appropriate IAM roles.

For GitHub Actions, prefer OIDC-based authentication.

---

## Step 3 — Create VPC

Create:

```text
VPC
Public Subnets
Private Subnets
Route Tables
Internet Gateway
NAT Gateway
Security Groups
```

---

## Step 4 — Create RDS

Create PostgreSQL RDS.

Create the required databases:

```text
order_db
inventory_db
payment_db
shipping_db
auth_db
notification_db
```

---

## Step 5 — Create MSK

Create the Kafka cluster.

Configure:

```text
Private networking
Security groups
Kafka authentication
Encryption
Monitoring
```

---

## Step 6 — Create ECR Repositories

Create repositories for every service:

```text
order-service
inventory-service
payment-service
shipping-service
auth-service
notification-service
saga-orchestrator
api-gateway
```

---

## Step 7 — Build Docker Images

For every service:

```bash
./mvnw clean package
docker build -t <service-name> .
```

---

## Step 8 — Push Images to ECR

Authenticate:

```bash
aws ecr get-login-password \
--region <region> | \
docker login \
--username AWS \
--password-stdin <ecr-url>
```

Push:

```bash
docker push <ecr-url>/<service>:latest
```

---

## Step 9 — Create ECS Cluster

Create:

```text
ecommerce-cluster
```

Use:

```text
AWS Fargate
```

---

## Step 10 — Create Task Definitions

Create task definitions for each service.

Example:

```text
order-task
inventory-task
payment-task
shipping-task
auth-task
notification-task
saga-task
gateway-task
```

Configure:

```text
CPU
Memory
Container Port
Environment Variables
Secrets
Logging
IAM Role
```

---

## Step 11 — Create ECS Services

Create ECS services from the task definitions.

Example:

```text
order-service
inventory-service
payment-service
shipping-service
auth-service
notification-service
saga-orchestrator
api-gateway
```

---

## Step 12 — Configure Service Discovery

Configure internal service communication.

Example:

```text
order-service
inventory-service
payment-service
shipping-service
```

should communicate through private AWS networking.

---

## Step 13 — Configure ALB

Expose only the API Gateway.

```text
Internet
   |
   v
ALB
   |
   v
API Gateway
```

Other microservices remain private.

---

## Step 14 — Configure Secrets

Move:

```text
JWT_SECRET
DB_PASSWORD
KAFKA credentials
```

to AWS Secrets Manager.

---

## Step 15 — Configure CloudWatch

Enable logging for every ECS service.

Verify:

```text
Application startup
Database connection
Kafka connection
HTTP requests
Exceptions
```

---

## Step 16 — Configure Health Checks

Use:

```text
/actuator/health
```

for application health.

---

## Step 17 — Configure Auto Scaling

Configure minimum and maximum task counts.

Example:

```text
Minimum: 2
Maximum: 6
```

The exact values depend on workload and cost.

---

## Step 18 — Configure CI/CD

GitHub:

```text
git push
   ↓
GitHub Actions
   ↓
Test
   ↓
Build
   ↓
Docker
   ↓
ECR
   ↓
ECS
```

---

# 41. Production Environment Variables

Example:

```text
SPRING_PROFILES_ACTIVE=aws

DB_URL=jdbc:postgresql://rds-endpoint:5432/order_db

DB_USERNAME=postgres

KAFKA_BOOTSTRAP_SERVERS=msk-endpoint

JWT_SECRET=<AWS_SECRET>
```

Passwords and secrets should come from Secrets Manager rather than plain environment configuration committed to Git.

---

# 42. AWS vs Local Environment

| Component     | Local       | Docker       | AWS                   |
| ------------- | ----------- | ------------ | --------------------- |
| Containers    | STS         | Docker       | ECS/Fargate           |
| PostgreSQL    | Local PG    | Docker PG    | RDS                   |
| Kafka         | Local Kafka | Docker Kafka | MSK                   |
| Images        | Local       | Local        | ECR                   |
| Load Balancer | Gateway     | Gateway      | ALB + Gateway         |
| Secrets       | `.env`      | `.env`       | Secrets Manager       |
| Logs          | Console     | Docker logs  | CloudWatch            |
| Monitoring    | Actuator    | Actuator     | CloudWatch + Actuator |
| DNS           | localhost   | localhost    | Route 53              |
| HTTPS         | Optional    | Optional     | ACM                   |

---

# 43. Why ECS Instead of EKS?

For this project, ECS/Fargate is the simpler AWS deployment choice.

EKS is powerful but introduces Kubernetes concepts:

```text
Pods
Deployments
Services
Ingress
ConfigMaps
Secrets
Nodes
Helm
Kubernetes networking
```

For a Java developer with approximately two years of experience, demonstrating:

```text
Spring Boot
Docker
Kafka
Saga
ECS
RDS
ECR
CloudWatch
CI/CD
```

already provides a strong cloud-native project.

EKS can be added later if Kubernetes is specifically required.

---

# 44. AWS Cost Considerations

AWS costs depend heavily on:

* region
* ECS task count
* CPU/memory
* RDS instance size
* MSK configuration
* NAT Gateway
* data transfer
* CloudWatch logs

For learning, avoid leaving production-sized infrastructure running continuously.

Important resources to monitor:

```text
ECS
RDS
MSK
NAT Gateway
Load Balancer
CloudWatch
```

Delete unused resources when they are no longer needed.

---

# 45. Production Improvements

Future improvements include:

* Multi-AZ deployment
* RDS read replicas
* Redis caching
* AWS ElastiCache
* MSK monitoring
* OpenTelemetry
* Distributed tracing
* WAF
* CloudFront
* Blue/green deployments
* Canary deployments
* Kubernetes/EKS
* Infrastructure as Code
* Terraform
* Database migrations
* Automated rollback
* Contract testing
* API versioning

---

# 46. Final AWS Architecture

```text
                              Internet
                                  |
                                  v
                            Route 53
                                  |
                                  v
                         Application LB
                                  |
                                  v
                           API Gateway
                           ECS/Fargate
                                  |
       ┌──────────────────────────┼──────────────────────────┐
       |             |            |            |             |
       v             v            v            v             v
    Order        Inventory     Payment      Shipping       Auth
    Service       Service      Service       Service      Service
       |             |            |            |             |
       └─────────────┴────────────┴────────────┴─────────────┘
                                  |
                                  v
                             Amazon MSK
                              Kafka
                                  |
                                  v
                         Saga Orchestrator
                              ECS
                                  |
                    ┌─────────────┴─────────────┐
                    |                           |
                    v                           v
               Notification                 Compensation
                 Service                     Workflow
                    |
                    v
              Amazon RDS
               PostgreSQL

       Supporting Services
       ┌───────────────────────────────┐
       | ECR                           |
       | Secrets Manager               |
       | CloudWatch                    |
       | IAM                           |
       | ACM                           |
       | Route 53                      |
       └───────────────────────────────┘
```

---

# 47. Interview Explanation

If the interviewer asks:

**"How would you deploy your microservices application to AWS?"**

A good answer is:

> "I would containerize each Spring Boot microservice using Docker and push the images to Amazon ECR. I would deploy the containers using ECS with Fargate so I don't have to manage the underlying servers. PostgreSQL would be hosted on Amazon RDS and Kafka on Amazon MSK. An Application Load Balancer would expose the API Gateway, while the internal microservices would remain in private subnets. I would use Secrets Manager for JWT and database credentials, CloudWatch for logs and monitoring, and GitHub Actions for CI/CD. The Saga and Kafka-based event-driven architecture would remain the same after deployment."

---

# 48. Common AWS Interview Questions

### Q1. Why use ECS?

ECS provides managed container orchestration without requiring Kubernetes.

### Q2. Why use Fargate?

Fargate removes the need to manage EC2 instances for containers.

### Q3. Why use ECR?

ECR provides a private registry for Docker images.

### Q4. Why use RDS?

RDS provides managed PostgreSQL with backups, monitoring and high-availability options.

### Q5. Why use MSK?

MSK provides managed Apache Kafka infrastructure.

### Q6. Where do you store secrets?

AWS Secrets Manager.

### Q7. Where do you store logs?

CloudWatch Logs.

### Q8. How do services communicate?

Synchronous REST communication is used where immediate responses are required, while Kafka is used for asynchronous event-driven communication.

### Q9. How do you handle failures?

The system uses:

```text
Retry
Circuit Breaker
Transactional Outbox
Kafka DLT
Saga Compensation
Idempotency
Optimistic Locking
```

### Q10. How do you deploy a new version?

```text
Git Push
   ↓
GitHub Actions
   ↓
Tests
   ↓
Docker Build
   ↓
ECR
   ↓
ECS Deployment
   ↓
Health Check
```

### Q11. How do you protect the database?

The database stays in private subnets and its Security Group only permits access from authorized application services.

### Q12. How does JWT work after deployment?

The client receives a JWT from Auth Service. The API Gateway validates the JWT before forwarding protected requests to internal services.

---

# 49. Final Project Architecture

The final project demonstrates:

```text
Java 17
Spring Boot
Spring Cloud Gateway
Spring Security
JWT
REST APIs
PostgreSQL
Kafka
Saga Pattern
Transactional Outbox
Retry
DLT
Idempotency
Optimistic Locking
Circuit Breaker
JUnit 5
Mockito
Testcontainers
Docker
Docker Compose
AWS ECS
AWS ECR
AWS RDS
AWS MSK
CloudWatch
Secrets Manager
GitHub Actions
CI/CD
```

This gives the project a complete path from:

```text
Local Development
       ↓
Docker
       ↓
Testing
       ↓
CI/CD
       ↓
AWS
       ↓
Production-style Microservices
```

---

# 50. Recommended Next Improvements

After AWS deployment documentation, the remaining project work should focus on:

1. Distributed tracing
2. Correlation IDs
3. Swagger/OpenAPI
4. Performance testing
5. AWS deployment implementation
6. Terraform / Infrastructure as Code
7. Final README
8. Architecture diagrams
9. Resume project description
10. Interview preparation

The most important next implementation after documentation is **distributed tracing + correlation IDs**, because your architecture now contains multiple services and Kafka events, making request tracing across services an important production concern.
