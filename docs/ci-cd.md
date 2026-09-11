# CI/CD

## 1. Overview

CI/CD stands for:

```text
CI = Continuous Integration
CD = Continuous Delivery / Continuous Deployment
```

CI/CD automates the process of:

```text
Code
 ↓
Build
 ↓
Test
 ↓
Package
 ↓
Docker Image
 ↓
Deploy
```

For the Distributed E-Commerce Platform, CI/CD helps ensure that every code change is automatically validated before it is deployed.

The project uses:

```text
Git
GitHub
Maven
JUnit 5
Mockito
Testcontainers
Docker
Docker Compose
GitHub Actions
```

---

# 2. Why CI/CD Is Required

Without CI/CD, developers might manually perform:

```text
git pull
 ↓
mvn clean package
 ↓
run tests
 ↓
build Docker image
 ↓
deploy
```

This is error-prone and time-consuming.

With CI/CD:

```text
Developer
   ↓
git push
   ↓
GitHub Actions
   ↓
Build
   ↓
Test
   ↓
Docker Build
   ↓
Deploy
```

The process becomes repeatable and automated.

---

# 3. Continuous Integration

Continuous Integration means developers frequently integrate their code into a shared repository.

Example:

```text
Developer A
     |
     v
   Git Push
     |
     v
  GitHub
     |
     v
CI Pipeline
```

The pipeline automatically builds and tests the application.

---

# 4. Continuous Delivery

Continuous Delivery means the application is automatically built, tested, and prepared for deployment.

```text
Code
 ↓
Build
 ↓
Test
 ↓
Package
 ↓
Docker Image
 ↓
Ready for Deployment
```

Deployment may still require a manual approval.

---

# 5. Continuous Deployment

Continuous Deployment goes one step further.

After successful testing:

```text
Code
 ↓
Build
 ↓
Test
 ↓
Docker Build
 ↓
Automatic Deployment
```

No manual deployment step is required.

---

# 6. CI/CD Pipeline

The project's target pipeline is:

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
          ┌───────────┴───────────┐
          |                       |
        Build                    Test
          |                       |
          └───────────┬───────────┘
                      |
                      v
                Maven Package
                      |
                      v
                Docker Build
                      |
                      v
                Docker Image
                      |
                      v
                  Deploy
```

---

# 7. Git Workflow

A simple development workflow is:

```text
Create Feature
      ↓
Write Code
      ↓
Run Tests
      ↓
Git Commit
      ↓
Git Push
      ↓
Pull Request
      ↓
CI Pipeline
      ↓
Code Review
      ↓
Merge
```

Example:

```bash
git checkout -b feature/inventory-reservation
```

After development:

```bash
git add .
git commit -m "feat: add inventory reservation"
git push origin feature/inventory-reservation
```

---

# 8. Pull Request

A Pull Request allows code changes to be reviewed before merging.

Example:

```text
feature/inventory-reservation
            |
            v
       Pull Request
            |
            v
      GitHub Actions
            |
       ┌────┴────┐
       |         |
     PASS       FAIL
       |         |
       v         v
   Review      Fix Code
       |
       v
     Merge
```

---

# 9. Maven Build

The project uses Maven to build the Spring Boot services.

Basic command:

```bash
mvn clean package
```

This performs:

```text
Clean
 ↓
Compile
 ↓
Test
 ↓
Package
 ↓
JAR
```

For example:

```text
order-service
      ↓
target/
      ↓
order-service-*.jar
```

---

# 10. Maven Test

Before creating a production artifact:

```bash
mvn test
```

This executes the test suite.

Tests include:

```text
JUnit
Mockito
Integration Tests
Testcontainers
```

depending on the service and Maven configuration.

---

# 11. Maven Package

The package command creates the application JAR.

```bash
mvn clean package
```

Example:

```text
target/
└── order-service-1.0.0.jar
```

The JAR is then used by the Docker image.

---

# 12. Build Failure

Suppose a developer introduces a compilation error.

```text
Git Push
   ↓
GitHub Actions
   ↓
Maven Build
   ↓
Compilation Error
   ↓
Pipeline FAILED
```

The Docker image should not be built or deployed.

This prevents broken code from reaching the deployment environment.

---

# 13. Test Failure

Suppose the build succeeds but a test fails:

```text
Build
 ↓
