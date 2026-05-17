# Appliance Store

[![CI](https://github.com/Vovan4ok/appliance-store/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Vovan4ok/appliance-store/actions/workflows/ci.yml)

A full-stack e-commerce web application for managing and selling home appliances, built with Spring Boot 3.2 and Java 17.

## Tech Stack

- **Backend:** Spring Boot 3.2, Spring MVC, Spring Security, Spring Data JPA
- **Auth:** Stateless JWT authentication (HttpOnly cookie for MVC; Bearer token for REST API)
- **Email:** Spring Mail — async HTML notifications rendered from Thymeleaf templates
- **REST API:** Jackson, SpringDoc OpenAPI / Swagger UI
- **Frontend:** Thymeleaf, Bootstrap 5
- **Database:** PostgreSQL (prod) / H2 (dev)
- **Migrations:** Flyway
- **Containerization:** Docker, Docker Compose
- **Build:** Maven
- **Code style:** Checkstyle (Google-inspired ruleset — `checkstyle.xml`)
- **CI:** GitHub Actions

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
- Approve / unapprove submitted orders — approval validates and decrements stock; blocked with a clear error if any item is out of stock
- View all orders across all clients
- Client and employee list tables show phone and date of birth
- Profile page — edit profile details, change password, upload avatar

### REST API
- JWT Bearer token authentication — `POST /api/v1/auth/login` returns a token; pass it as `Authorization: Bearer <token>`
- Appliances — full CRUD (`GET`/`POST`/`PUT`/`DELETE`); paginated list with all filters
- Manufacturers — full CRUD; paginated list with optional `search` param
- Orders — list (EMPLOYEE sees all, CLIENT sees own); approve endpoint (EMPLOYEE only)
- Clients — read-only list and detail
- Consistent JSON error responses (status, error, message, path) for all 4xx/5xx cases
- Interactive docs at `/swagger-ui.html` with built-in Bearer auth support

### Email Notifications
- HTML emails on key events: registration welcome, order submitted, order approved, and a password-changed security alert
- Decoupled via Spring application events; sent asynchronously after the database transaction commits, so a mail failure never blocks or breaks the user's request
- Rendered from Thymeleaf templates; local development uses a [Mailpit](https://mailpit.axllent.org/) SMTP catcher (web UI at `http://localhost:8025`)

### General
- Role-based access control (CLIENT / EMPLOYEE)
- Custom password validation (min 8 chars, uppercase, lowercase, special character)
- Phone number validation — 7–15 digits, optional `+` prefix, spaces/dashes/parentheses allowed
- Pagination and sorting on all list views
- Internationalization — English and Ukrainian (toggle via `?lang=uk`)
- AOP-based method logging

## Development

```bash
# Run all tests
mvn test

# Check code style
mvn checkstyle:check

# Run tests + checkstyle together (matches CI)
mvn verify
```

## Getting Started

### Run locally (dev profile — no Docker needed)

```bash
mvn spring-boot:run
```

App starts at `http://localhost:8080` with an H2 in-memory database seeded with sample data.
H2 console available at `http://localhost:8080/h2-console` (user: `sa`, password: `password`).

### Run with Docker (prod profile — PostgreSQL)

```bash
cp .env.example .env
# fill in your values in .env
docker compose up --build
```

App starts at `http://localhost:8080` backed by PostgreSQL with Flyway-managed schema.
Avatar uploads are stored in the `app_uploads` Docker volume.
A [Mailpit](https://mailpit.axllent.org/) container catches all outgoing email — open its inbox at `http://localhost:8025`.

## Configuration Profiles

| Profile | Database | Flyway | Usage |
|---------|----------|--------|-------|
| `dev` (default) | H2 in-memory | disabled | Local development |
| `test` | H2 in-memory | disabled | Automated tests |
| `prod` | PostgreSQL | enabled | Docker / production |

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
│   ├── security/        # JWT filter and utilities
│   ├── service/         # Business logic interfaces and implementations
│   ├── event/           # Application events (registration, orders, password)
│   ├── listener/        # Email notification listener
│   ├── aspect/          # AOP logging
│   └── validation/      # Custom password constraint
├── main/resources/
│   ├── db/migration/    # Flyway SQL migrations (V1–V4)
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
| `JWT_SECRET` | Secret key for JWT signing (min 32 characters) |
| `UPLOAD_DIR` | Filesystem path for avatar uploads (default: `uploads`) |
| `MAIL_HOST` | SMTP server host |
| `MAIL_PORT` | SMTP server port (default: `587`) |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |
| `MAIL_FROM` | "From" address for outgoing email (default: `no-reply@appliance-store.com`) |
| `MAIL_ENABLED` | Set `false` to disable email sending (default: `true`) |

> Under Docker Compose these default to the bundled Mailpit container — no configuration needed.