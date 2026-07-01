# Propostas de Melhoria Arquitetural para Escalabilidade

Com base nos resultados do teste de estresse, onde o gargalo principal se mostrou na camada de conexões síncronas (banco de dados e threads presas do Tomcat), elaboramos este guia com **melhorias arquiteturais modernas** que podem aumentar drasticamente a capacidade do sistema.

As melhorias estão divididas por camadas:

---

## 1. Camada de Aplicação (Spring Boot)

### 1.1 Migração para Arquitetura Reativa (Spring WebFlux)
O Spring Web MVC clássico é *bloqueante* (uma requisição = uma thread). Se o banco de dados demora, a thread fica parada aguardando.
- **A Solução:** Utilizar **Spring WebFlux (Project Reactor + Netty)** e **R2DBC** (driver reativo de banco de dados). Isso permite que uma única thread processe centenas de requisições concorrentes, resolvendo quase que por completo o problema do esgotamento de threads que presenciamos no teste.

### 1.2 Processamento Assíncrono e Mensageria (Kafka / RabbitMQ)
Evite que a API espere por tarefas pesadas (ex: enviar um e-mail confirmando o cadastro na corrida, ou processar pagamentos).
- **A Solução:** Utilizar um *Message Broker* (como RabbitMQ, Apache Kafka ou AWS SQS). O endpoint apenas recebe a requisição, coloca um evento na fila e devolve um `202 Accepted` quase imediatamente.

### 1.3 Virtual Threads (Project Loom - Java 21)
Como o projeto já utiliza **Java 21**, você pode ativar o poder das *Virtual Threads*.
- **Como aplicar:** Basta adicionar `spring.threads.virtual.enabled=true` no seu `application.yml`. 
- **O impacto:** O Tomcat usará threads leves e gerenciadas pela JVM, permitindo criar milhões delas sem consumir a RAM do SO. Isso praticamente elimina o gargalo de concorrência do Tomcat!

---

## 2. Camada de Dados (PostgreSQL)

### 2.1 Separação de Leitura e Escrita (CQRS e Read Replicas)
90% do tráfego do sistema costuma ser para listar corridas (Leitura) e 10% para criação (Escrita).
- **A Solução:** Subir uma réplica *Read-Only* do PostgreSQL. Configurar o Spring Boot com _Routing DataSource_, onde requisições `GET` batem no banco de leitura e `POST/PUT/DELETE` batem no banco principal.

### 2.2 Pool de Conexões Externo (PgBouncer)
Aumentar o HikariCP no Java resolve até certo ponto, mas milhares de pods do Docker gerariam milhares de conexões no Postgres, derrubando o banco.
- **A Solução:** Colocar o **PgBouncer** na frente do PostgreSQL. Ele empilha conexões no banco de forma ultraleve, permitindo que o Java abra 10.000 conexões com o PgBouncer, mas o PgBouncer repasse apenas 50 conexões ordenadas para o Banco de Dados.

---

## 3. Camada de Caching e Rede

### 3.1 Cache Distribuído (Redis)
Os acessos da rota `/corridas` não precisam ir no banco de dados todas as vezes.
- **A Solução:** Integrar o **Redis** via `@EnableCaching` do Spring Boot. A API devolverá o JSON da lista de corridas diretamente da memória RAM, caindo o tempo de resposta de 20ms para **menos de 1ms**.

### 3.2 Content Delivery Network (CDN)
Como o projeto utiliza Thymeleaf (Front-end embutido com CSS/JS local).
- **A Solução:** Usar a **Cloudflare** ou Amazon CloudFront. Eles armazenam suas imagens, CSS e arquivos em servidores globais perto do usuário. O Spring Boot nem ficará sabendo que o usuário baixou a imagem, reduzindo o tráfego de rede da sua hospedagem e CPU.

### 3.3 API Gateway e Rate Limiting
No teste de estresse, não houve barreira que impedisse o "bombardeio" da API (que simulou um ataque DDoS).
- **A Solução:** Usar o **Spring Cloud Gateway** ou um WAF na frente (como o Kong API Gateway). Você configura regras como: *"Cada usuário (IP ou Token JWT) só pode fazer 10 requisições por segundo"*. Qualquer coisa acima disso recebe um HTTP `429 Too Many Requests`, protegendo a vida do servidor.

---

> [!TIP]
> **Comece pelo mais simples (Low Hanging Fruits):** Ativar as *Virtual Threads (Java 21)* no `application.yml` e implementar um *Redis* para listagens públicas costumam ser as soluções de menor esforço e maior impacto imediato!
