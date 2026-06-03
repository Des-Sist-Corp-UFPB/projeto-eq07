# 🤖 Project Context for AI

This document serves as a context guide to help you understand the architecture, tech stack, and guidelines of this project. Use this information as a foundation for answering commands, generating code, or debugging issues.

---

## 🚀 1. Project Overview

- **Project Name:** Corridas
- **Application Type:** Full-Stack Application
- **Backend:** Spring Boot (Java)
- **Goal:** Application for managing road races

---

## 🛠️ 2. Tech Stack & Infrastructure

When generating code or configurations, make sure to respect the following versions and technologies:

- **Language:** Java 17 (or higher)
- **Framework:** Spring Boot 3.x
- **Dependency Manager:** Maven
- **Database:** PostgreSQL
- **Containerization:** Docker & Docker Compose
- **CI/CD:** GitHub Actions with deployment to GitHub Container Registry (`ghcr.io`)

---

## ⚙️ 3. Environment Configuration (Properties)

The development and staging environments use a PostgreSQL database with the following settings (sensitive credentials are injected via environment variables in the container):

- `spring.datasource.url` = `jdbc:postgresql://postgres:5432/eq07`
- `spring.datasource.username` = `${DB_USERNAME}` *(Injected via GitHub Secrets in production/CI)*
- `spring.datasource.password` = `${DB_PASSWORD}` *(Injected via GitHub Secrets in production/CI)*
- `spring.datasource.driver-class-name` = `org.postgresql.Driver`

---

## 📦 4. CI/CD Flow (GitHub Actions)

Every change to the `main` branch triggers an automatic workflow that:

1. Builds the Java application with Maven
2. Creates the Docker image
3. Authenticates and pushes the image to the official repository on `ghcr.io`

---

## 🧠 5. Code Generation Guidelines

Whenever generating code for this project, follow these rules:

- **Code Standards:** Follow Clean Code best practices and native Spring patterns (business rules in Services, endpoints in Controllers)
- **Database:** Use Flyway for database migration management
- **Token Usage:** Always aim to minimize token consumption
- **Security:** Never expose passwords or sensitive data directly in code or `application.properties` files. Always use environment variable placeholders (e.g. `${MY_VARIABLE}`)
- **Error Handling:** Use `@ControllerAdvice` with semantic and clear HTTP status responses
- **Responses:** Be direct, provide clean solutions, and briefly explain the suggested changes

---

## 📂 6. Project Structure (Project Tree)

The project follows the standard Spring Boot layered structure. Below is the mapping of the main directories and files:

```
├── .github/
│   └── workflows/
│       └── deploy.yml              # GitHub Actions workflow (CI/CD)
├── src/
│   ├── main/
│   │   ├── java/com/equipe07/projeto/
│   │   │   ├── config/             # Global configurations (Security, CORS, etc.)
│   │   │   ├── controller/         # REST endpoints (API entry point)
│   │   │   ├── dto/                # Data Transfer Objects (Request and Response)
│   │   │   ├── exception/          # Error handling and @ControllerAdvice
│   │   │   ├── model/              # Database entities (JPA/Hibernate)
│   │   │   ├── repository/         # Database access interfaces (Spring Data JPA)
│   │   │   ├── service/            # Business rules and internal logic layer
│   │   │   └── ProjetoApplication.java  # Main class that bootstraps Spring
│   │   └── resources/
│   │       ├── application-prod.properties  # Application configuration
│   │       └── db/migration/       # Flyway scripts
│   └── test/                       # Unit and Integration Tests (JUnit/Mockito)
├── Dockerfile                      # Docker image build instructions
├── docker-compose.yml              # Local orchestration (Spring + Postgres)
├── pom.xml                         # Maven dependencies and build
└── README.md                       # Documentation for human developers
```