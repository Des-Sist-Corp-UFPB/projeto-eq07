# Memória do Projeto — Corridas DSC/UFPB

## Identidade do Projeto
- **Nome**: Sistema Gerenciamento de Corridas — Projeto Base DSC
- **Disciplina**: Desenvolvimento de Sistemas Corporativos
- **Professor**: Rodrigo Rebouças
- **Instituição**: Universidade Federal da Paraíba — Campus IV
- **Propósito**: Boilerplate educacional para alunos iniciarem seus projetos

## Stack Técnica
| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 21 |
| Framework | Spring Boot | 3.4.5 |
| Build | Maven | 3.9+ |
| Templates | Thymeleaf + HTMX | 3.x + 2.0.4 |
| Frontend Tailwind
| Banco | PostgreSQL | 16 |
| Migrations | Flyway | 11.x |
| Segurança | Spring Security | 6.x |
| Testes | JUnit 5 + Testcontainers | - |

## 📂 Arquitetura do Projeto

Abaixo está a representação da estrutura de pastas e a organização arquitetural do sistema:

```text
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

## Comandos Essenciais

### Desenvolvimento
```bash
# Subir ambiente completo (banco + app + adminer)
docker compose -f docker/docker-compose.dev.yml up

# Só o banco (para rodar a app localmente com mvn)
docker compose -f docker/docker-compose.dev.yml up postgres adminer

# Rodar aplicação local (perfil dev)
mvn spring-boot:run

# Rodar testes (requer Docker para Testcontainers)
mvn test
```

### Build e Verificações
```bash
# Build sem testes
mvn clean package -DskipTests

# Build com testes
mvn clean verify

# SAST: SpotBugs + FindSecBugs + OWASP Dependency Check
mvn verify -Psecurity

# Verificar dependências desatualizadas
mvn versions:display-dependency-updates -Pversions

# Trivy local (scan filesystem)
docker compose -f docker/docker-compose.dev.yml --profile scan up trivy

# Trivy scan da imagem (depois de fazer o build)
docker build -f docker/Dockerfile -t mercado:latest .
docker run --rm aquasec/trivy image mercado:latest
```

### Produção
```bash
# Build imagem de produção
docker build -f docker/Dockerfile -t mercado:latest .

# Subir produção (requer .env configurado)
docker compose -f docker/docker-compose.prod.yml up -d
```

## Acesso Local
- **App**: http://localhost:8080
- **Adminer (DB UI)**: http://localhost:8888
- **Health Check**: http://localhost:8080/actuator/health

### Por que Flyway para migrations?
Controle versionado do schema do banco. Cada alteração no banco deve ser uma migration nova (nunca editar migrations já aplicadas). Garante rastreabilidade e reversibilidade.


## Convenções de Código
- Nomes em português no domínio (entidades, métodos de negócio)
- Endpoints REST em português
- Comentários em português
- Commits no padrão Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`
- Records Java para DTOs (imutáveis por padrão)
- `@Transactional(readOnly = true)` em métodos de consulta

## Ferramentas de Segurança
| Ferramenta | Escopo | Comando |
|------------|--------|---------|
| SpotBugs + FindSecBugs | SAST bytecode Java | `mvn verify -Psecurity` |
| Semgrep | SAST código-fonte | `semgrep --config=auto src/` |
| Trivy (fs) | Vulnerabilidades em libs | docker compose `--profile scan` |
| Trivy (image) | Vulnerabilidades na imagem Docker | `trivy image mercado:latest` |
| OWASP Dependency-Check | CVEs em dependências | `mvn verify -Psecurity` |
