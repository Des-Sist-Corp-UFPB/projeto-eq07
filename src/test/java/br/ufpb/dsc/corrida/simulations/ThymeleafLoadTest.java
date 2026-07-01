package br.ufpb.dsc.corrida.simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

public class ThymeleafLoadTest extends Simulation {

    // 1. Configuração do Protocolo HTTP (Com Timeouts de Proteção para CI)
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080") 
        .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
        .acceptLanguageHeader("pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
        .upgradeInsecureRequestsHeader("1");

    // 2. Definição do Cenário de Navegação
    ScenarioBuilder scn = scenario("Cenário: Login e Acesso ao Dashboard Thymeleaf") 
        .exec(flushCookieJar()) // Garante isolamento entre as iterações
        .exec(
            // PASSO 1: Abre a página de login
            http("Acessar Tela de Login")
                .get("/login")
                .check(status().is(200))
        )
        .pause(2) 
        .exec(
            // PASSO 2: Envia o formulário de Login via POST
            http("Efetuar Login")
                .post("/login")
                .formParam("username", "admin")
                .formParam("password", "senhaSegura123")
                .check(status().in(200, 302)) 
        )
        .pause(1)
        .exec(
            // PASSO 3: Tenta acessar a página interna protegida
            http("Acessar Home")
                .get("/")
                .check(status().is(200))
        );

    // 3. Perfil de Carga Determinado por Tempo Estável (Duração Total: ~1 Minuto)
    {
        setUp(
            scn.injectClosed(
                // Fase 1: Aquecimento (Sobe de 1 para 30 usuários simultâneos em 20 segundos)
                rampConcurrentUsers(1).to(30).during(20),
                // Fase 2: Sustentação (Segura 30 usuários simultâneos batendo no sistema por mais 40 segundos)
                constantConcurrentUsers(30).during(40)
            )
        ).protocols(httpProtocol);
    }
}