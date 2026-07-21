# Concert Ticket Booking Platform

## Technology Stack
- Java 17
- Spring Boot 3.2.x
- PostgreSQL
- Redis & Redisson (Distributed Locking)
- Apache Kafka (Message Broker for asynchronous processing)
- Docker & Docker Compose

## Prerequisites
- Java 17
- Maven
- Docker & Docker Compose

## Getting Started

### 1. Start Infrastructure (PostgreSQL, Redis, Kafka)
```bash
docker-compose up -d
```

### 2. Run Application
```bash
./mvnw clean spring-boot:run
```

### 3. API Documentation
Once the application is running, Swagger UI is available at:
`http://localhost:8080/swagger-ui/index.html`

### 4. Run Unit Tests
To run the comprehensive test suite (including Mockito Unit Tests for Booking Service):
```bash
./mvnw test
```

## Architecture Highlights
- **3-Layer Architecture**: Controller, Service, Repository.
- **Idempotency**: Redis is used to guarantee idempotency for the booking API.
- **Distributed Locking**: Redisson is used to handle race conditions during high concurrent booking requests.
- **Asynchronous Processing**: Kafka is used to decouple the booking request from the database persistence, returning a `202 Accepted` immediately to handle high traffic spikes.

## Coding Standards & Conventions
- **Lombok**: Avoid using `@Data` for JPA Entities to prevent infinite recursion and memory leaks (especially with lazy loading relationships). Use `@Getter`, `@Setter`, and `@Builder` instead.
- **Error Handling**: Standardized via `@RestControllerAdvice` globally mapping exceptions to consistent JSON error bodies with appropriate HTTP Status Codes (e.g. 400, 409, 429).
- **Separation of Concerns**: Controllers only handle HTTP logic. Services handle business logic. Persistence is strictly managed via Repositories.
