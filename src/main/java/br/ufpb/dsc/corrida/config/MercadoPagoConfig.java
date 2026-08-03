package br.ufpb.dsc.corrida.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

/**
 * Configuração da integração com o Mercado Pago.
 *
 * <p>Inicializa o SDK com o access token configurado via variável de ambiente.
 * {@code @EnableAsync} desacopla tarefas pesadas (PDF + e-mail) do fluxo HTTP.
 * {@code @EnableScheduling} ativa o job de expiração de cobranças Pix.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class MercadoPagoConfig {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    /**
     * Injeta o access token no SDK do Mercado Pago na inicialização da aplicação.
     * O SDK usa configuração global (thread-safe).
     */
    @PostConstruct
    public void inicializar() {
        com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);
        log.info("[MercadoPago] SDK inicializado. Sandbox={}", sandbox);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public boolean isSandbox() {
        return sandbox;
    }
}
