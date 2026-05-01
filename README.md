# Device Management Service

A production-grade REST API for managing device resources, built with Java 21 and Spring Boot 3.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Improvements & Future Enhancements](#improvements--future-enhancements)
- [Production Considerations](#production-considerations)

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 17 |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Bean Validation |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Containerisation | Jib (Google), Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build Tool | Gradle 9 |

---

## Architecture

The service follows a standard **layered architecture**:

```
HTTP Request
     ↓
DeviceController      — routing, request/response handling, Swagger docs
     ↓
DeviceService         — business logic, domain validation, orchestration
     ↓
DeviceRepository      — Spring Data JPA, database queries
     ↓
DeviceEntity          — JPA entity, persistence mapping
```

### Key Design Decisions

**Hybrid ID Strategy** — Two identifiers per device:
- `id` (`Long`) — internal primary key used for DB joins, indexing, and sequence generation. Never exposed externally.
- `deviceId` (`UUID`) — public-facing identifier exposed in all API responses and URL paths. Decouples the external identity from internal structure and prevents sequential ID enumeration attacks.

**Spring Auditing** — `dateCreated` and `lastUpdated` are populated automatically via `@CreatedDate` and `@LastModifiedDate`. A custom `DateTimeProvider` ensures `OffsetDateTime` is used consistently across timezones.

**Centralised Exception Handling** — `GlobalExceptionHandler` maps all domain exceptions to consistent JSON error responses with timestamps.

**MapStruct Mapper** — Compile-time mapping with explicit field ignoring for audit and ID fields, preventing accidental overwrites on update operations.

**Pagination** — All list endpoints return paginated responses using Spring Data `Pageable`, preventing unbounded result sets.

---

## Prerequisites

- Java 21+
- Docker Desktop (running)
- Gradle 9+ (or use the included `./gradlew` wrapper)

---

## Setup & Installation

### 1. Clone the repository

```bash
git clone <repository-url>
cd device-service
```

### 2. Configure environment variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

`.env.example`:
```env
POSTGRES_DB=device_service
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password
POSTGRES_PORT=5433
APP_PORT=8080
```

> ⚠️ Never commit your `.env` file. It is listed in `.gitignore`.

---

## Running the Application

### Option A — Full Docker setup (recommended)

Build the application image with Jib, then start all services:

```bash
# Step 1 — Build the application image
./gradlew jibDockerBuild

# Step 2 — Start PostgreSQL and the application
docker compose up
```

The API will be available at `http://localhost:8080`.

> **Apple Silicon (ARM64) note:** The Jib build targets both `linux/amd64` and `linux/arm64` — no extra configuration needed.

### Option B — Local development (app runs locally, DB in Docker)

Start only the database:

```bash
docker compose up postgres
```

Run the application with the `dev` profile:

```bash
./gradlew bootRun
```

The `dev` profile connects to `localhost:5433` and enables SQL logging.

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

### Base URL

```
/api/devices
```

---

### Endpoints

#### `POST /api/devices` — Create a device

**Request body:**
```json
{
    "name": "iPhone 15 Pro",
    "brand": "Apple",
    "state": "AVAILABLE"
}
```

**Response `201 Created`:**
```json
{
    "deviceId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "iPhone 15 Pro",
    "brand": "Apple",
    "state": "AVAILABLE",
    "dateCreated": "2024-03-15T10:30:00+01:00"
}
```

---

#### `GET /api/devices/{deviceId}` — Get a device

**Response `200 OK`:**
```json
{
    "deviceId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "iPhone 15 Pro",
    "brand": "Apple",
    "state": "AVAILABLE",
    "dateCreated": "2024-03-15T10:30:00+01:00"
}
```

---

#### `GET /api/devices` — Get all devices (paginated)

Supports optional filtering by `brand` **or** `state`. Providing both returns `400 Bad Request`.

```
GET /api/devices                        — all devices (paginated)
GET /api/devices?brand=Apple            — filter by brand
GET /api/devices?state=AVAILABLE        — filter by state
GET /api/devices?page=0&size=10         — control pagination
GET /api/devices?sort=dateCreated,desc  — control sorting
```

**Response `200 OK`:**
```json
{
    "content": [
        {
            "deviceId": "550e8400-e29b-41d4-a716-446655440000",
            "name": "iPhone 15 Pro",
            "brand": "Apple",
            "state": "AVAILABLE",
            "dateCreated": "2024-03-15T10:30:00+01:00"
        }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
}
```

> Default page size is `20`, sorted by `dateCreated` descending.

---

#### `PUT /api/devices/{deviceId}` — Fully update a device

All fields are required. Returns `409 Conflict` if the device is `IN_USE`.

**Request body:**
```json
{
    "name": "iPhone 16 Pro",
    "brand": "Apple",
    "state": "AVAILABLE"
}
```

**Response `200 OK`:** Returns updated device.

---

#### `PATCH /api/devices/{deviceId}` — Partially update a device

All fields are optional. `name` and `brand` cannot be changed if the device is `IN_USE`. `state` can always be updated.

**Request body (update state only):**
```json
{
    "state": "IN_USE"
}
```

**Response `200 OK`:** Returns updated device.

---

#### `DELETE /api/devices/{deviceId}` — Delete a device

Returns `409 Conflict` if the device is `IN_USE`.

**Response `204 No Content`**

---

### Device States

| Value | Description |
|---|---|
| `AVAILABLE` | Device is available for use |
| `IN_USE` | Device is currently in use — `name` and `brand` are locked, deletion is blocked |
| `INACTIVE` | Device is inactive |

---

### HTTP Status Codes

| Code | Meaning |
|---|---|
| `200` | Success |
| `201` | Resource created |
| `204` | Resource deleted |
| `400` | Validation error or invalid filter combination |
| `404` | Device not found |
| `409` | Business rule conflict — device is in use |
| `500` | Unexpected server or database error |

---

## Error Handling

All errors return a consistent JSON structure:

```json
{
    "message": "Device not found with id: 550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2024-03-15T10:30:00Z"
}
```

| Scenario | Status |
|---|---|
| Missing or invalid request field | `400` |
| Both `brand` and `state` filters provided | `400` |
| Device not found | `404` |
| Update `name`/`brand` on `IN_USE` device | `409` |
| Delete `IN_USE` device | `409` |
| Database error | `500` |

---

## Testing

Docker must be running — integration tests use Testcontainers to spin up a real PostgreSQL instance automatically.

### Run all tests

```bash
./gradlew test
```

### Test structure

```
src/test/
├── BaseIntegrationTest.java          — shared Testcontainers + RestTemplate setup
├── TestFactory.java                  — centralised test data builders and helpers
├── controller/
│   └── DeviceControllerIT.java       — full integration tests via HTTP (16 tests)
├── service/
│   └── DeviceServiceTest.java        — unit tests with Mockito (16 tests)
├── repository/
│   └── DeviceRepositoryTest.java     — repository slice tests with Testcontainers (4 tests)
└── transform/
    └── DeviceMapperTest.java         — MapStruct mapper unit tests (3 tests)
```

### Test coverage highlights

- All CRUD endpoints tested end-to-end via HTTP with a real PostgreSQL instance
- All business rule validations tested (in-use locks, delete restrictions)
- All filter and pagination paths covered
- Repository queries verified with real data
- Mapper field ignoring verified explicitly

---

## Example curl Requests

```bash
# Create a device
curl -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -d '{"name":"iPhone 15 Pro","brand":"Apple","state":"AVAILABLE"}'

# Get all devices (first page)
curl http://localhost:8080/api/devices

# Filter by brand
curl "http://localhost:8080/api/devices?brand=Apple"

# Filter by state
curl "http://localhost:8080/api/devices?state=AVAILABLE"

# Paginate
curl "http://localhost:8080/api/devices?page=0&size=5&sort=dateCreated,desc"

# Get single device
curl http://localhost:8080/api/devices/{deviceId}

# Full update
curl -X PUT http://localhost:8080/api/devices/{deviceId} \
  -H "Content-Type: application/json" \
  -d '{"name":"iPhone 16 Pro","brand":"Apple","state":"AVAILABLE"}'

# Partial update — state only
curl -X PATCH http://localhost:8080/api/devices/{deviceId} \
  -H "Content-Type: application/json" \
  -d '{"state":"IN_USE"}'

# Delete
curl -X DELETE http://localhost:8080/api/devices/{deviceId}
```

---

## Improvements & Future Enhancements

The following improvements have been identified as candidates for future iterations:

| Area | Description |
|---|---|
| **Authentication** | The API has no security layer. Spring Security with JWT or OAuth2 should be added before production deployment |
| **API Versioning** | Add version prefix `/api/v1/devices` to protect clients from breaking changes |
| **Structured Logging** | Add SLF4J log statements to all service operations for production observability |
| **Database Indexes** | Add indexes on `brand` and `state` columns — currently used as filter parameters but unindexed |
| **Schema Migrations** | Introduce Flyway for versioned, auditable schema management in production |
| **Caching** | Device lookups by `deviceId` are good candidates for `@Cacheable` |
| **Health Checks** | Add Spring Boot Actuator to expose `/actuator/health` for container orchestration |
| **CI/CD Pipeline** | Add GitHub Actions or GitLab CI to run `./gradlew test` on every commit |
| **Metrics** | Integrate Micrometer + Prometheus for production visibility |

---

## Production Considerations

**Schema management** — Replace `ddl-auto: update` with Flyway migrations before production. This provides versioned, auditable, and reversible schema changes.

**Secrets management** — Environment variables are used for database credentials. In production, source these from a secrets manager (AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets).

**Container memory** — The Jib configuration sets `-XX:MaxRAMPercentage=75.0` and `-XX:+UseContainerSupport`, ensuring the JVM respects Docker memory limits and avoids OOM kills.

**Connection pooling** — HikariCP is the default connection pool. Tune `maximum-pool-size` based on expected concurrency and the PostgreSQL `max_connections` setting.

**Horizontal scaling** — The service is stateless and can be scaled horizontally behind a load balancer. Ensure database connection pool limits are adjusted per instance accordingly.

**Data persistence** — PostgreSQL data is persisted via a named Docker volume (`postgres_data`). This survives container restarts but not volume deletion — use managed database services (RDS, Cloud SQL) in production.