PASS

Tests
 ↓
FAIL
```

The pipeline stops.

```text
Tests Failed
     ↓
No Deployment
```

This protects the application from known regressions.

---

# 14. Docker Build

After successful tests, Docker images can be created.

For example:

```bash
docker build -t ecommerce-order-service:latest ./order-service
```

The Dockerfile:

```dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/order-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

# 15. Docker Image Flow

```text
Maven
  ↓
JAR
  ↓
Dockerfile
  ↓
Docker Build
  ↓
Docker Image
```

Example:

```text
order-service.jar
      ↓
order-service Docker Image
```

---

# 16. Microservice Images

The project contains multiple services.

Each service can have its own Docker image:

```text
order-service
inventory-service
payment-service
shipping-service
auth-service
notification-service
api-gateway
saga-orchestrator
```

Conceptually:

```text
                         Docker Images
                              |
       ┌──────────┬───────────┼───────────┬──────────┐
       ↓          ↓           ↓           ↓          ↓
     Order    Inventory    Payment     Shipping    Auth
```

---

# 17. Docker Image Tagging

Avoid relying only on:

```text
latest
```

for production deployments.

A better approach is to tag images using a version or Git commit SHA.

Example:

```text
order-service:1.0.0
```

or:

```text
order-service:a8f32d1
```

where:

```text
a8f32d1
```

represents the Git commit.

This makes deployments traceable.

---

# 18. Why Commit-Based Tags?

Suppose:

```text
Version A
   ↓
order-service:a8f32d1
```

and later:

```text
Version B
   ↓
order-service:c91e7f2
```

If Version B causes a problem, we know exactly which image was deployed.

This makes rollback easier.

---

# 19. GitHub Actions

GitHub Actions is used to automate CI/CD workflows.

Workflow files are stored under:

```text
.github/workflows/
```

Example:

```text
.github/
└── workflows/
    └── ci.yml
```

---

# 20. Basic GitHub Actions Workflow

A basic CI workflow looks like:

```yaml
name: CI

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main

jobs:
  build-and-test:

    runs-on: ubuntu-latest

    steps:

      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Build and test
        run: mvn clean test
```

This automatically runs when code is pushed or a Pull Request is created.

---

# 21. CI Workflow

The basic workflow is:

```text
Git Push
   ↓
Checkout Repository
   ↓
Install Java 17
   ↓
Install Maven Dependencies
   ↓
Compile
   ↓
Run Tests
   ↓
PASS / FAIL
```

---

# 22. Multi-Service CI

Because this is a microservices project, CI should eventually build all services.

Conceptually:

```text
GitHub Actions
      |
      ├── Order Service
      ├── Inventory Service
      ├── Payment Service
      ├── Shipping Service
      ├── Auth Service
      ├── Notification Service
      ├── API Gateway
      └── Saga Orchestrator
```

Each service should be compiled and tested.

---

# 23. Matrix Builds

GitHub Actions can use a matrix strategy to run similar jobs for multiple services.

Conceptually:

```yaml
strategy:
  matrix:
    service:
      - order-service
      - inventory-service
      - payment-service
      - shipping-service
```

Then the same build logic can be applied to each service.

This reduces duplicated workflow configuration.

---

# 24. Integration Tests in CI

The project uses Testcontainers.

Therefore, the CI environment needs to support Docker.

The pipeline becomes:

```text
GitHub Actions
      ↓
Java 17
      ↓
Maven
      ↓
JUnit
      ↓
Testcontainers
      ↓
PostgreSQL Container
      +
Kafka Container
      ↓
Integration Tests
```

This is important because the tests depend on real infrastructure.

---

# 25. Testcontainers in GitHub Actions

A Linux GitHub Actions runner can run Docker containers.

Therefore:

```text
JUnit Test
   ↓
Testcontainers
   ↓
Docker
   ↓
PostgreSQL/Kafka
```

can be executed inside the CI environment.

---

# 26. CI Environment

The CI environment should provide:

```text
Java 17
Maven
Docker
Git
```

The application does not need the developer's local machine configuration.

This is one of the main benefits of CI/CD.

---

# 27. Environment Variables

Sensitive configuration should not be hardcoded.

Examples:

