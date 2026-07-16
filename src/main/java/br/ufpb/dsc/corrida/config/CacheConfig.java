package br.ufpb.dsc.corrida.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuração do cache em memória com Caffeine.
 *
 * <p>O cache {@code eligibilityChecks} armazena os resultados da análise de risco via LLM
 * para evitar chamadas repetidas para o mesmo par (usuário, corrida) dentro da janela de TTL.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${litellm.cache-ttl-seconds:86400}")
    private long cacheTtlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("eligibilityChecks");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
                .maximumSize(10_000));
        return manager;
    }
}
