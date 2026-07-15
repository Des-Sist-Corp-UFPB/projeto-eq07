## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21 + Spring Boot 3.4.5 |
| Templates | Thymeleaf + HTMX 2.0 |
| Frontend | Tailwind 5.3 |
| Banco | PostgreSQL 16 |
| Migrações | Flyway 11 |
| Segurança | Spring Security 6 |
| Build | Maven 3.9 |
| CI/CD | GitHub Actions |

---

## Environment Setup

| Variable | Required | Purpose |
|---|---|---|---|
| `ORS_API_KEY` | Yes | Route calculation and address geocoding via OpenRouteService 
| `ORS_BASE_URL` | No | ORS base URL (defaults to production) |
| `API_SECURITY_TOKEN_SECRET` | Yes | JWT to authetication | 
| `SPRING_DATA_MONGODB_URI` | Yes | URI to connect to the audit log database |
| `DATABASE_URL` | Yes | URL to connect application database |
| `DB_USERNAME` | Yes | Username to connect Postgres database |
| `DB_PASSWORD` | Yes | Password to connect Postgres database |

---

## Testes

```bash
# Rodar todos os testes (requer Docker em execução — usa Testcontainers)
mvn test

# Rodar com relatório de cobertura (JaCoCo)
mvn verify
# Relatório: abra o arquivo target/site/jacoco/index.html no browser
```

---

## Análise de Segurança (SAST)

```bash
# SpotBugs + FindSecBugs + OWASP Dependency Check
mvn verify -Psecurity

# Trivy: scan de vulnerabilidades no filesystem
docker compose -f docker/docker-compose.dev.yml --profile scan up trivy

# Verificar dependências desatualizadas
mvn versions:display-dependency-updates -Pversions
```

Veja `docs/SECURITY.md` para detalhes.

---

## Configurando o Deploy Automático (GitHub Actions)

O projeto inclui um pipeline de CI/CD em `.github/workflows/deploy.yml` que:
- roda os testes automaticamente a cada `push` na branch `main`
- executa análise de segurança (SAST) no código e nas dependências
- constrói a imagem Docker de produção e faz o deploy no servidor da disciplina

Para ativar o deploy, você precisa configurar **dois secrets** e uma **variável** no seu repositório GitHub.

---

### Secret 1 — Chave SSH de deploy (`SSH_DEPLOY_KEY`)

O servidor da disciplina (`dsc.rodrigor.com`) já está preparado para receber deploys.
A chave SSH que autoriza o acesso está disponível na página da disciplina:

**Acesse: https://gd.dsc.rodrigor.com** e copie a chave SSH privada disponibilizada pelo professor.

Depois, adicione no seu repositório:

1. No GitHub, acesse seu repositório → **Settings**
2. No menu lateral: **Secrets and variables → Actions**
3. Clique em **New repository secret**
4. Nome: `SSH_DEPLOY_KEY`
5. Valor: cole a chave privada copiada do portal (o texto completo, incluindo as linhas `-----BEGIN...` e `-----END...`)
6. Clique em **Add secret**

---

### Secret 2 — Chave da API do NVD (`NVD_API_KEY`)

#### O que é o NVD?

**NVD** significa *National Vulnerability Database* — é o banco de dados oficial do governo americano (NIST) que cataloga todas as vulnerabilidades de segurança conhecidas em softwares. Cada vulnerabilidade recebe um identificador chamado **CVE** (ex.: CVE-2024-12345) e uma nota de gravidade chamada **CVSS** (de 0 a 10).

O **OWASP Dependency Check** (uma das ferramentas de segurança do projeto) consulta esse banco para verificar se as bibliotecas que o seu projeto usa possuem vulnerabilidades conhecidas.

#### Por que preciso de uma chave?

Sem a chave, o download do banco de dados NVD é muito lento (pode levar 20+ minutos no CI/CD, ou até falhar por timeout). Com a chave gratuita, o download é feito via API e leva menos de 2 minutos.

