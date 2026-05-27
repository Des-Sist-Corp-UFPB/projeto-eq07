# 🤖 Project Context for AI

This document serves as a context guide to help you understand the architecture, tech stack, and guidelines of this project. Use this information as a foundation for answering commands, generating code, or debugging issues.

---

## 🚀 1. Project Overview

- **Project Name:** Corridas
- **Application Type:** Full-Stack Application
- **Backend:** Spring Boot (Java)
- **Frontend:** Vue.js
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
├── frontend/                       # Project frontend (Vue.js)
│   └── src/                        # Frontend source code
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

---

## 🎨 7. Frontend Development Guidelines (Vue.js)

When generating code, screens, or components for the user interface, follow the structural and technical governance rules below, based on the **Vue.js 3** ecosystem.

### 🚨 Golden Rules (Critical)

1. **Absolute Reuse and Generalization:** Before creating any new component, analyze the `src/components/` folder to check whether it already exists. If an existing component is similar to what you need, **refactor and generalize it** (via *props* and *slots*) to cover both use cases instead of duplicating code.
2. **Focus on Spring Boot Integration:** All data flow, form handling, and state management should be designed with the Spring Boot API consumption in mind. Keep property names in `camelCase` and align objects with backend DTOs.
3. **Strict Environment Variable Documentation:** Whenever you suggest or create a new environment variable (in the `.env` file), you **must document and thoroughly explain why it is needed** and where it applies.
4. **Dependency Minimalism:** Avoid installing new dependencies (`npm install`). Try to solve problems using native Vue 3 features, plain CSS, or already-installed utilities. New libraries should only be suggested if strictly necessary and justified.

---

### 📁 Standard Folder Structure (Vue.js)

```
├── src/
│   ├── assets/          # Images, icons, and global style files
│   ├── components/      # GENERIC and REUSABLE components (BaseButton, BaseModal)
│   ├── composables/     # Shared logic / Custom Hooks (useAuth, useFetch)
│   ├── router/          # Route configuration (Vue Router) and Route Guards
│   ├── stores/          # Global state management (Pinia), if needed
│   ├── views/           # Main pages tied to routes (LoginView, DashboardView)
│   ├── services/        # HTTP clients (Axios) and requests to the Spring Boot API
│   ├── App.vue          # Root application component
│   └── main.js/ts       # Entry point that initializes the Vue instance
├── .env.example         # Example of required environment variables
└── package.json         # Project dependencies (keep as clean as possible)
```