```text
JWT_SECRET
DATABASE_PASSWORD
AWS_ACCESS_KEY
AWS_SECRET_KEY
```

should be provided through environment variables or a secret-management system.

---

# 28. GitHub Secrets

Sensitive values can be stored as GitHub Actions Secrets.

Conceptually:

```text
GitHub Secrets
      |
      ├── JWT_SECRET
      ├── DATABASE_PASSWORD
      └── AWS credentials
```

The workflow can consume these values without storing them directly in the repository.

---

# 29. Important Secret Rule

Never put:

```text
JWT_SECRET=...
AWS_SECRET_ACCESS_KEY=...
DATABASE_PASSWORD=...
```

directly inside:

```text
ci.yml
```

or Java source code.

Use GitHub Secrets or an appropriate cloud secret-management service.

---

# 30. Docker Registry

After building a Docker image, it can be pushed to a container registry.

Examples include:

```text
Docker Hub
Amazon ECR
GitHub Container Registry
```

The production architecture can use:

```text
GitHub Actions
      ↓
Docker Build
      ↓
Docker Image
      ↓
Container Registry
      ↓
Deployment Environment
```

---

# 31. Example Image Pipeline

For Order Service:

```text
Code
 ↓
Maven Test
 ↓
JAR
 ↓
Docker Build
 ↓
ecommerce-order-service:a8f32d1
 ↓
Container Registry
```

The same process can be applied to the other services.

---

# 32. Deployment

Deployment means running the newly built images in the target environment.

Conceptually:

```text
Container Registry
       ↓
Deployment Platform
       ↓
Pull Image
       ↓
Start Container
       ↓
Health Check
       ↓
Application Running
```

---

# 33. Docker Compose Deployment

For local or simple server environments, Docker Compose can run the complete platform.

The project contains:

```text
docker-compose.yml
```

which defines:

```text
PostgreSQL
Kafka
Order
Inventory
Payment
Shipping
Auth
Notification
Gateway
Saga
```

---

# 34. Local Deployment

The complete environment can be started with:

```bash
docker compose up -d
```

For rebuilding images:

```bash
docker compose up -d --build
```

Verify:

```bash
docker compose ps
```

---

# 35. Deployment Health

After deployment, services should be checked.

For example:

```text
Gateway       :8080
Order         :8081
Inventory     :8082
Payment       :8083
Shipping      :8084
Auth          :8085
Notification  :8086
Saga          :8090
```

Actuator health endpoints can also be used where configured.

---

# 36. Monitoring After Deployment

The project already includes monitoring using:

```text
Spring Boot Actuator
Prometheus
Grafana
```

The CI/CD process should therefore be followed by monitoring.

```text
Deploy
  ↓
Health Check
  ↓
Prometheus
  ↓
Grafana
  ↓
Monitor
```

---

# 37. Deployment Verification

After deployment:

```text
1. Check containers
2. Check application health
3. Check database connectivity
4. Check Kafka connectivity
5. Test Gateway
6. Test authentication
7. Test order creation
8. Verify Kafka events
9. Verify Saga flow
10. Check logs and metrics
```

---

# 38. Smoke Testing

Smoke tests are a small set of tests that verify that the deployed application is basically functional.

Example:

```text
Auth Login
    ↓
Create Product
    ↓
Create Order
    ↓
Inventory Reservation
    ↓
Payment
    ↓
Shipping
```

If the basic flow works, the deployment is considered healthy enough for further validation.

---

# 39. Deployment Failure

Suppose the new version fails its health check:

```text
New Deployment
      ↓
Health Check
      ↓
FAIL
      ↓
Deployment Stopped
```

The previous version should remain available when the deployment strategy supports it.

---

# 40. Rollback

Rollback means returning to a previously working version.

Example:

```text
Current:
order-service:c91e7f2

Problem detected
      ↓
Rollback
      ↓
order-service:a8f32d1
```

Commit-based image tags make this easier.

---

# 41. CI/CD Failure Scenarios

Important failures include:

### Build failure

```text
Compilation Error
   ↓
Pipeline Failed
```

### Unit test failure

```text
JUnit Failure
   ↓
Pipeline Failed
```

### Integration test failure

```text
Kafka/PostgreSQL Test Failure
   ↓
Pipeline Failed
```

