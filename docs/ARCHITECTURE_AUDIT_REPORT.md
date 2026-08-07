# Relatório de Auditoria Arquitetural — Projeto Corridas DSC/UFPB

## 1. Resumo Executivo e Visão Geral da Stack Técnica

### Objetivo de alto nível
A base de código implementa uma plataforma completa de gerenciamento de corridas para organizadores e atletas. Ela suporta cadastro e autenticação de usuários, onboarding de organizadores, publicação e gestão de corridas, inscrições de atletas, entradas pagas via Mercado Pago, gerenciamento de perfil com armazenamento de fotos, conexões sociais entre usuários, experimentação com feature flags, painéis administrativos, auditoria e análise de elegibilidade para participação em corridas.

A aplicação é implementada como um monólito em Spring Boot com páginas HTML renderizadas no servidor usando Thymeleaf e HTMX, além de um conjunto de endpoints REST para APIs utilizadas pela interface e por integrações externas.

### Stack técnica ( Peterson )

#### Linguagens e runtime
- Java 21
- HTML, CSS e JavaScript (templates de frontend)

#### Frameworks e bibliotecas do backend
- Spring Boot 3.4.5
- Spring MVC
- Spring Security 6
- Spring Data JPA
- Spring AOP
- Spring Validation
- Spring Cache / Caffeine
- Spring Mail
- Spring Actuator
- Annotations de instrumentação com OpenTelemetry

#### Stack de frontend / interface
- Templates Thymeleaf
- HTMX para atualizações parciais de página
- Bootstrap via WebJars
- Tailwind é mencionado na documentação do projeto e provavelmente é usado em templates, embora os templates executados analisados sejam principalmente orientados a Bootstrap/HTMX.

#### Dados e persistência
- PostgreSQL 16
- Flyway para migrações de schema
- JPA/Hibernate
- MinIO para armazenamento de objetos (fotos de perfil)

#### Integrações e serviços externos
- OpenRouteService (roteamento e geocodificação)
- SDK do Mercado Pago para pagamentos via Pix
- OpenPDF para geração de comprovantes em PDF
- Java JWT para emissão de tokens
- Lombok para redução de boilerplate

#### Testes e qualidade
- JUnit 5
- Testcontainers (PostgreSQL e MinIO)
- JaCoCo
- PMD / SpotBugs / FindSecBugs / OWASP Dependency Check

#### DevOps / hospedagem
- Docker e Docker Compose
- Workflows de CI/CD no GitHub Actions
- Observabilidade via OpenTelemetry e exportação compatível com Grafana/LGTM

---

## 2. Conceitos e Padrões Arquiteturais

### Arquitetura geral
O projeto segue uma arquitetura monolítica em camadas com Spring Boot, com separação clara entre:
- Controllers: pontos de entrada HTTP para páginas MVC e APIs REST
- Services: lógica de negócio principal e orquestração
- Repositories: abstrações de persistência sobre entidades JPA
- Entities: modelos de domínio JPA
- DTOs: contratos de entrada e saída de dados
- Classes de configuração: segurança, armazenamento, serviços externos e observabilidade

Isso é melhor descrito como um monólito modular usando MVC e decomposição orientada ao domínio, e não como uma arquitetura de microsserviços.

### Fluxo de requisição
Um fluxo típico é:
1. Uma requisição HTTP chega a um controller como [src/main/java/br/ufpb/dsc/corrida/race/CorridaViewController.java](src/main/java/br/ufpb/dsc/corrida/race/CorridaViewController.java) ou [src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoController.java](src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoController.java).
2. Os filtros do Spring Security autenticam e autorizam a requisição com base na configuração em [src/main/java/br/ufpb/dsc/corrida/config/security/SecurityConfig.java](src/main/java/br/ufpb/dsc/corrida/config/security/SecurityConfig.java).
3. O controller delega para uma classe de service, por exemplo [src/main/java/br/ufpb/dsc/corrida/race/CorridaService.java](src/main/java/br/ufpb/dsc/corrida/race/CorridaService.java) ou [src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoService.java](src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoService.java).
4. O service executa as regras de domínio e chama os repositories para ler ou persistir entidades.
5. A camada de repository interage com o PostgreSQL via Hibernate/JPA.
6. Em caso de sucesso, o controller retorna uma view Thymeleaf ou uma resposta JSON/HTTP.

