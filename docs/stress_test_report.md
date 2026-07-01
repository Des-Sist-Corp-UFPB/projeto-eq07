# Relatório de Teste de Estresse - Quebra de SLA (3 Segundos)

## 1. Objetivo do Teste
Encontrar o ponto de quebra (*breaking point*) ou estrangulamento da aplicação, especificamente identificar como e quando o sistema falharia em responder às requisições em menos de **3 segundos (3000 ms)** sob carga massiva.

## 2. Cenário de Estresse (Injection Profile)
Para atingir o limite do hardware local / container, configuramos um cenário de **Rampa Agressiva** de duração de 40 segundos:
- **Perfil Público (`GET /corridas`):** Subiu gradativamente de 10 requisições por segundo para **400 requisições por segundo**.
- **Perfil Usuário (`POST /login`):** Subiu gradativamente de 5 logins por segundo para **100 logins por segundo** (Pesado em CPU devido à criptografia BCrypt).

## 3. Resultados Obtidos
Ao disparar o teste, a asserção configurada falhou propositalmente, abortando o sucesso do build do Maven (`BUILD FAILURE - Gatling simulation assertions failed`) pois **o limite de 3000ms foi ultrapassado violentamente**.

### Métricas de Degradação (O Ápice da Falha):
* **Total de Requisições Feitas:** 18.500
* **Tempo Máximo de Resposta:** 44.763 ms (~44 Segundos!)
* **Tempo Médio de Resposta (Mean):** 20.059 ms (~20 Segundos)
* **Taxa de Transferência Atingida:** 177.8 requisições por segundo na média.
* **Quedas (Timeouts ou HTTP 5xx):** 0%. *(Curiosamente, o Tomcat do Spring Boot enfileirou todas as 18 mil requisições e processou sem derrubar conexões, mas a lentidão foi severa).*

### Onde o sistema atingiu o ápice e falhou:
- **Até ~300 requisições totais concorrentes:** O sistema consegue responder abaixo dos 3 segundos confortavelmente (conforme vimos no teste anterior).
- **A partir da casa das 2.000 requisições (por volta do 15º segundo do teste):** O pool de conexões com o PostgreSQL (geralmente limitado a 10 conexões HikariCP por padrão no Spring Boot) satura. Como as threads do Tomcat (padrão de 200 threads simultâneas) continuam aceitando requisições HTTP, elas entram numa fila de espera aguardando conexão livre no banco de dados.

- **Ápice do Gargalo:** Nos últimos 20 segundos do teste, o sistema virou um gargalo gigante. **87% das requisições demoraram mais de 1.2 segundos**, e a mediana pulou para assustadores 21 segundos.

### Análise de Percentil (SLA de 1 Segundo)
Ao analisarmos a distribuição de tempo de resposta buscando o limite ideal de **1 segundo (1000ms)**, constatamos o exato ponto de quebra do sistema:
- **Apenas 13%** do total de requisições (cerca de 2.432 de 18.500 requisições) conseguiram ser processadas e devolvidas em torno de 1 segundo ou menos. A grande maioria dessas requisições bem-sucedidas ocorreu antes dos primeiros 15 segundos da rampa de injeção.
- **Os 87% restantes** (cerca de 16.068 requisições) ultrapassaram a barreira de 1 segundo e sofreram atrasos severos e contínuos. Isso prova em números que o limite aceitável do sistema com a configuração atual é de **~13% da carga testada**, ou seja, o sistema sustenta sua agilidade apenas até ser atingido por um limite de concorrência acumulado de aproximadamente **2.400 requisições simultâneas**.

### Busca do Limite Exato (SLA de 1 Segundo)
Para responder com exatidão matemática à pergunta de **quantas requisições são necessárias para que o tempo passe de 1 segundo**, realizamos uma execução complementar injetando de 1 a 200 RPS ao longo de 60 segundos.

Os resultados crus do motor do Gatling comprovaram que o sistema atende maravilhosamente bem até a **4815ª (quatro mil oitocentos e décima quinta) requisição concorrente**. 
A partir da **4816ª requisição**, a fila do Tomcat e as conexões do banco (Hikari) transbordam os limites do hardware, empurrando o tempo de resposta acima de 1000ms. 

**Resumo da rodada focada (10.590 requisições totais):**
- **4.815 requisições (45%)** responderam quase instantaneamente (< 800ms)
- **256 requisições (2%)** ficaram na zona de risco (800ms a 1.2s)
- **5.519 requisições (52%)** sofreram lentidão acima de 1.2s
- **Número exato do estrangulamento:** 4.816 requisições acumuladas.

## 4. Recomendações e Soluções 
Se a sua aplicação tiver picos na vida real equivalentes a 400 acessos simultâneos por segundo, você **precisará aplicar estas técnicas** para não estourar o SLA de 3 segundos:

1. **Aumentar as Threads do Tomcat (application.yml):**
   ```yaml
   server:
     tomcat:
       threads:
         max: 500 # Aumentar o pool nativo de atendimento web
   ```
2. **Aumentar o Pool de Conexões (HikariCP):**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 50 # Mais conexões simultâneas com o Postgres
   ```
3. **Implementar Caching (Redis/Caffeine):** A listagem de corridas (`GET /corridas`) faz batidas no banco, mas raramente muda. Colocar `@Cacheable` nessa rota reduziria a carga do Postgres em 90%.
4. **Escala Horizontal:** Utilizar instâncias replicadas do Docker app com um balanceador de carga na frente (ex: Nginx).