#### Como obter (gratuito, leva ~1 minuto)

1. Acesse https://nvd.nist.gov/developers/request-an-api-key
2. Preencha seu e-mail institucional (use o e-mail da UFPB se possível)
3. Marque a caixa de uso não-comercial
4. Clique em **Submit**
5. Acesse seu e-mail — você receberá a chave em segundos

#### Adicionando ao repositório

1. No GitHub: **Settings → Secrets and variables → Actions**
2. Clique em **New repository secret**
3. Nome: `NVD_API_KEY`
4. Valor: cole a chave recebida por e-mail
5. Clique em **Add secret**

> **Sem a chave ainda?** O pipeline funciona mesmo sem ela, mas o OWASP Dependency Check
> pode demorar muito ou falhar por timeout. Configure assim que possível.

---

### Variável — Nome da imagem Docker (`APP_IMAGE`)

O pipeline publica a imagem Docker no GitHub Container Registry (GHCR) com o nome do seu repositório. Você não precisa configurar isso manualmente — o workflow usa `${{ github.repository }}` para montar o nome automaticamente.

Mas o arquivo `.env` no servidor precisa saber qual imagem usar. O script de deploy atualiza isso automaticamente na primeira execução.

---

### Verificando se o deploy funcionou

Após configurar os secrets e fazer um `push` na branch `main`:

1. No GitHub, clique na aba **Actions**
2. Você verá o workflow **"Build & Deploy"** em execução
3. Ele tem 3 etapas: **Testes e SAST → Build e push → Deploy em produção**
4. Se tudo der certo, a aplicação estará disponível em `https://dsc.rodrigor.com`

Se alguma etapa falhar, clique nela para ver os logs detalhados.

---

## Estrutura do Projeto

```
base_projeto/
├── .github/workflows/
│   └── deploy.yml           # Pipeline CI/CD (GitHub Actions)
├── src/main/java/br/ufpb/dsc/mercado/
│   ├── config/              # Configurações (Security, GlobalModelAttributes, etc.)
│   ├── controller/          # Controllers HTTP + HTMX
│   ├── domain/              # Entidades JPA
│   ├── dto/                 # Data Transfer Objects (Records)
│   ├── exception/           # Exceções de domínio
│   ├── repository/          # Interfaces Spring Data JPA
│   └── service/             # Lógica de negócio
├── src/main/resources/
│   ├── db/migration/        # Scripts Flyway (V1__, V2__, ...)
│   └── templates/           # Templates Thymeleaf
├── docker/                  # Dockerfiles + docker-compose
├── docs/                    # Documentação técnica
├── CLAUDE.md                # Memória para Claude Code
└── pom.xml
```

---

## Log de auditoria

- **O que é auditado**: Operações críticas de negócio mapeadas nas classes de serviço do sistema. Os recursos e as ações atualmente auditadas são:
  - **Recurso `Corrida`**: Criação (`RACE_CREATED`), Edição (`RACE_UPDATED`) e Cancelamento (`RACE_CANCELLED`).
  - **Recurso `USER`**: Login (`LOGIN`), Edição de conta (`EDIT_USER`) e Exclusão (`DELETE_USER`).
  - **Recurso `USER_INFO`**: Registro inicial (`CREATE_USER_INFO`), Consulta de perfil (`GET_USER_INFO`), Atualização de perfil (`UPDATE_USER_INFO`) e Alteração de foto (`UPDATE_PROFILE_PHOTO`).
  - **Recurso `ORGANIZER`**: Registro de Organizador e Organização (`REGISTER_ORGANIZER`).
  - **Recurso `CONECTION`**: Envio de convite (`SEND_CONNECTION`), Aceitar convite (`ACCEPT_CONNECTION`), Recusar convite (`DECLINE_CONNECTION`) e Desfazer conexão (`REMOVE_CONNECTION`).