### Padrões de design principais
- Padrão Repository: repositories Spring Data JPA como [src/main/java/br/ufpb/dsc/corrida/race/RaceRepository.java](src/main/java/br/ufpb/dsc/corrida/race/RaceRepository.java) encapsulam a lógica de persistência.
- Injeção de dependência: beans gerenciados pelo Spring são conectados por injeção via construtor ou campo.
- AOP: usado para auditoria e aplicação de feature flags, via [src/main/java/br/ufpb/dsc/corrida/audit/AuditAspect.java](src/main/java/br/ufpb/dsc/corrida/audit/AuditAspect.java) e [src/main/java/br/ufpb/dsc/corrida/featuretoggle/FeatureToggleAspect.java](src/main/java/br/ufpb/dsc/corrida/featuretoggle/FeatureToggleAspect.java).
- Padrão de boundary transacional: métodos de service usam @Transactional para definir fluxos de negócio atômicos.
- Estilo Strategy / facade: [src/main/java/br/ufpb/dsc/corrida/featuretoggle/FeatureToggleService.java](src/main/java/br/ufpb/dsc/corrida/featuretoggle/FeatureToggleService.java) abstrai o acesso a feature flags.
- Integração orientada a eventos: [src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoService.java](src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoService.java) publica um evento de conclusão de corrida e o webhook de pagamento atualiza o estado da inscrição de forma assíncrona via envio de e-mail.

### Observações arquiteturais
A base de código tem uma forte sensação de vertical slice dentro de um monólito modular, mas os módulos de domínio ainda não estão totalmente isolados por bounded context. Há alguma duplicação e acoplamento cruzado, especialmente entre as relações de usuário e organizador. O desenho é educativo e rico em funcionalidades, mas beneficiaria de fronteiras de módulo mais rigorosas e de services de domínio mais explícitos se evoluísse para um sistema maior em produção.

---

## 3. Mapeamento de Telas e Interface de Usuário (UI)

### Página inicial pública ( Igino )
- Rota: /
- Implementação: [src/main/java/br/ufpb/dsc/corrida/home/HomeController.java](src/main/java/br/ufpb/dsc/corrida/home/HomeController.java)
- Objetivo: exibir próximas corridas e usuários cadastrados recentemente.
- Elementos de UI: cards de corridas, cards de usuários e navegação.
- Lógica de estado/dados: carrega próximas corridas via CorridaService e usuários recentes via UsuarioService.

### Telas de autenticação ( Peterson )
- Rotas: /login, /registrar, /registrar/organizador
- Implementações: [src/main/java/br/ufpb/dsc/corrida/user/UsuarioViewController.java](src/main/java/br/ufpb/dsc/corrida/user/UsuarioViewController.java) e [src/main/java/br/ufpb/dsc/corrida/organizer/OrganizerViewController.java](src/main/java/br/ufpb/dsc/corrida/organizer/OrganizerViewController.java)
- Objetivo: cadastrar usuários comuns, fazer login e se registrar como organizador.
- Elementos de UI: formulários, mensagens de validação, banners de sucesso/erro.
- Lógica de estado/dados: binding de formulários com DTOs e redirecionamento após sucesso ou falha.

### Páginas de perfil e conta do usuário ( Igino )
- Rotas: /minhaConta, /user/{username}/profile
- Implementações: [src/main/java/br/ufpb/dsc/corrida/user/UsuarioViewController.java](src/main/java/br/ufpb/dsc/corrida/user/UsuarioViewController.java)
- Objetivo: gerenciamento de conta, visualização de perfil público e contexto de conexões.
- Elementos de UI: resumo do perfil, foto, estatísticas e ações de conexão.
- Lógica de estado/dados: carrega UserInfo e status de conexão a partir de repositories e services.
- OBS: Estava nos planos adicionar as atividades do usuário ao seu perfil, como um feed de postagens.

