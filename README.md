# Appliance Store

[![CI](https://github.com/Vovan4ok/appliance-store/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Vovan4ok/appliance-store/actions/workflows/ci.yml)
[![Security](https://github.com/Vovan4ok/appliance-store/actions/workflows/security.yml/badge.svg?branch=master)](https://github.com/Vovan4ok/appliance-store/actions/workflows/security.yml)

A full-stack e-commerce web application for managing and selling home appliances, built with Spring Boot 4.1 and Java 17.

## Tech Stack

- **Backend:** Spring Boot 4.1 (Spring Framework 7), Spring MVC, Spring Security 7, Spring Data JPA (Hibernate 7)
- **Auth:** Stateless JWT authentication (HttpOnly cookie for MVC; Bearer token for REST API), revocable on logout
- **Redis:** JWT logout deny-list, AI description cache (Spring Cache), per-user AI rate limit — in-memory fallbacks in dev/test
- **Email:** Spring Mail — async HTML notifications rendered from Thymeleaf templates
- **AI:** Spring AI 2.0 (`ChatClient`) + OpenAI — product description generator
- **REST API:** Jackson 3, SpringDoc OpenAPI 3 / Swagger UI
- **Frontend:** Thymeleaf, Bootstrap 5
- **Database:** PostgreSQL (prod) / H2 (dev)
- **Migrations:** Flyway
- **Containerization:** Docker, Docker Compose (app, PostgreSQL, Redis, Mailpit, Prometheus, Grafana)
- **Observability:** Micrometer metrics (HTTP, JVM, database pool, Redis, Spring AI + custom business metrics) scraped by Prometheus, provisioned Grafana dashboard; Actuator health check
- **Testing:** JUnit 5, Mockito, MockMvc, Testcontainers (real Redis in Docker)
- **Build:** Maven
- **Code style:** Checkstyle (Google-inspired ruleset — `checkstyle.xml`)
- **CI:** GitHub Actions
- **Dependency security:** OWASP Dependency-Check in CI (fails on CVSS ≥ 7, findings in GitHub's Security tab), Dependabot updates

## Features

### Client
- Browse and filter appliances by category, manufacturer, power type, price, and availability ("In stock only")
- Out-of-stock appliances shown with a blurred overlay — visible but unorderable
- Shopping cart — add items, update quantities, remove rows; warns if a cart item goes out of stock
- Submit orders for employee review
- View order history
- Manufacturer list (card-grid) with name search and pagination; click any card to open the detail page with full info and a "Browse Appliances" link filtered to that brand
- Profile page — edit name, phone, date of birth, card number; change password; upload avatar

### Employee
- Full CRUD for appliances, manufacturers, clients, and employees; manufacturer detail page with Edit + Delete actions and a "Browse Appliances" link
- Appliance list filters: name, category, power type, manufacturer, price range, sort, and "Out of stock only"
- Out-of-stock appliance cards shown with a lighter blur overlay — info still readable, edit/delete still clickable
- Manage stock levels directly on the appliance create/edit form
- "AI suggest" button on the appliance edit page — fills the description with an AI marketing text in the current UI language (English or Ukrainian), with undo; the employee reviews it and saves (see [AI Product Descriptions](#ai-product-descriptions))
- Approve / unapprove submitted orders — approval validates and decrements stock; blocked with a clear error if any item is out of stock
- View all orders across all clients
- Client and employee list tables show phone and date of birth
- Profile page — edit profile details, change password, upload avatar

### REST API
- JWT Bearer token authentication — `POST /api/v1/auth/login` returns a token; pass it as `Authorization: Bearer <token>`; `POST /api/v1/auth/logout` revokes it immediately
- Appliances — full CRUD (`GET`/`POST`/`PUT`/`DELETE`); paginated list with all filters
- Manufacturers — full CRUD; paginated list with optional `search` param
- AI description — `POST /api/v1/appliances/{id}/ai-description?lang=en|uk` (EMPLOYEE only)
- Orders — list (EMPLOYEE sees all, CLIENT sees own); approve endpoint (EMPLOYEE only)
- Clients — read-only list and detail
- Consistent JSON error responses (status, error, message, path) for all 4xx/5xx cases
- Interactive docs at `/swagger-ui.html` with built-in Bearer auth support

### Email Notifications
- HTML emails on key events: registration welcome, order submitted, order approved, and a password-changed security alert
- Decoupled via Spring application events; sent asynchronously after the database transaction commits, so a mail failure never blocks or breaks the user's request
- Rendered from Thymeleaf templates; local development uses a [Mailpit](https://mailpit.axllent.org/) SMTP catcher (web UI at `http://localhost:8025`)

### AI Product Descriptions
- `POST /api/v1/appliances/{id}/ai-description?lang=en|uk` (EMPLOYEE only) generates a marketing description from the appliance's name, manufacturer, model, category, power and characteristics
- Built with Spring AI (`ChatClient`) + OpenAI; the output language comes from the app's i18n (`?lang=`, unsupported languages fall back to English)
- Returns a **suggestion** — nothing is saved until an employee reviews it and updates the appliance
- In the UI: **AI suggest** on the edit page inserts the text into the description field (with **Undo**); if AI is off or fails, a notice is shown and the current text is kept
- **Off by default**: enable with `AI_CHAT_PROVIDER=openai` and `OPENAI_API_KEY` (environment only — never committed)

Example response:

```json
{
  "applianceId": 58,
  "language": "en",
  "description": "Start every morning with perfectly golden toast...",
  "source": "AI",
  "fallbackReason": null
}
```

**How hallucination is controlled**
- The prompt allows only the facts passed in; inventing specs, prices, warranties or comparisons is forbidden, and so are unstated claims about durability, size, design, materials or usage
- Placeholder values (e.g. `-----`) are removed before sending, and units follow the output language (Вт, В, л in Ukrainian)
- No characteristics, no AI copy: a product without real characteristics is never sent to the model (it could only invent things) — the existing description is kept with `fallbackReason` = `INSUFFICIENT_DATA`
- Price and stock are deliberately not sent, so the model can't quote stale numbers
- Enum values are sent as plain words (`SMALL` → "small appliance", `AC220` → "220 V mains"), so internal codes don't leak into the copy
- Product data is sanitized (control characters and `<>` stripped, each field capped at 300 chars) and wrapped in a `<product>` data block the model is told never to obey — a prompt-injection guard
- A human reviews every suggestion before it is saved

**How cost and failures are controlled**
- Model pinned to `gpt-5-mini` (override with `SPRING_AI_OPENAI_CHAT_MODEL`) so a Spring AI upgrade can't silently change it
- Output capped at 500 completion tokens; the result is trimmed to 1000 chars (the DB column size)
- `reasoning-effort=minimal`: `gpt-5-mini` is a reasoning model whose hidden reasoning tokens count against the cap — with the default effort a 300-token budget was used up before any text was written, so the effort is kept minimal for a short, fact-bound copywriting task
- 15 s timeout and one retry, then a graceful fallback to the existing description (`"source": "FALLBACK"` with `fallbackReason` = `AI_DISABLED`, `INSUFFICIENT_DATA`, `AI_ERROR` or `EMPTY_RESPONSE`) — the endpoint never fails because of the AI provider
- Employee-only endpoint, so shoppers can't run up the bill
- **Cached in Redis for 7 days** — repeat requests are served in ~30 ms instead of ~2–4 s and cost nothing; the key includes a hash of the product data and the prompt, so editing either never serves a stale text; fallbacks are never cached
- **Rate-limited** to 10 AI requests per employee per minute (`app.ai.rate-limit.per-minute`) — the 11th gets `429 Too Many Requests` with a `Retry-After` header, and the UI shows a "please wait" notice
- Only the chat model is enabled; embedding, image, audio and moderation models are switched off
- Tests mock `ChatClient`, and the test profile forces `spring.ai.model.chat=none`, so CI never calls the real API

### Redis
Redis backs three features, each behind an interface with an in-memory implementation for dev/test (`app.redis.enabled`, on in the `prod` profile) — so local development still needs no Docker:

| Feature | How it works in Redis |
|---------|-----------------------|
| **Logout that really logs out** | Every JWT has a unique id (`jti`). Logout (web or `POST /api/v1/auth/logout`) stores it in a deny-list (`jwt:denylist:<jti>`) with a TTL equal to the token's remaining lifetime, and the JWT filter rejects listed tokens — a copied token stops working immediately, and Redis cleans the entry up by itself |
| **AI description cache** | Spring Cache with `RedisCacheManager`, 7-day TTL (`aiDescriptions::<id>:<lang>:<hash>`), written asynchronously so caching never slows a response |
| **AI rate limit** | Fixed one-minute window per employee: atomic `INCR` on `ai:rate:<user>:<minute>` with a short TTL — shared by all app instances |

**When Redis is down the app degrades instead of failing:** the cache counts as a miss, the rate limit and the deny-list are skipped (fail-open, so an outage doesn't log everyone out), each with a warning in the log, and `/actuator/health` (port 8081 in prod) reports `redis: DOWN` (HTTP 503). The Redis client rejects commands while disconnected instead of queueing them, so requests don't wait for timeouts during an outage, and everything recovers automatically. Redis runs with an append-only file, so revoked tokens survive a Redis restart.

### Dependency Security
- **OWASP Dependency-Check** (`.github/workflows/security.yml`) scans every push, every pull request and, because new CVEs appear even when the code doesn't change, every Monday. It checks all runtime dependencies against the NVD and the CISA *Known Exploited Vulnerabilities* list
- The build **fails on any vulnerability with CVSS 7.0 or higher**; findings show up in the repository's **Security → Code scanning** tab (SARIF), and the HTML report is attached to each run
- Reviewed false positives go into `dependency-check-suppressions.xml` with a reason and an expiry date, so they get looked at again
- **Dependabot** (`.github/dependabot.yml`) opens weekly PRs for Maven dependencies, GitHub Actions and Docker images (Dockerfile and docker-compose) — minor/patch updates grouped, major versions one by one — and each PR runs the full CI before merging
- Run the scan locally (needs a free [NVD API key](https://nvd.nist.gov/developers/request-an-api-key)):

  ```bash
  ./mvnw dependency-check:check -DnvdApiKey=<your key>
  # report: target/dependency-check-report.html
  ```

### Observability
The app exports metrics in Prometheus format (Micrometer); `docker compose up` also starts **Prometheus** (`http://localhost:9090`) and **Grafana** (`http://localhost:3000`, login from `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`, default `admin` / `admin`), which opens the ready-made **Appliance Store** dashboard:

| Dashboard section | What it shows |
|-------------------|---------------|
| **Overview** | App up, requests/s, 5xx error rate, p95 latency, pending orders, out-of-stock appliances |
| **HTTP** | Requests by status, slowest endpoints (p95 per URI) |
| **Orders** | Submitted / approved / unapproved orders per hour, approved orders and revenue in the selected range |
| **AI descriptions** | Requests by outcome (generated / cached / fallback + reason), OpenAI latency p50/p95, cache hit ratio, rate-limited requests, OpenAI tokens used |
| **Authentication** | Successful and failed logins (web / API), logouts |
| **JVM and database** | Heap, CPU, HikariCP connections (active / idle / waiting) |

- **Custom business metrics** (`metrics/StoreMetrics`): `store_orders_total{event}`, `store_orders_approved_amount`, `store_ai_descriptions_seconds{outcome,reason}`, `store_ai_rate_limited_total`, `store_auth_logins_total{channel,result}`, `store_auth_logouts_total{channel}`, plus the gauges `store_orders_pending` and `store_appliances_out_of_stock` (read from the database on each scrape). Order events are counted only after their transaction commits, and tags only ever take a few fixed values (never user names or ids), which keeps the number of time series small
- Spring AI adds its own metrics (`gen_ai_client_operation_seconds`, `gen_ai_client_token_usage_total`), so OpenAI errors and token spend are visible too
- **Metrics are never public:** in the `prod` profile `/actuator/health` and `/actuator/prometheus` are served only on the management port **8081**, which Docker Compose publishes on localhost only (Prometheus scrapes it inside the Docker network); port 8080 has no actuator endpoints. In `dev` both are on 8080
- Dashboards, data source and scrape config live in the repo (`observability/`), so the whole setup comes up with no clicking; change the dashboard JSON and restart Grafana to update it

### General
- Role-based access control (CLIENT / EMPLOYEE)
- Health check at `/actuator/health` (database, Redis in prod, disk) — with `/actuator/prometheus` the only actuator endpoints exposed (management port 8081 in prod)
- Custom password validation (min 8 chars, uppercase, lowercase, special character)
- Phone number validation — 7–15 digits, optional `+` prefix, spaces/dashes/parentheses allowed
- Pagination and sorting on all list views
- Internationalization — English and Ukrainian (toggle via `?lang=uk`)
- AOP-based method logging

## Development

No Maven install needed — the project ships with the Maven wrapper (`mvnw` / `mvnw.cmd`).

```bash
# Run all tests
./mvnw test

# Check code style
./mvnw checkstyle:check

# Run tests + checkstyle together (matches CI)
./mvnw verify
```

> On Windows PowerShell use `.\mvnw.cmd` instead of `./mvnw`.

The Redis integration tests start a real Redis with [Testcontainers](https://testcontainers.com/), so they need Docker; without Docker they are skipped and everything else still runs.

## Getting Started

### Run locally (dev profile — no Docker needed)

```bash
./mvnw spring-boot:run
```

App starts at `http://localhost:8080` with an H2 in-memory database seeded with sample data (122 appliances, 10 in stock each; a few are seeded out of stock or low on stock to demo the UI).
H2 console available at `http://localhost:8080/h2-console` (user: `sa`, password: `password`).

To try the AI description generator, set the variables in your shell first (never in a committed file):

```bash
export AI_CHAT_PROVIDER=openai
export OPENAI_API_KEY=<your key>
./mvnw spring-boot:run
```

> PowerShell: `$env:AI_CHAT_PROVIDER="openai"; $env:OPENAI_API_KEY="<your key>"; .\mvnw.cmd spring-boot:run`. Optionally pick a model with `SPRING_AI_OPENAI_CHAT_MODEL`.

### Run with Docker (prod profile — PostgreSQL)

```bash
cp .env.example .env
# fill in your values in .env — JWT_SECRET is required, e.g. generate one with:
openssl rand -base64 48
docker compose up --build
```

> Compose refuses to start without `JWT_SECRET`, and the app itself fails fast if the secret is shorter than 32 bytes.

App starts at `http://localhost:8080` backed by PostgreSQL with Flyway-managed schema and Redis.
Avatar uploads are stored in the `app_uploads` Docker volume; Redis data (append-only file) in `redis_data`.
Health check: `http://localhost:8081/actuator/health` (management port, this machine only).
Metrics: Prometheus at `http://localhost:9090`, Grafana dashboard at `http://localhost:3000`.
A [Mailpit](https://mailpit.axllent.org/) container catches all outgoing email — open its inbox at `http://localhost:8025`.

## Configuration Profiles

| Profile | Database | Flyway | Usage |
|---------|----------|--------|-------|
| `dev` (default) | H2 in-memory | disabled | Local development |
| `test` | H2 in-memory | disabled | Automated tests |
| `prod` | PostgreSQL + Redis | enabled | Docker / production |

`dev` and `test` use in-memory replacements for the Redis features.

## Sample Credentials

| Role | Email | Password |
|------|-------|----------|
| Employee | `phobos@gmail.com` | `Phobos@123!` |
| Employee | `moon@gmail.com` | `Moon@123!` |
| Client | `mercury@gmail.com` | `Mercury@123!` |
| Client | `jupiter@gmail.com` | `Jupiter@123!` |

## Project Structure

```
src/
├── main/java/com/vovan4ok/appliance/store/
│   ├── config/          # Security, MVC, JWT, OpenAPI config
│   ├── controller/      # MVC controllers + global exception handler
│   │   └── api/         # REST API controllers (appliances, manufacturers, orders, clients, auth)
│   ├── exception/       # Custom exceptions (InsufficientStockException)
│   ├── model/           # JPA entities and DTOs
│   │   └── dto/api/     # REST request/response DTOs
│   ├── repository/      # Spring Data JPA repositories + Criteria API spec
│   ├── security/        # JWT filter and utilities, token deny-list (Redis / in-memory)
│   ├── service/         # Business logic interfaces and implementations (incl. AI description service)
│   ├── event/           # Application events (registration, orders, password)
│   ├── listener/        # Email notification + order metrics listeners
│   ├── metrics/         # Micrometer business metrics (StoreMetrics) and gauges
│   ├── aspect/          # AOP logging
│   └── validation/      # Custom password constraint
├── main/resources/
│   ├── db/migration/    # Flyway SQL migrations (V1–V6)
│   ├── messages/        # i18n message files (EN, UK)
│   ├── templates/       # Thymeleaf HTML templates (incl. email/ templates)
│   └── *.sql            # Dev seed data files
└── test/                # Unit and controller tests (Mockito, MockMvc)
```

## Environment Variables (prod)

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/appliance_store` |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | **Required** — secret key for JWT signing (min 32 bytes, no default; the app won't start without it) |
| `UPLOAD_DIR` | Filesystem path for avatar uploads (default: `uploads`) |
| `MAIL_HOST` | SMTP server host |
| `MAIL_PORT` | SMTP server port (default: `587`) |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |
| `MAIL_FROM` | "From" address for outgoing email (default: `no-reply@appliance-store.com`) |
| `MAIL_ENABLED` | Set `false` to disable email sending (default: `true`) |
| `AI_CHAT_PROVIDER` | `openai` enables the AI description generator (default: `none` — off) |
| `OPENAI_API_KEY` | OpenAI API key, required when `AI_CHAT_PROVIDER=openai` |
| `SPRING_AI_OPENAI_CHAT_MODEL` | Optional OpenAI model override (default: `gpt-5-mini`; `reasoning-effort=minimal` assumes a GPT-5-family model) |
| `REDIS_HOST` | Redis host (default: `localhost`; Docker Compose sets `redis`) |
| `REDIS_PORT` | Redis port (default: `6379`) |
| `MANAGEMENT_PORT` | Port for `/actuator/health` and `/actuator/prometheus` (default: `8081`) |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | Grafana login under Docker Compose (default: `admin` / `admin`) |

> Under Docker Compose the mail settings default to the bundled Mailpit container — no configuration needed.