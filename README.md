# Aeronix (Backend)

A microservices-based backend for an airline booking platform.

---

## Architecture Overview

The system is composed of 9 independent Spring Boot services, all registered with a Eureka service discovery server and exposed through a single API Gateway.

```
Client
  │
  ▼
API Gateway (port 8080)
  │   JWT Auth Filter
  │
  ├── Auth Service       (8081)  — JWT + Google OAuth2
  ├── Flight Service     (8082)  — Flight search & scheduling
  ├── Seat Service       (8083)  — Seat inventory + Redis holds
  ├── Booking Service    (8084)  — Bookings, PNR, check-in
  ├── Passenger Service  (8085)  — Passenger profiles
  ├── Payment Service    (8086)  — Razorpay integration + refunds
  ├── Notification Svc   (8087)  — Email (Gmail) + SMS (Twilio)
  └── Airline Service    (8088)  — Airlines & airports data

Eureka Server           (8761)  — Service discovery
```

**Infrastructure dependencies:** MySQL, Redis, RabbitMQ

---

## Services

### API Gateway — `api-gateway` (port 8080)
The single entry point for all client requests. Handles JWT validation on every inbound request before routing to downstream services. Also aggregates Swagger UI docs from all services at `/swagger-ui.html`.

### Auth Service — `auth-service` (port 8081)
Handles user registration, login, and token issuance. Supports both username/password and Google OAuth2 login. Issues and validates JWTs shared with the API Gateway.

**Key routes:** `/api/auth/**`

### Flight Service — `flight-service` (port 8082)
Manages flight schedules, routes, and availability. Used by the booking service for fare lookups.

**Key routes:** `/api/flights/**`

### Seat Service — `seat-service` (port 8083)
Tracks seat inventory per flight. Uses **Redis** to implement temporary seat holds during the booking flow, with a scheduler to release expired holds automatically.

**Key routes:** `/api/seats/**`

### Booking Service — `booking-service` (port 8084)
Core booking lifecycle: create bookings, generate PNR codes, check-in, add-ons, cancellations, and status management. Exposes analytics and revenue endpoints for admin use.

**Key routes:** `/api/bookings/**`

### Passenger Service — `passenger-service` (port 8085)
Manages passenger profiles linked to bookings.

**Key routes:** `/api/passengers/**`

### Payment Service — `payment-service` (port 8086)
Integrates with **Razorpay** (currency: INR) to initiate payments and process gateway callbacks. Supports refunds and revenue reporting. Publishes payment events to **RabbitMQ**, which the Notification Service consumes.

**Key routes:** `/api/payments/**`

### Notification Service — `notification-service` (port 8087)
Consumes events from RabbitMQ and sends notifications via:
- **Email** — Gmail SMTP (`aeronix.support@gmail.com`)
- **SMS** — Twilio

**Key routes:** `/api/notifications/**`

### Airline Service — `airline-service` (port 8088)
Manages airline and airport reference data, with a `DataSeeder` that pre-populates the database on startup.

**Key routes:** `/api/airlines/**`, `/api/airports/**`

### Eureka Server — `eureka-server` (port 8761)
Netflix Eureka service registry. All services register here; the API Gateway uses it for load-balanced routing (`lb://service-name`).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3, Spring Cloud |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Auth | Spring Security, JWT, Google OAuth2 |
| Database | MySQL 8 |
| Cache / Holds | Redis |
| Messaging | RabbitMQ |
| Payments | Razorpay |
| Notifications | Gmail SMTP, Twilio |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven (Maven Wrapper included) |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven (or use the included `mvnw` wrappers)
- MySQL 8
- Redis
- RabbitMQ

### 1. Configure environment variables

Create a `.env` file with the following
(Change with your credentials):
```env
DATABASE_PASS=your_mysql_password
JWT_SECRET=your_jwt_secret
GOOGLE_CLIENT_ID=your_google_oauth_client_id
GOOGLE_CLIENT_SECRET=your_google_oauth_client_secret
APP_BASE_URL=http://localhost:8081
RAZORPAY_KEY=your_razorpay_key
RAZORPAY_SECRET=your_razorpay_secret
MAIL_PASS=your_gmail_app_password
SID=your_twilio_account_sid
TOKEN=your_twilio_auth_token
NUMBER=your_twilio_phone_number
```

### 2. Set up the database

Create a MySQL database:

```sql
CREATE DATABASE aeronix_db;
```

Each service uses `spring.jpa.hibernate.ddl-auto=update`, so tables are created automatically on first run.

### 3. Start infrastructure

Ensure MySQL, Redis (port 6379), and RabbitMQ (port 5672) are running locally.

### 4. Start services (in order)

Start the Eureka server first, then the remaining services in any order:

```bash
# 1. Eureka Server
cd eureka-server && ./mvnw spring-boot:run

# 2. All other services (each in its own terminal)
cd auth-service       && mvn spring-boot:run
cd api-gateway        && mvn spring-boot:run
cd flight-service     && mvn spring-boot:run
cd seat-service       && mvn spring-boot:run
cd booking-service    && mvn spring-boot:run
cd passenger-service  && mvn spring-boot:run
cd payment-service    && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd airline-service    && mvn spring-boot:run
```

### 5. Access the API

All traffic goes through the gateway:

```
http://localhost:8080
```

Aggregated Swagger UI (all services):

```
http://localhost:8080/swagger-ui.html
```

Eureka dashboard:

```
http://localhost:8761
```

---

## Running Tests

Each service has its own test suite. Run from within the service directory:

```bash
mvn test
```

---

## Service Port Reference

| Service | Port |
|---|---|
| API Gateway | 8080 |
| Auth Service | 8081 |
| Flight Service | 8082 |
| Seat Service | 8083 |
| Booking Service | 8084 |
| Passenger Service | 8085 |
| Payment Service | 8086 |
| Notification Service | 8087 |
| Airline Service | 8088 |
| Eureka Server | 8761 |

---

## Notes

- The `airline-service` seeds initial airline and airport data automatically via `DataSeeder` on startup.
- Seat holds are managed in Redis with a TTL; the `SeatHoldScheduler` cleans up expired holds.
- Payment events flow from `payment-service` → RabbitMQ → `notification-service` to trigger booking confirmation emails and SMS.