### Páginas de listagem e detalhes de corrida ( Peterson )
- Rotas: /corridas, /corridas/encerradas, /corridas/{slug}
- Implementações: [src/main/java/br/ufpb/dsc/corrida/race/CorridaViewController.java](src/main/java/br/ufpb/dsc/corrida/race/CorridaViewController.java)
- Objetivo: navegar pelas corridas públicas e consultar informações detalhadas.
- Elementos de UI: cards de corrida, detalhes, botões de chamada para inscrição, status de pagamento e aviso ao organizador.
- Lógica de estado/dados: calcula status de inscrição, pendência de pagamento, expiração e capacidade.

### Telas de gerenciamento do organizador ( Igino )
- Rotas: /organizacao/{orgId}/corridas, /organizacao/{orgId}/corridas/nova, /organizacao/{orgId}/corridas/{id}/editar
- Implementações: [src/main/java/br/ufpb/dsc/corrida/race/CorridaViewController.java](src/main/java/br/ufpb/dsc/corrida/race/CorridaViewController.java)
- Objetivo: gerenciar o ciclo de vida das corridas de uma organização.
- Elementos de UI: lista de corridas, formulários e ações CRUD.
- Lógica de estado/dados: validação de propriedade e preenchimento de formulário a partir das entidades de domínio.

### Telas de inscrição e pagamento ( Peterson )
- Rotas: /inscricoes/{id}/pagamento, /inscricoes/{id}/comprovante, /minhas-inscricoes
- Implementações: [src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoController.java](src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoController.java) e [src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoViewController.java](src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoViewController.java)
- Objetivo: tratar fluxos de inscrição, status de pagamento e comprovantes.
- Elementos de UI: página/QR de pagamento, polling de status e tela de comprovante.
- Lógica de estado/dados: verifica o status atual do pagamento, suporta renovação e expõe um endpoint JSON de status.

### Telas administrativas ( Igino )
- Rotas: /admin, /admin/audit, /admin/users, /admin/features
- Implementações: [src/main/java/br/ufpb/dsc/corrida/admin/AdminController.java](src/main/java/br/ufpb/dsc/corrida/admin/AdminController.java)
- Objetivo: administrar logs de auditoria, usuários e feature flags.
- Elementos de UI: dashboards e tabelas paginadas.
- Lógica de estado/dados: listagem e paginação com base em repositories.

### Observações arquiteturais
A interface é renderizada no servidor e é relativamente coesa, mas o gerenciamento de estado no frontend é mínimo; a maior parte do estado é derivada no servidor a partir da requisição atual e dos dados dos repositories. HTMX é usado para interação dinâmica, mas o código não mostra uma camada dedicada de estado para SPA.

---

## 4. Recursos e Capacidades Abrangentes

### Cadastro e autenticação de usuários
- Implementado por [src/main/java/br/ufpb/dsc/corrida/user/UsuarioController.java](src/main/java/br/ufpb/dsc/corrida/user/UsuarioController.java), [src/main/java/br/ufpb/dsc/corrida/user/UsuarioService.java](src/main/java/br/ufpb/dsc/corrida/user/UsuarioService.java) e [src/main/java/br/ufpb/dsc/corrida/user/AuthService.java](src/main/java/br/ufpb/dsc/corrida/user/AuthService.java).
- Suporta cadastro, login, edição, exclusão e busca de perfil público.
- As senhas são hashadas com BCrypt em [src/main/java/br/ufpb/dsc/corrida/user/User.java](src/main/java/br/ufpb/dsc/corrida/user/User.java).

### Onboarding de organizadores
- Implementado por [src/main/java/br/ufpb/dsc/corrida/organizer/OrganizerService.java](src/main/java/br/ufpb/dsc/corrida/organizer/OrganizerService.java) e [src/main/java/br/ufpb/dsc/corrida/organizer/OrganizerViewController.java](src/main/java/br/ufpb/dsc/corrida/organizer/OrganizerViewController.java).
- Cria tanto uma conta de usuário quanto um registro de organizador/organização em uma única transação.