### Docker build failure

```text
Dockerfile/Image Error
   ↓
Pipeline Failed
```

### Deployment failure

```text
Health Check Failed
   ↓
Rollback / Stop Deployment
```

---

# 42. Branch Strategy

A simple strategy is:

```text
main
 |
 +---- feature/order-service
 |
 +---- feature/inventory
 |
 +---- feature/security
 |
 +---- feature/ci-cd
```

Developers work on feature branches.

After review:

```text
feature branch
      ↓
Pull Request
      ↓
CI
      ↓
Code Review
      ↓
main
```

---

# 43. Commit Convention

Meaningful commit messages make CI/CD and release history easier to understand.

Examples:

```text
feat: add inventory reservation
fix: handle duplicate inventory events
test: add payment consumer tests
docs: add security documentation
ci: add GitHub Actions pipeline
```

---

# 44. Build Once, Deploy Many

A useful CI/CD principle is:

> Build the artifact once and promote the same artifact through environments.

For example:

```text
Git Commit
   ↓
Build Image
   ↓
Test Image
   ↓
Dev
   ↓
Staging
   ↓
Production
```

The same image should ideally be promoted rather than rebuilding different artifacts for each environment.

---

# 45. Environment Separation

A production system should separate environments:

```text
Development
     ↓
Testing
     ↓
Staging
     ↓
Production
```

Each environment can have different:

```text
Database
Kafka
Secrets
Configuration
Resources
```

The application code remains the same.

---

# 46. Configuration Management

Environment-specific values should be externalized.

Examples:

```text
Database URL
Kafka URL
JWT secret
Cloud credentials
External API URLs
```

For Docker:

```text
Environment Variables
```

For production:

```text
Secret Manager
Configuration Service
Environment Variables
```

depending on the deployment architecture.

---

# 47. CI/CD and Database Changes

Database schema changes should also be handled carefully.

For production systems, database migrations should ideally use a migration tool such as:

```text
Flyway
Liquibase
```

instead of relying only on automatic schema generation.

The deployment flow can become:

```text
Deploy
 ↓
Database Migration
 ↓
Application Startup
 ↓
Health Check
```

This is a future improvement for the project.

---

# 48. Zero-Downtime Deployment

A production deployment should ideally avoid unnecessary downtime.

Common approaches include:

```text
Rolling Deployment
Blue-Green Deployment
Canary Deployment
```

---

# 49. Rolling Deployment

Example:

```text
Version 1
Instance A
Instance B
Instance C

       ↓

Replace one instance at a time

       ↓

Version 2
Instance A
Instance B
Instance C
```

This allows some instances to continue serving traffic.

---

# 50. Blue-Green Deployment

Two environments are maintained:

```text
Blue  → Current Production
Green → New Version
```

After validation:

```text
Traffic
  ↓
Green
```

If something goes wrong:

```text
Traffic
  ↓
Blue
```

This makes rollback fast.

---

# 51. Canary Deployment

Only a small percentage of traffic is initially sent to the new version.

Example:

```text
95% → Version 1
5%  → Version 2
```

If Version 2 is healthy:

```text
70% → Version 1
30% → Version 2
```

Eventually:

```text
100% → Version 2
```

---

# 52. CI/CD with Microservices

Microservices make CI/CD more flexible because services can potentially be built and deployed independently.

For example:

```text
Order Service
     ↓
Build
     ↓
Test
     ↓
Deploy
```

without necessarily rebuilding the entire application.

However, compatibility between services and shared event contracts must be managed carefully.

---

# 53. Event Contract Compatibility

The project communicates through Kafka events.

For example:

```text
order.created
```

If the event structure changes:

```text
Producer
     ↓
New Event Format
     ↓
Old Consumer
     ↓
Potential Failure
```

Therefore event schema changes should be backward compatible where possible.

This is an important CI/CD concern for event-driven microservices.

---

# 54. Pipeline Stages

The complete pipeline can be represented as:

```text
Stage 1
Checkout
   ↓
Stage 2
Compile
   ↓
Stage 3
Unit Tests
   ↓
Stage 4
Integration Tests
   ↓
Stage 5
Package JAR
   ↓
Stage 6
Build Docker Images
   ↓
Stage 7
Push Images
   ↓
Stage 8
Deploy
   ↓
Stage 9
Health Check
   ↓
Stage 10
Monitor
```

