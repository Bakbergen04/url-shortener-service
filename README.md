# URL Shortener Service

A production-like URL shortening service built as a backend-focused portfolio project. It creates short links, redirects visitors, records click analytics, uses PostgreSQL as the source of truth, and caches frequently accessed links in Redis.

The repository includes a REST API, OpenAPI documentation, a small static web interface, Flyway migrations, Docker Compose, and automated tests with Testcontainers.

## Features

- Generated Base62 codes and custom aliases
- HTTP/HTTPS URL validation
- Expiring and manually deactivated links
- Race-safe atomic click counter updates
- Per-click events without storing raw IP addresses
- Redis cache with expiration-aware TTL
- Daily click statistics in UTC
- Search, pagination, and sorting
- Soft deletion
- Unified API error responses
- Swagger UI and OpenAPI specification
- Responsive framework-free web interface
- Unit, controller, context, and integration tests

## Technology stack

- Java 21
- Spring Boot 3.5
- Spring Web, Validation, Data JPA, Data Redis
- PostgreSQL 16
- Redis 7.4
- Flyway
- Lombok and MapStruct
- springdoc-openapi
- JUnit 5, Mockito, Testcontainers
- Maven, Docker, Docker Compose

## Architecture

The application follows a three-layer structure:

```text
HTTP request
    │
    ▼
Controller ──► Service ──► Repository ──► PostgreSQL
                  │
                  ├──────► Redis cache
                  └──────► Click analytics
```

The base Java package is `kg.jumabaev.shortener`.

### Redirect flow

1. `GET /{shortCode}` checks `short-link:{code}` in Redis.
2. A cached value is checked for active and expiration state.
3. On a cache miss, the link is loaded from PostgreSQL.
4. Missing links return `404`; inactive or expired links return `410`.
5. The click counter and `lastAccessedAt` are updated atomically in PostgreSQL.
6. A `ClickEvent` is stored with optional User-Agent and Referer data.
7. The link is cached for 10 minutes or until its earlier expiration time.
8. The client receives an HTTP `302` redirect.

Redis is a performance optimization, not the source of truth. Cache failures are handled in fail-open mode, so redirects continue through PostgreSQL.

## API endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/links` | Create a short link |
| `GET` | `/{shortCode}` | Redirect to the original URL |
| `GET` | `/api/v1/links/{shortCode}` | Get link information |
| `GET` | `/api/v1/links/{shortCode}/stats` | Get click statistics |
| `GET` | `/api/v1/links` | List and search links |
| `PATCH` | `/api/v1/links/{shortCode}` | Partially update a link |
| `DELETE` | `/api/v1/links/{shortCode}` | Soft-delete a link |

Pagination parameters are `page`, `size`, and `sort`. The optional `search` parameter searches title, original URL, and short code. Page size is capped at 100.

## Quick start with Docker

Requirements: Docker Desktop or Docker Engine with Compose v2.

```bash
docker compose up --build
```

This starts the application, PostgreSQL, and Redis. Default values are intended for local development only and can be overridden by copying `.env.example` to `.env`.

Available URLs:

- Web UI: <http://localhost:8080/>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Stop containers without deleting database volumes:

```bash
docker compose down
```

## Local development

Requirements:

- JDK 21
- PostgreSQL
- Redis
- Docker when running Testcontainers tests

Default application settings expect:

```text
PostgreSQL: localhost:5432/url_shortener
Username:   url_shortener
Password:   url_shortener
Redis:      localhost:6379
```

Run the application:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

To run only PostgreSQL and Redis from Compose, use the Compose development password and pass it to Spring:

```powershell
docker compose up -d postgres redis
$env:SPRING_DATASOURCE_PASSWORD="local_dev_password"
.\mvnw.cmd spring-boot:run
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/url_shortener` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `url_shortener` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `url_shortener` | Database password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `APP_BASE_URL` | `http://localhost:8080` | Public base URL for generated links |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `SERVER_PORT` | `8080` | HTTP port |

Never commit a real `.env` file. Only `.env.example` belongs in version control.

## Request examples

Create a link:

```bash
curl -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://example.com/some/long/path",
    "customAlias": "my-link",
    "title": "Example",
    "expiresAt": "2026-12-31T23:59:59Z"
  }'
```

Follow a redirect:

```bash
curl -i http://localhost:8080/my-link
```

Get link information and statistics:

```bash
curl http://localhost:8080/api/v1/links/my-link
curl http://localhost:8080/api/v1/links/my-link/stats
```

Search and paginate:

```bash
curl "http://localhost:8080/api/v1/links?search=example&page=0&size=20&sort=createdAt,desc"
```

Update a link:

```bash
curl -X PATCH http://localhost:8080/api/v1/links/my-link \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated title","active":true}'
```

Deactivate a link:

```bash
curl -X DELETE -i http://localhost:8080/api/v1/links/my-link
```

## Error format

```json
{
  "timestamp": "2026-06-25T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "originalUrl: must be a valid HTTP or HTTPS URL",
  "path": "/api/v1/links"
}
```

## Tests

Docker must be running because integration tests start isolated PostgreSQL and Redis containers.

```bash
./mvnw test
```

The test suite covers:

- Base62 code generation
- URL validation
- Link creation and alias collisions
- Service cache invalidation
- Controller validation, statistics, and pagination
- PostgreSQL persistence and Flyway schema
- HTTP `302` redirects and atomic click counts
- Expired link handling
- Redis use on repeated redirects
- Cache invalidation after update and delete

## Project structure

```text
src
├── main
│   ├── java/kg/jumabaev/shortener
│   │   ├── analytics
│   │   ├── cache
│   │   ├── config
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── exception
│   │   ├── mapper
│   │   ├── repository
│   │   ├── service
│   │   └── util
│   └── resources
│       ├── db/migration/V1__init_schema.sql
│       ├── static
│       │   ├── index.html
│       │   ├── app.js
│       │   └── styles.css
│       └── application.yml
└── test/java/kg/jumabaev/shortener
    ├── controller
    ├── integration
    ├── service
    └── util
```

## Possible improvements

- Authentication and per-user link ownership
- Rate limiting for creation and redirect endpoints
- Asynchronous click-event ingestion through a message broker
- Geographic analytics using privacy-preserving enrichment
- Metrics, tracing, and production health endpoints
- Scheduled cleanup or archival of expired links
- Multiple application replicas with load testing