### Catálogo e gestão do ciclo de vida das corridas
- Implementado por [src/main/java/br/ufpb/dsc/corrida/race/CorridaService.java](src/main/java/br/ufpb/dsc/corrida/race/CorridaService.java) e [src/main/java/br/ufpb/dsc/corrida/race/CorridaManagementController.java](src/main/java/br/ufpb/dsc/corrida/race/CorridaManagementController.java).
- Suporta operações de criação, edição, publicação, cancelamento, listagem e visualização.
- Usa OpenRouteService para calcular geometria de rota, distância e duração.

### Fluxo de inscrição e pagamento
- Implementado por [src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoService.java](src/main/java/br/ufpb/dsc/corrida/inscricao/InscricaoService.java), [src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoService.java](src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoService.java) e [src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoWebhookController.java](src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoWebhookController.java).
- Permite inscrições em corridas pagas e gratuitas, criação de cobrança Pix, atualização de status via webhook e envio de comprovante.

### Dados de perfil e armazenamento de fotos
- Implementado por [src/main/java/br/ufpb/dsc/corrida/user/UserInfoService.java](src/main/java/br/ufpb/dsc/corrida/user/UserInfoService.java) e [src/main/java/br/ufpb/dsc/corrida/storage/MinioServiceImpl.java](src/main/java/br/ufpb/dsc/corrida/storage/MinioServiceImpl.java).
- Armazena fotos de perfil no MinIO e expõe URLs assinadas.

### Conexões sociais
- Implementado por [src/main/java/br/ufpb/dsc/corrida/userConections/UserConnectionService.java](src/main/java/br/ufpb/dsc/corrida/userConections/UserConnectionService.java).
- Suporta envio, aceitação, recusa e remoção de solicitações de conexão.

### Feature flags e controles administrativos
- Implementado por [src/main/java/br/ufpb/dsc/corrida/featuretoggle/FeatureToggleAspect.java](src/main/java/br/ufpb/dsc/corrida/featuretoggle/FeatureToggleAspect.java), [src/main/java/br/ufpb/dsc/corrida/featuretoggle/DatabaseFeatureToggleProvider.java](src/main/java/br/ufpb/dsc/corrida/featuretoggle/DatabaseFeatureToggleProvider.java) e [src/main/java/br/ufpb/dsc/corrida/admin/AdminApiController.java](src/main/java/br/ufpb/dsc/corrida/admin/AdminApiController.java).
- Permite alternar em tempo de execução funcionalidades como pagamentos e busca de corridas.

### Avaliação de elegibilidade
- Implementado por [src/main/java/br/ufpb/dsc/corrida/eligibility/EligibilityService.java](src/main/java/br/ufpb/dsc/corrida/eligibility/EligibilityService.java) e [src/main/java/br/ufpb/dsc/corrida/eligibility/EligibilityApiController.java](src/main/java/br/ufpb/dsc/corrida/eligibility/EligibilityApiController.java).
- Usa dados do perfil de saúde do usuário e um cliente LLM para fornecer uma avaliação de risco; por padrão, em caso de erro ou ausência de consentimento, retorna com segurança um resultado “apto”.

### Auditoria de logs
- Implementado por [src/main/java/br/ufpb/dsc/corrida/audit/AuditAspect.java](src/main/java/br/ufpb/dsc/corrida/audit/AuditAspect.java) e [src/main/java/br/ufpb/dsc/corrida/audit/AuditLogService.java](src/main/java/br/ufpb/dsc/corrida/audit/AuditLogService.java).
- Captura contexto da requisição, ator, recurso, estado antes/depois e erros.

### Observações arquiteturais
O sistema é rico em capacidades para um projeto acadêmico, mas alguns recursos parecem parcialmente implementados ou experimentais, como o endpoint de revogar feature flag pelo admin e o branching em tempo de execução baseado em feature flag. Isso sugere uma arquitetura de prova de conceito em vez de uma implementação totalmente robusta para produção.

---

## 5. Como funciona cada funcionalidade

