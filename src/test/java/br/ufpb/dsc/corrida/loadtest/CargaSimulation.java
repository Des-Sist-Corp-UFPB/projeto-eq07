package br.ufpb.dsc.corrida.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class CargaSimulation extends Simulation {

    // Define o protocolo HTTP base
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/x-www-form-urlencoded");

    // Cenário 1: Perfil Público (Navega pelas corridas e ping)
    ScenarioBuilder cenarioPublico = scenario("Perfil Público")
        .exec(
            http("Ping (Health Check)")
                .get("/ping")
                .check(status().is(200))
        )
        .pause(1)
        .exec(
            http("Listar Corridas")
                .get("/corridas")
                // Pode retornar 200 (Thymeleaf/HTML)
                .check(status().in(200, 302)) 
        );

    // Cenário 2: Perfil Usuário Comum (Tenta login)
    ScenarioBuilder cenarioUsuario = scenario("Perfil Usuário")
        .exec(
            http("Login Form")
                .post("/user/login")
                .formParam("login", "atleta_loadtest")
                .formParam("senha", "123456")
                // Como não sabemos se ele já existe no banco, aceitamos 200 (sucesso) ou 401/403 (falha auth)
                .check(status().in(200, 401, 403))
        );

    // Cenário 3: Perfil Organizador (Acesso negado ou restrito a área protegida)
    ScenarioBuilder cenarioOrganizador = scenario("Perfil Organizador")
        .exec(
            http("Acessar área do Organizador 1")
                .get("/organizacao/1/corridas")
                // Sem token, esperamos um 403 Forbidden, 401 ou 302 para login
                .check(status().in(401, 403, 302))
        );

    // Configuração de Injeção: Busca do Ponto Exato de Quebra (1 Segundo)
    {
        setUp(
            cenarioPublico.injectOpen(
                // Inicia com 1 req/s e vai até 150 req/s ao longo de 60 segundos
                rampUsersPerSec(1).to(150).during(Duration.ofSeconds(60))
            ),
            cenarioUsuario.injectOpen(
                // Inicia com 1 req/s e vai até 50 req/s ao longo de 60 segundos
                rampUsersPerSec(1).to(50).during(Duration.ofSeconds(60))
            )
        ).protocols(httpProtocol);
    }
}