- **Onde fica armazenado**: MongoDB Atlas ou banco local, em uma coleção cuja propriedade dinâmica de nome é definida no `application.yml` (padrão `audit_logs`). Cada documento registrado armazena os seguintes campos (definidos em [AuditLog.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditLog.java)):
  - `id`: Identificador único do documento.
  - `action`: Código da ação de negócio executada (ex: `RACE_UPDATED`, `RACE_CANCELLED_FAILED`).
  - `operator`: Identificação do operador (e-mail do usuário autenticado).
  - `ip`: Endereço IP do cliente (respeitando o cabeçalho `X-Forwarded-For`).
  - `userAgent`: String do User-Agent do navegador ou cliente HTTP.
  - `httpMethod`: Método HTTP correspondente à requisição (ex: POST, PUT, PATCH).
  - `resource`: Nome do recurso de domínio afetado (ex: `Corrida`).
  - `targetId`: ID único do recurso modificado.
  - `stateBefore`: Dados serializados e higienizados do recurso *antes* da execução.
  - `stateAfter`: Dados serializados e higienizados do recurso *depois* da execução.
  - `errorMessage`: Detalhe da mensagem de erro capturada caso a ação falhe.
  - `timestamp`: Instante do registro do log (com TTL configurado para 90 dias).
- **Como foi implementado**: Utilizando programação orientada a aspectos (AOP) com Spring AOP para interceptar métodos marcados com a anotação customizada `@Auditable`. O aspecto captura os dados de contexto HTTP (IP com tratamento para `X-Forwarded-For`, User-Agent e método HTTP) e segurança (operador autenticado), além de mapear recursivamente os estados anterior e posterior do recurso (com exclusão automática de campos sensíveis como senhas, tokens e propriedades anotadas com `@ToString.Exclude`). A persistência é realizada por eventos do Spring (`AuditLogEvent`) desacoplados, onde operações de sucesso são gravadas após o commit da transação (`@TransactionalEventListener`) e falhas são salvas assincronamente (`@Async`).
- **Quais classes/arquivos participam**:
  - [Auditable.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/Auditable.java) (anotação customizada)
  - [AuditLog.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditLog.java) (entidade documento MongoDB com TTL index de 90 dias)
  - [AuditLogRepository.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditLogRepository.java) (repositório MongoDB)
  - [AuditContextUtils.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditContextUtils.java) (utilitário para HttpServletRequest e IP do cliente)
  - [AuditAspect.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditAspect.java) (interceptador aspect e higienizador por reflexão)
  - [AuditLogEvent.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditLogEvent.java) (evento de auditoria)
  - [AuditLogListener.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditLogListener.java) (listener transacional e assíncrono)
  - [AuditConfig.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/audit/AuditConfig.java) (habilitação do `@EnableAsync`)

---

## Integração com Serviço Externo

- **Serviço externo**: OpenRouteService (Directions e Geocoding APIs)
- **Finalidade**: Usado para geocodificação de endereços no formulário de criação/edição de corrida (autocomplete de texto para coordenadas geográficas) e para calcular rotas reais de pedestre entre o ponto de largada e o ponto de chegada. O serviço fornece a distância exata da prova, o tempo estimado de duração e o traçado completo em formato GeoJSON para renderização interativa na tela com Leaflet.js.
- **Classes/arquivos participantes**:
  - [OpenRouteServiceClient.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/race/OpenRouteServiceClient.java) (cliente HTTP com Spring RestClient e resiliência a falhas)
  - [CorridaService.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/race/CorridaService.java) (uso das rotas e regras de caching de requisições baseadas em coordenadas inalteradas)
  - [GeocodingApiController.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/race/GeocodingApiController.java) (REST API Proxy para chamadas do front-end)
  - [RotaDTO.java](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/java/br/ufpb/dsc/corrida/race/dto/RotaDTO.java) (DTO de transferência da rota calculada)
  - [application.yml](file:///c:/Users/willi/Desktop/Faculdade/DSC/projeto-eq07/src/main/resources/application.yml) (definição de chaves e URLs do serviço)