### Login de usuários ( Peterson )
- Usuário envia seu login e senha que são validados pelo spring security, após isso é gerado um json web token que será armazenado para ser utilizado nas requisições posteriores de forma que o spring security valide aquela autenticação.
- No cadastro de usuários tems os campos de nome, username (que é único para cada usuário, seria como o username do instagram), email, que será usado na autenticação e senha.
- Eu posso me cadastrar também como organizador, exigindo que eu informe mais dados pessoais, como CPF, CREF, também preciso preencher os dados da organização, quando eu me cadastro como organizador, preciso ter uma organização associada a mim.

### Cadastro das informações do usuário ( Igino )
- Na aba de meu perfil, posso editar meus dados, como peso, altura, nível de condicionamento, CPF, e também um termo de concentimento para o usu da IA na sua avaliação de riscos, caso fique desmarcado, é bloqueado o uso da LLm para avaliação, caso eu ative, em cada corrida, antes da inscrição, a Ia faz uma avaliação de riscos em relação a aquela corrida.
- É possível alterar a foto de perfil, a imagem será salva no minIO e a sua chave associada ao usuário no banco de dados.

### Regras de corrida ( Peterson )
- Uma corrida só pode ser criada por um usuário organizador.
- Corridas pagas são bloqueadas a menos que a feature flag PAYMENT_V2 esteja habilitada.
- Uma corrida só pode ser editada se faltar mais de 24 horas para o início.
- Corridas canceladas são tratadas como inexistentes para descoberta pública.
- Os slugs são gerados de forma URL-safe e única.
- É usado o serviço do OpenRouteService para pegar geolocalização do ponto de partida e ponto de chegada que é retornado em formato JSON pela API, e exibindo na tela em forma de mapa. a partir desses dados capturados é possível determinar a distência de forma automática.
- Na hora do criação também existem algumas informações que podem ser adicionadas em relação a clima, elevação, tipo de trajeto, para aujdar na avaliação da IA em relação aos riscos.
- Posso definir um custo para aquela corrida, caso eu ão defina, ela será gratuita.
- Pode adicionar items ou comodidades que será disponibilizada na corrida.

### Regras de inscrição ( Igino )
- Um usuário não pode se inscrever duas vezes na mesma corrida enquanto o status estiver CONFIRMADA, ATIVA ou AGUARDANDO_PAGAMENTO.
- Se a corrida for paga, o CPF precisa estar presente antes de seguir com a inscrição.
- A capacidade é verificada contando inscrições com status AGUARDANDO_PAGAMENTO, ATIVA ou CONFIRMADA.
- Conflitos com outras corridas ativas ou corridas organizadas pelo usuário são bloqueados por sobreposição de horário.
- Corridas gratuitas são confirmadas imediatamente; corridas pagas criam uma cobrança Pix a menos que o pagamento V2 esteja desativado.

### Regras de pagamento ( Peterson )
- A expiração do pagamento é definida em 30 minutos.
- Atualizações de pagamento via webhook reconciliam com o Mercado Pago e confirmam ou cancelam a inscrição.
- Os webhooks validam uma assinatura HMAC-SHA256 antes de aceitar qualquer transição de estado.
- Se a API de pagamento falhar na inicialização, a inscrição é cancelada para evitar um estado inconsistente.
- Quando um pagamento é confirmado, é disparado um email para a caixa de entrada do usuário através do SMTP

### Regras de perfil e saúde ( Igino )
- Peso e altura precisam ter valores positivos.
- A validação de CPF usa o algoritmo de dígitos verificadores brasileiro.
- As verificações de elegibilidade relacionadas a saúde exigem consentimento explícito do usuário.
- As notas médicas são sanitizadas antes de serem enviadas a um LLM para reduzir risco de prompt injection.
- A elegibilidade usa um rate limiter de 5 verificações por usuário por minuto.

### Medidas de segurança implementadas ( Peterson )
- Spring Security com autenticação JWT e controle de acesso por roles.
- Validação de assinatura de webhook para o Mercado Pago.
- Registro de auditoria de operações sensíveis.
- Sanitização de notas médicas antes do envio a um LLM externo.

### Observalidade ( Igino )
- Mostra o Grafana, Umami entre outros...

### Observações arquiteturais
As regras de negócio estão majoritariamente embutidas em services em vez de um modelo de domínio mais rico. Isso é aceitável para o escopo atual, mas torna as regras mais difíceis de raciocinar e reutilizar entre módulos. O código também usa uma mistura de exceções verificadas e não verificadas, o que pode levar a inconsistências de tratamento de erro.

