# PulseWatch

**PulseWatch** is an API/URL uptime monitoring platform built with Spring Boot. Users register HTTP endpoints they care about; a background scheduler concurrently checks those endpoints on a per-monitor interval, records every check as an immutable fact, and flips monitor status between `UP`/`DOWN` using a consecutive-failure/recovery state machine — the same core mechanism real tools like UptimeRobot or Pingdom are built around.

> **Status: Core monitoring loop is implemented and tested end-to-end** — auth, monitor CRUD, the concurrent scheduler/checker, and the failure/recovery state machine all work and have been verified manually. Incident lifecycle tracking, alerting, and deployment are still in progress — see [Still To Build](#still-to-build).

---

## Tech Stack

- **Java 25**, **Spring Boot 4.1.0**
- **MySQL** (hosted on Aiven free tier)
- Maven, JPA/Hibernate, Spring Security, JWT (`jjwt` 0.11.5), Lombok
- Java virtual threads (Project Loom) for concurrent HTTP checks

---

## Feature 1: JWT Authentication Tested end-to-end in Postman

- `User` entity, `UserRepository`
- `RegisterRequest` / `LoginRequest` / `AuthResponse` DTOs with validation
- `JwtUtil` — HS256 token generation/validation, secret key loaded via `@Value` from `application.properties` (Base64-decoded, constructor-injected, not regenerated on restart)
- `AuthService` — registration and login logic, BCrypt password hashing
- `AuthController` — `/auth/register`, `/auth/login`
- `JwtAuthenticationFilter` — validates Bearer tokens on every request, populates `SecurityContextHolder`
- `SecurityConfig` — stateless sessions, CSRF disabled (not applicable to token-based auth), `/auth/**` open, all other endpoints require authentication

## Feature 2: Monitor CRUD  Tested end-to-end in Postman

- `Monitor` entity — `@ManyToOne(LAZY)` to `User`, `MonitorStatus`/`HttpMethod` enums (`EnumType.STRING`), `@PrePersist` sets `createdAt` and defaults `status` to `PENDING`
- `MonitorRepository` — `findByUserId`, plus a sargable native query (`findDueMonitors`) for scheduler use
- `MonitorRequest`/`MonitorResponse` DTOs — validated inputs (URL pattern check, minimum 30-second check interval, chosen deliberately after modeling storage growth and false-alarm trade-offs rather than copying industry defaults)
- `MonitorService` — full CRUD with IDOR-prevention via `findOwnedMonitor` (ensures users can only access their own monitors)
- `MonitorController` — `POST/GET/PUT/DELETE /monitors`, user identity extracted from JWT via `Authentication.getDetails()`

## Feature 3: Scheduler + HTTP Checker  Verified against real URLs

- `MonitorScheduler` — `@Scheduled` job polling every 10 seconds for due monitors, dispatches each check onto its own virtual thread (`Executors.newVirtualThreadPerTaskExecutor()`) for concurrent, non-blocking checks
- `MonitorCheckService` — performs the actual HTTP request via Java's `HttpClient`, respects per-monitor timeout, categorizes failures (`TIMEOUT`, `CONNECTION_REFUSED`, `DNS_FAILURE`, `STATUS_MISMATCH`, `UNKNOWN_ERROR`)
- `CheckResult` — immutable Java record capturing one check's outcome
- `MonitorCheck` entity — immutable, insert-only record of every check performed (raw-facts table, never updated)
- `MonitorCheckRepository`
- Verified against real URLs (Google, own deployed backend) with correct status codes and timing

## Feature 4: Failure/Recovery State Machine  

- `consecutiveFailures` / `consecutiveSuccesses` counters added to `Monitor`
- `applyCheckResult` logic: flips `status` to `DOWN` after 2 consecutive failures, back to `UP` after 2 consecutive successes
- Threshold of 2 chosen after simulating false-alarm rate vs. detection delay across different thresholds, rather than picking arbitrarily
- Verified via `httpstat.us`: correct flip timing in both directions

---

## Architectural Decisions Worth Highlighting

- **Stateless JWT auth** (no server-side sessions) — deliberately chosen for horizontal scalability
- **Raw facts vs. derived state** — `monitor_checks` is immutable history; `Monitor.status` is a maintained summary derived from it, not a second source of truth
- **Sargable SQL queries** — parameter-side date math (`DATE_SUB(NOW(), INTERVAL ? SECOND)`), not column-side, to preserve index usage on the scheduler's hot-path query
- **Consecutive-check thresholds instead of single-check triggers** — avoids false alarms from transient network blips, at the cost of a small detection delay (a tradeoff, not a free win)
- **Concurrent, non-blocking checks via virtual threads** — avoids both OS-thread exhaustion under load and the complexity of a reactive stack, while still checking many monitors in parallel

---

## Data Model

```
users
 └── id, email, password_hash, created_at

monitors
 └── id, user_id (FK), url, http_method, check_interval_seconds,
     status, consecutive_failures, consecutive_successes, created_at

monitor_checks   (immutable — insert only)
 └── id, monitor_id (FK), checked_at, response_time_ms, status_code,
     success, failure_reason

incidents        (derived — not yet populated by application logic)
 └── id, monitor_id (FK), started_at, resolved_at, status
```

---

## Still To Build

- Incident lifecycle tracking with MTTR (mean time to recovery)
- Redis distributed locking (so the scheduler is safe to run on more than one instance)
- Email alerts with debouncing
- Metrics endpoint
- Docker Compose containerization
- Deployment to Render (app) + Aiven (already used for MySQL)
- Environment-variable-based configuration (secrets currently live in `application.properties`)

---

## Getting Started

```bash
# clone
git clone <repo-url>
cd pulsewatch

# configure src/main/resources/application.properties with:
#   - your MySQL connection URL, username, and password
#   - a Base64-encoded JWT signing secret

# run
./mvnw spring-boot:run
```

Requires Java 25 and a running MySQL instance. Once running:
1. `POST /auth/register` and `POST /auth/login` to get a JWT
2. Use the JWT in an `Authorization: Bearer <token>` header for all `/monitors` endpoints
3. Register a monitor — the scheduler will begin checking it automatically once its interval is due

---

## Author

Built by Toshit .