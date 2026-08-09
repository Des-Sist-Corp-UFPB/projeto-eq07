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
PROJETO-EQ07/
├── .github/                      # Configurações do GitHub (CI/CD e Workflows)
│   └── workflows/
│       ├── ci.yml                # Integração Contínua
│       └── deploy.yml            # Deploy Automatizado
├── docker/                       # Arquivos e configurações do Docker
├── docs/                         # Documentação técnica do projeto
├── logs/                         # Registros de log da aplicação
├── src/
│   ├── main/
│   │   ├── java/br/ufpb/dsc/corrida/
│   │   │   ├── config/           # Classes de configuração (Segurança, CORS, etc.)
│   │   │   ├── exception/        # Tratamento global de exceções da API
│   │   │   ├── home/             # Lógica relacionada à página/fluxo inicial
│   │   │   ├── user/             # Módulo de Usuários (Domínio Principal)
│   │   │   │   ├── dto/          # Objetos de Transferência de Dados (Data Transfer Objects)
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── Genero.java
│   │   │   │   ├── NivelCondicionamento.java
│   │   │   │   ├── Papel.java
│   │   │   │   ├── User.java     # Entidade de Domínio
│   │   │   │   ├── UserInfo.java # Entidade complementar
│   │   │   │   ├── UserInfoRepository.java
│   │   │   │   ├── UserInfoService.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── UsuarioController.java       # Endpoints REST da API
│   │   │   │   ├── UsuarioService.java          # Regras de Negócio
│   │   │   │   └── UsuarioViewController.java   # Controle de Views (Thymeleaf)
│   │   │   └── CorridaApplication.java          # Classe Principal do Spring Boot
│   │   └── resources/
│   │       ├── db/               # Scripts de banco de dados / Migrations (Flyway/Liquibase)
│   │       ├── public/           # Arquivos estáticos globais (CSS, JS, Imagens)
│   │       ├── templates/        # Views HTML renderizadas pelo servidor (Thymeleaf)
│   │       │   ├── auth/         # Telas de login/autenticação
│   │       │   ├── fragments/    # Componentes HTML reutilizáveis (Header, Footer, etc.)
│   │       │   ├── index.html
│   │       │   ├── minha-conta.html
│   │       │   └── perfil-publico.html
│   │       ├── application-dev.yml   # Propriedades de Desenvolvimento
│   │       ├── application-prod.yml  # Propriedades de Produção
│   │       ├── application-test.yml  # Propriedades de Teste
│   │       └── application.yml       # Configurações Gerais do Spring
│   └── test/                     # Estrutura de Testes Automatizados
│       └── java/br/ufpb/dsc/corrida/
│           ├── controller/       # Testes de Unidade e Integração dos Endpoints
│           │   ├── UserInfoControllerTest.java
│           │   └── UserInfoIntegrationTest.java
│           ├── service/userinfo/ # Testes das Regras de Negócio
│           │   └── UserInfoServiceTest.java
│           └── CorridaApplicationTests.java

```