---

# 55. Recommended GitHub Actions Pipeline

A more complete workflow can look like:

```yaml
name: Ecommerce CI/CD

on:
  push:
    branches:
      - main

  pull_request:
    branches:
      - main

jobs:

  test:
    runs-on: ubuntu-latest

    steps:

      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Run tests
        run: mvn clean test

  docker-build:
    needs: test
    runs-on: ubuntu-latest

    steps:

      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Build application
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: |
          docker build \
            -t ecommerce-order-service:${{ github.sha }} \
            ./order-service
```

This is a simplified example. A production workflow should be adapted to the actual multi-module/repository structure and deployment platform.

---

# 56. Why `needs` Is Used

In:

```yaml
needs: test
```

the Docker build waits for the test job.

Therefore:

```text
Tests
  ↓
PASS
  ↓
Docker Build
```

If tests fail:

```text
Tests
  ↓
FAIL
  ↓
Docker Build does not run
```

---

# 57. Security in CI/CD

Security should exist throughout the pipeline.

```text
Developer
   ↓
Git
   ↓
CI
   ↓
Build
   ↓
Test
   ↓
Container
   ↓
Deploy
```

Security practices include:

* Never commit secrets
* Use GitHub Secrets
* Scan dependencies
* Scan Docker images
* Use minimal base images
* Restrict deployment credentials
* Use least privilege
* Protect the main branch

---

# 58. Dependency Security

The project uses multiple dependencies.

A CI pipeline can include dependency vulnerability scanning.

For example:

```text
Maven Dependencies
       ↓
Security Scan
       ↓
Vulnerability?
       ↓
PASS / FAIL
```

Tools such as GitHub Dependabot or dependency scanners can help identify vulnerable dependencies.

---

# 59. Docker Security

Production images should:

* Use trusted base images
* Avoid unnecessary packages
* Avoid running as root where practical
* Keep dependencies updated
* Scan images for vulnerabilities

The current project uses:

```dockerfile
FROM eclipse-temurin:17-jdk
```

A production optimization can consider a smaller runtime-oriented image where appropriate.

---

# 60. CI/CD and Monitoring

CI/CD does not end after deployment.

The complete process is:

```text
Code
 ↓
Build
 ↓
Test
 ↓
Deploy
 ↓
Monitor
 ↓
Detect Problem
 ↓
Rollback / Fix
```

Monitoring with:

```text
Actuator
Prometheus
Grafana
```

helps detect runtime issues after deployment.

---

# 61. Deployment Observability

After deployment, monitor:

```text
CPU
Memory
Request count
Error rate
Response time
Kafka consumer behavior
Database connections
JVM metrics
Service health
```

For this distributed application, Kafka and database health are especially important.

---

# 62. CI/CD and Distributed Systems

The CI/CD pipeline must consider that this is not a single application.

It contains:

```text
8 application services
+
PostgreSQL
+
Kafka
```

Therefore testing and deployment must validate both:

```text
Individual Service
```

and:

```text
Complete Distributed System
```

---

# 63. Final CI/CD Architecture

```text
                         DEVELOPER
                             |
                             | git push
                             v
                          GITHUB
                             |
                             v
                    GITHUB ACTIONS
                             |
              ┌──────────────┴──────────────┐
              |                             |
           BUILD                          TEST
              |                             |
              |                   ┌─────────┴─────────┐
              |                   |                   |
              |                UNIT TESTS       INTEGRATION
              |                   |                   |
              |                JUnit/Mockito     Testcontainers
              |                                       |
              |                                ┌──────┴──────┐
              |                                |             |
              |                           PostgreSQL       Kafka
              |                                |             |
              └────────────────────────────────┴─────────────┘
                             |
                          PASS
                             |
                             v
                       MAVEN PACKAGE
                             |
                             v
                       DOCKER BUILD
                             |
                             v
                    CONTAINER REGISTRY
                             |
                             v
                         DEPLOYMENT
                             |
                             v
                       HEALTH CHECK
                             |
                             v
                     ACTUATOR / METRICS
                             |
                             v
                    PROMETHEUS / GRAFANA
```

---