---

## 6. Serviços Externos e Integrações de Terceiros

### OpenRouteService
- Papel: cálculo de rota e geocodificação para pontos de largada/chegada das corridas
- Ponto de disparo: criação/edição de corrida e UI de busca de endereço
- Implementação: [src/main/java/br/ufpb/dsc/corrida/ors/OpenRouteServiceClient.java](src/main/java/br/ufpb/dsc/corrida/ors/OpenRouteServiceClient.java)

### Mercado Pago
- Papel: criação de cobrança Pix e confirmação via webhook
- Ponto de disparo: fluxo de inscrição e processamento do webhook de pagamento
- Implementação: [src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoService.java](src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoService.java), [src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoWebhookController.java](src/main/java/br/ufpb/dsc/corrida/pagamento/MercadoPagoWebhookController.java)

### MinIO
- Papel: armazenamento persistente de objetos para imagens de perfil e outros ativos binários
- Ponto de disparo: upload de foto de perfil do usuário
- Implementação: [src/main/java/br/ufpb/dsc/corrida/storage/MinioServiceImpl.java](src/main/java/br/ufpb/dsc/corrida/storage/MinioServiceImpl.java)

### SMTP / e-mail
- Papel: entrega assíncrona de e-mails de confirmação de pagamento com anexos PDF
- Ponto de disparo: webhook de confirmação de pagamento
- Implementação: [src/main/java/br/ufpb/dsc/corrida/race/EmailService.java](src/main/java/br/ufpb/dsc/corrida/race/EmailService.java)

### LLM / LiteLLM
- Papel: avaliação de risco de elegibilidade baseada nas informações de saúde do usuário e nas características da corrida
- Ponto de disparo: endpoint e service de elegibilidade
- Implementação: [src/main/java/br/ufpb/dsc/corrida/eligibility/EligibilityService.java](src/main/java/br/ufpb/dsc/corrida/eligibility/EligibilityService.java)

### Observações arquiteturais
O sistema está bem integrado com serviços externos, mas a estratégia de integração é majoritariamente direta, com SDKs e chamadas REST feitas diretamente das classes de service. Em um sistema de produção, esses pontos provavelmente seriam encapsulados por adapters mais explícitos e com comportamento de circuit breaker.

---

## 7. Banco de Dados e Modelos de Dados

### Entidades principais
- User: representa os usuários da aplicação e implementa Spring Security UserDetails
- UserInfo: informações de perfil em relação 1:1 com um usuário
- Race: entidade principal de corrida, com rota, tempo, status e relação com organização
- Organization: container de organização pertencente ao organizador
- Organizer: perfil de organizador vinculado a um usuário e a uma organização
- Inscricao: junção do tipo muitos-para-muitos entre usuários e corridas, com estado adicional de ciclo de vida
- Pagamento: registro de pagamento vinculado a uma inscrição
- UserConnection: solicitação/relação social de conexão entre usuários
- FeatureFlag / UserFeatureFlag: toggles de funcionalidades em runtime
- AuditLog: trilha de auditoria em forma de documento/entidade para eventos de auditoria

### Relacionamentos
- User 1:1 UserInfo
- Organization 1:1 Organizer (ou 1:n dependendo dos detalhes de implementação)
- Organization 1:n Race
- User muitos-para-muitos Race através de Inscricao
- Inscricao 1:1 Pagamento
- User 1:n UserConnection como requester/receiver

### Convenções de schema observadas
- PostgreSQL é o armazenamento transacional principal
- Flyway gerencia a evolução do banco de dados
- Entidades JPA usam timestamps do Hibernate e mapeamento de enums
- Alguns dados são armazenados em estruturas tipo String/JSON para geometria de rota

### Observações arquiteturais
O modelo de domínio é expressivo e cobre bem o domínio principal do negócio. A principal fraqueza é que o modelo de persistência não está totalmente normalizado em torno de uma linguagem de domínio mais rica; alguns conceitos ainda são representados por regras de service e checks ad-hoc.
