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
## 🎥 Vídeo de Demonstração do Sistema

Para uma visão detalhada do funcionamento do **RunManager** (Sistema de Gerenciamento de Corridas), gravamos um vídeo apresentando todos os módulos do projeto em execução. 

No vídeo, demonstramos os papéis de Atleta, Organizador e Administrador, além das integrações com serviços corporativos (MinIO, OpenRouteService, Mercado Pago, IA de análise de risco e monitoramento via Grafana).

**[Apresentação Sistema de Gerenciamento de Corridas](https://youtu.be/9FeBUtBQt6w?si=l49irj3uyfrL2In3)**

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

---

## Observabilidade / Variáveis de Ambiente (OpenTelemetry & LGTM)

A aplicação conta com observabilidade unificada baseada no padrão **OpenTelemetry (OTel)**, exportando os 3 sinais de telemetria (**Traces**, **Métricas** e **Logs**) via protocolo **OTLP** para o backend central Grafana (LGTM / Prometheus + Tempo + Loki).

### Variáveis de Ambiente OpenTelemetry

| Variável de Ambiente | Obrigatória | Propósito | Motivo / Utilidade | Onde obter o valor | Exemplo / Formato |
|---|---|---|---|---|---|
| `OTEL_SERVICE_NAME` | **Sim** | Identificador único da aplicação / equipe | Usado para filtrar e correlacionar Traces, Métricas e Logs no Grafana centralizado da turma | Padrão definido pela disciplina (`dsc-eqNN` ou `aps-eqNN`) | `OTEL_SERVICE_NAME=dsc-eq07` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | **Sim** | Endpoint de ingestão do coletor OTLP | Define para onde o Java Agent envia os dados via HTTP/protobuf | Servidor central da turma ou container local LGTM | `OTEL_EXPORTER_OTLP_ENDPOINT=https://` (prod) ou `http://localhost:4318` (local) |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | **Sim** | Protocolo de transporte OTLP | Define a serialização no canal de envio (ex: HTTP protobuf) | Padrão OpenTelemetry OTLP | `OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf` |
| `OTEL_EXPORTER_OTLP_HEADERS` | Não / Prod | Cabeçalho HTTP com Token Bearer | Autenticação no coletor OTLP centralizado da disciplina (**NUNCA comitar!**) | Canal oficial da disciplina no Discord | `OTEL_EXPORTER_OTLP_HEADERS=Authorization=Bearer SEU_TOKEN_AQUI` |
| `OTEL_TRACES_EXPORTER` | **Sim** | Exportador de Tracing | Ativa o envio de rastros de requisições e queries SQL para o Tempo | Padrão OpenTelemetry | `OTEL_TRACES_EXPORTER=otlp` |
| `OTEL_METRICS_EXPORTER` | **Sim** | Exportador de Métricas | Ativa o envio de métricas de JVM (heap, GC, threads), HTTP e métricas customizadas para o Prometheus | Padrão OpenTelemetry | `OTEL_METRICS_EXPORTER=otlp` |
| `OTEL_LOGS_EXPORTER` | **Sim** | Exportador de Logs | Intercepta logs do Logback e os envia ao Loki correlacionados com `trace_id` e `span_id` | Padrão OpenTelemetry | `OTEL_LOGS_EXPORTER=otlp` |

### Como Executar com Observabilidade

#### Execução Local (com Java Agent anexado)
```bash
# Definir variáveis de ambiente
export OTEL_SERVICE_NAME=dsc-eq07
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=otlp
export OTEL_LOGS_EXPORTER=otlp

# Rodar a aplicação com o agente OpenTelemetry
java -javaagent:opentelemetry-javaagent.jar -jar target/mercado-0.0.1-SNAPSHOT.jar
```

#### Execução via Docker Compose
```bash
docker compose -f docker/docker-compose.dev.yml up --build
```

### Como Validar a Telemetria no Grafana

1. **Métricas (Prometheus)**:
   - Acesse o Grafana → menu **Dashboards**.
   - Filtre por `service.name = dsc-eq07`.
   - Visualize os gráficos de uso de memória JVM, atividade de threads, throughput de requisições HTTP e latência.

2. **Logs Correlacionados (Loki)**:
   - Acesse o Grafana → menu **Explore** → selecione a fonte de dados **Loki**.
   - Execute a consulta LogQL: `{service_name="dsc-eq07"}`.
   - Verifique se os registros de log exibem os atributos do MDC (`requestId`, `userId`, `clientIp`) e o `trace_id`.
   - Ao clicar em uma linha de log com `trace_id`, clique no botão para navegar diretamente para o rastro correspondente no Tempo.

3. **Traces Distribuidos (Tempo)**:
   - Acesse o Grafana → menu **Explore** → selecione a fonte de dados **Tempo**.
   - Filtre por `Service Name = dsc-eq07`.
   - Clique em um trace para inspecionar a cascata completa de execução (requisição HTTP → controller → queries SQL / serviços externos).

### Instrumentação Manual (@WithSpan)

Para atender aos requisitos de telemetria refinada e fornecer visibilidade sobre passos críticos de negócio, métodos do domínio foram anotados com `@WithSpan` e `@SpanAttribute` da biblioteca OpenTelemetry Instrumentation.

| Classe | Método | Nome do Span (`@WithSpan`) | Atributos Customizados (`@SpanAttribute`) | Motivo da Escolha |
|---|---|---|---|---|
| `EligibilityService` | `check(userId, raceId)` | `eligibility.check-risk` | `user.id`, `race.id` | Medir a latência do pipeline de elegibilidade do atleta (consulta de consentimento, rate-limit, cache e chamada à LLM). |
| `InscricaoService` | `inscrever(user, raceId, riskAcknowledged)` | `race.inscrever-atleta` | `race.id`, `risk.acknowledged` | Isolar a duração da validação de regras de negócio de inscrição (duplicidade, limite de vagas, conflitos de horário). |
| `CorridaService` | `criarCorrida(dto, organizationId, userDetails)` | `race.criar-corrida` | `organization.id` | Monitorar o tempo gasto na validação de permissões de organização e na criação da corrida. |


## Integração com Mercado Pago

- **Serviço externo**: Mercado Pago (Payments API)
- **Finalidade**: Processamento de pagamentos via Pix para confirmação de inscrições em corridas. Após a geração da cobrança, o sistema recebe notificações assíncronas (webhooks) do Mercado Pago informando mudanças de status do pagamento (aprovado, rejeitado, cancelado), atualizando automaticamente o status da `Inscricao` correspondente sem necessidade de polling.
- **Fluxo de segurança do webhook**:
  1. **Validação de assinatura HMAC-SHA256** — todo webhook recebido é validado contra o cabeçalho `x-signature`, usando o template `id:{data.id};request-id:{x-request-id};ts:{timestamp};` assinado com o secret configurado.
  2. **Idempotência** — pagamentos já marcados como `APROVADO` no banco não são reprocessados, evitando duplicidade em caso de reenvio da notificação pelo Mercado Pago.
  3. **Double-check** — antes de atualizar o estado local, o status é reconsultado diretamente na API do Mercado Pago (nunca confia-se apenas no conteúdo do payload do webhook).
- **Classes/arquivos participantes**:
  - `MercadoPagoWebhookController.java` (endpoint `POST /api/v1/webhooks/mercadopago`, validação de assinatura e roteamento de status)
  - `MercadoPagoService.java` (cliente da API do Mercado Pago — criação de cobrança Pix e consulta de status autoritativo)
  - `Pagamento.java` (entidade JPA com `mpPaymentId`, vínculo `@ManyToOne`/`@OneToOne` com `Inscricao`)
  - `PagamentoRepository.java` (busca por `mpPaymentId`)
  - `EmailService.java` (disparo assíncrono de comprovante ao atleta após aprovação)
  - `application.yml` (definição de `mercadopago.webhook-secret` e demais chaves)

### Variáveis de Ambiente — Mercado Pago

| Variável | Obrigatória | Propósito |
|---|---|---|
| `MP_ACCESS_TOKEN` | Sim | Token de acesso à API do Mercado Pago (criação de cobranças Pix e consulta de status) |
| `MP_WEBHOOK_SECRET` | Sim | Secret usado para validar a assinatura HMAC-SHA256 dos webhooks recebidos (`mercadopago.webhook-secret`) |

> **Importante**: sem `MERCADOPAGO_WEBHOOK_SECRET` configurado, a validação de assinatura é **ignorada** (apenas um warning é logado)

### Testando webhooks localmente

Como o Mercado Pago precisa de uma URL pública para enviar notificações, use um túnel (ex.: `ngrok http 8080`) e cadastre a URL gerada (`https://SEU-DOMINIO.ngrok-free.app/api/v1/webhooks/mercadopago`) no painel de desenvolvedor do Mercado Pago. Use o inspector do ngrok (`http://127.0.0.1:4040`) para depurar requisições recebidas durante os testes.
