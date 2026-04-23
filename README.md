# Appliance Store

[![CI](https://github.com/Vovan4ok/appliance-store/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Vovan4ok/appliance-store/actions/workflows/ci.yml)

A full-stack e-commerce web application for managing and selling home appliances, built with Spring Boot 3.2 and Java 17.

## Tech Stack

- **Backend:** Spring Boot 3.2, Spring MVC, Spring Security, Spring Data JPA
- **Auth:** Stateless JWT authentication (HttpOnly cookie)
- **Frontend:** Thymeleaf, Bootstrap 5
- **Database:** PostgreSQL (prod) / H2 (dev)
- **Migrations:** Flyway
- **Containerization:** Docker, Docker Compose
- **Build:** Maven
- **CI:** GitHub Actions

## Features

### Client
- Browse and filter appliances by category, manufacturer, power type, price, and availability ("In stock only")
- Out-of-stock appliances shown with a blurred overlay — visible but unorderable
- Shopping cart — add items, update quantities, remove rows; warns if a cart item goes out of stock
- Submit orders for employee review
- View order history
- Profile page — edit name, phone, date of birth, card number; change password; upload avatar

### Employee
- Full CRUD for appliances, manufacturers, clients, and employees
- Appliance list filters: name, category, power type, manufacturer, price range, sort, and "Out of stock only"
- Out-of-stock appliance cards shown with a lighter blur overlay — info still readable, edit/delete still clickable
- Manage stock levels directly on the appliance create/edit form
- Approve / unapprove submitted orders — approval validates and decrements stock; blocked with a clear error if any item is out of stock
- View all orders across all clients
- Client and employee list tables show phone and date of birth
- Profile page — edit profile details, change password, upload avatar

### General
- Role-based access control (CLIENT / EMPLOYEE)
- Custom password validation (min 8 chars, uppercase, lowercase, special character)
- Phone number validation — 7–15 digits, optional `+` prefix, spaces/dashes/parentheses allowed
- Pagination and sorting on all list views
- Internationalization — English and Ukrainian (toggle via `?lang=uk`)
- AOP-based method logging

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
│   ├── config/          # Security, MVC, JWT config
│   ├── controller/      # MVC controllers + global exception handler
│   ├── exception/       # Custom exceptions (InsufficientStockException)
│   ├── model/           # JPA entities and DTOs
│   ├── repository/      # Spring Data JPA repositories + Criteria API spec
│   ├── security/        # JWT filter and utilities
│   ├── service/         # Business logic interfaces and implementations
│   ├── aspect/          # AOP logging
│   └── validation/      # Custom password constraint
├── main/resources/
│   ├── db/migration/    # Flyway SQL migrations (V1–V4)
│   ├── messages/        # i18n message files (EN, UK)
│   ├── templates/       # Thymeleaf HTML templates
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