# 64. Final CI/CD Mental Model

Remember the pipeline as:

```text
CODE
  ↓
GIT
  ↓
CI
  ↓
BUILD
  ↓
UNIT TEST
  ↓
INTEGRATION TEST
  ↓
PACKAGE
  ↓
DOCKER IMAGE
  ↓
REGISTRY
  ↓
DEPLOY
  ↓
HEALTH CHECK
  ↓
MONITOR
```

If something fails:

```text
Build Failure
      ↓
STOP

Test Failure
      ↓
STOP

Docker Failure
      ↓
STOP

Deployment Failure
      ↓
ROLLBACK
```

The core principle is:

> **Never deploy code that has not successfully passed the required build and test stages.**

---

# 65. Interview Explanation

A strong interview answer:

> "For my distributed e-commerce project, I designed a CI/CD pipeline using GitHub Actions. Whenever code is pushed or a Pull Request is created, the pipeline checks out the code, sets up Java 17, builds the Maven project, and runs unit and integration tests. Integration tests use Testcontainers with PostgreSQL and Kafka to validate the actual infrastructure. After successful tests, the services are packaged as JARs and Docker images can be built and pushed to a container registry. The deployment environment then pulls the versioned images, performs health checks, and monitors the services using Actuator, Prometheus, and Grafana. I also use externalized secrets and commit-based Docker tags to make deployments traceable and easier to roll back."

---

# 66. Common CI/CD Interview Questions

### Q1. What is CI?

Continuous Integration means automatically building and testing code whenever changes are integrated into the shared repository.

### Q2. What is CD?

Continuous Delivery/Deployment automates the process of preparing or deploying tested software.

### Q3. Why use GitHub Actions?

It integrates directly with GitHub and can automate build, test, Docker, and deployment workflows.

### Q4. What happens when a unit test fails?

The CI pipeline fails and later stages such as Docker build or deployment should not proceed.

### Q5. How do you run integration tests in CI?

Using Testcontainers, which starts required infrastructure such as PostgreSQL and Kafka.

### Q6. Why Docker in CI/CD?

Docker creates consistent application environments across development, testing, and deployment.

### Q7. Why use Docker image tags based on Git commits?

They make each deployment traceable to an exact source-code version and simplify rollback.

### Q8. How do you manage secrets?

Use GitHub Secrets, environment variables, or production secret-management systems rather than committing secrets to Git.

### Q9. What is rollback?

Returning the deployment to a previously known-good application version.

### Q10. What is the difference between Continuous Delivery and Continuous Deployment?

Continuous Delivery automatically prepares a validated release but may require manual approval.

Continuous Deployment automatically releases the validated version to production.

### Q11. How would you deploy multiple microservices?

Each service can be built, tested, containerized, versioned, and deployed independently, while maintaining compatibility between service APIs and Kafka event contracts.

### Q12. How do you ensure a deployment is healthy?

Use health checks, logs, metrics, and monitoring tools such as Spring Boot Actuator, Prometheus, and Grafana.

---

# 67. Future Improvements

The current project can further improve its CI/CD pipeline by adding:

```text
[ ] GitHub Actions implementation
[ ] Docker image publishing
[ ] Amazon ECR
[ ] AWS deployment
[ ] Dependency vulnerability scanning
[ ] Docker image scanning
[ ] Flyway/Liquibase database migrations
[ ] Automated smoke tests
[ ] Deployment approval
[ ] Blue-Green deployment
[ ] Rolling deployment
[ ] Automatic rollback
[ ] Production secret manager
[ ] Canary deployment
```

These will be covered in the deployment and production-readiness documentation.

---

# 68. Final Summary

The CI/CD architecture provides an automated path from source code to deployment:

```text
Developer
   ↓
Git Push
   ↓
GitHub
   ↓
GitHub Actions
   ↓
Maven Build
   ↓
JUnit + Mockito
   ↓
Testcontainers
   ↓
PostgreSQL + Kafka Tests
   ↓
Maven Package
   ↓
Docker Build
   ↓
Container Registry
   ↓
Deployment
   ↓
Health Check
   ↓
Monitoring
```

This ensures that the Distributed E-Commerce Platform is not only functional locally but can also be built, tested, packaged, deployed, monitored, and rolled back in a repeatable manner.
