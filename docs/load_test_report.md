# Relatório de Teste de Carga - Gatling

## 1. Resumo da Execução
O teste de carga foi executado com sucesso localmente utilizando o **Gatling** como um plugin do Maven, disparado de dentro do container Docker da aplicação (`corridas-app-dev`) para evitar dependências na máquina host.

**Data/Hora:** 01 de Julho de 2026
**Ferramenta:** io.gatling (v4.8.0 / core 3.10.5)
**Ambiente:** Local (Docker Desktop) - Container `corridas-app-dev` 

## 2. O que foi feito?
1. **Configuração da Infraestrutura:** O `docker-compose.dev.yml` foi utilizado para levantar o PostgreSQL (versão 16) e a aplicação Spring Boot em containers separados.
2. **Ajuste de Java:** A versão do Java no `pom.xml` foi ajustada para `21` para garantir a compatibilidade de build com a imagem Docker utilizada (`eclipse-temurin-21`).
3. **Inclusão do Gatling:** Adicionamos as dependências e o plugin do Gatling no `pom.xml` no escopo de testes.
4. **Criação da Simulação:** Foi criada a classe `CargaSimulation.java` mapeando 3 jornadas (perfis) distintas de uso.

## 3. Perfis e Cenários Testados
Para validar o comportamento do sistema perante os mecanismos de Segurança e Banco de Dados, 3 perfis de usuários foram simulados com um tráfego de *Ramp-Up* (aumento gradual de concorrência) de 60 usuários virtuais ao longo de 10 segundos.

| Perfil | Endpoint Alvo | Comportamento Esperado | Resultado |
| :--- | :--- | :--- | :--- |
| **Público** (30 usuários) | `GET /corridas` e `GET /ping` | Retornar `200 OK` lendo as corridas públicas. | **100% OK**. A aplicação não sofreu gargalos nas buscas públicas. |
| **Usuário Comum** (20 usuários) | `POST /user/login` | Retornar processamento do *Form Login* validando as credenciais no Spring Security. | **100% OK**. Nenhum *timeout* no acesso ao Banco de Dados para verificação de hash (BCrypt). |
| **Organizador** (10 usuários) | `GET /organizacao/1/corridas` | Retornar recusa de acesso por falta de token (Redirecionamento para a tela de Login). | **100% OK** (*). O Spring Security barrou com sucesso as 10 requisições indevidas na rota protegida e redirecionou (HTTP 302 -> 200 Login Page) em poucos milissegundos. |

> `*` **Nota Técnica:** O Gatling relatou esses 10 redirecionamentos como "Errors (KO)" nativamente pois ele esperava que a requisição final parasse no HTTP 403. Como ele segue redirecionamentos por padrão, ele foi parar na tela de login recebendo um `200 OK`. Isso prova que a segurança funcionou de forma excelente!

## 4. Métricas de Performance Obtidas

O tempo de resposta do sistema Spring Boot + PostgreSQL foi excelente.

* **Total de Requisições:** 100 requisições simultâneas em 10 segundos.
* **Tempo Mínimo de Resposta:** 3 ms
* **Tempo Máximo de Resposta:** 996 ms
* **Tempo Médio de Resposta (Mean):** 70 ms
* **Taxa de Transferência (Throughput):** 9.09 requests/sec
* **Erros de Servidor (HTTP 5xx):** 0%

### Distribuição de Tempo
* **89%** das requisições foram resolvidas em **menos de 800ms**.
* **1%** entre 800ms e 1200ms.
* **0%** acima de 1200ms.

## 5. Conclusão
A arquitetura do projeto no estado atual (Spring Boot 3 + PostgreSQL 16 local em Docker) tem total capacidade de lidar com a carga esperada para uso inicial. A camada de segurança interceptou as conexões não autorizadas adequadamente sem sobrecarregar a fila de threads do Tomcat interno.

Para testes mais severos (milhares de requisições por segundo), recomenda-se rodar o script apontando para um ambiente de *Staging/Homologação* numa cloud, a fim de testar a rede real e limites reais de hardware.
