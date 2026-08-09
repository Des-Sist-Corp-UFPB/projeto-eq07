package br.ufpb.dsc.corrida.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuração do cache em memória com Caffeine.
 *
 * <p>Caches registrados:
 * <ul>
 *   <li>{@code eligibilityChecks} — armazena resultados da análise de risco via LLM.
 *       TTL configurável via {@code litellm.cache-ttl-seconds} (padrão: 86400 s = 24 h).</li>
 *   <li>{@code featureFlags} — armazena o estado de Feature Flags consultados no banco.
 *       TTL configurável via {@code feature-toggle.cache-ttl-seconds} (padrão: 300 s = 5 min).
 *       Apenas resultados {@code true} são cacheados; flags desabilitadas são sempre relidas
 *       do banco para detectar habilitações imediatamente.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${litellm.cache-ttl-seconds:86400}")
    private long eligibilityCacheTtlSeconds;

    @Value("${feature-toggle.cache-ttl-seconds:300}")
    private long featureToggleCacheTtlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache eligibilityChecks = new CaffeineCache(
                "eligibilityChecks",
                Caffeine.newBuilder()
                        .expireAfterWrite(eligibilityCacheTtlSeconds, TimeUnit.SECONDS)
                        .maximumSize(10_000)
                        .build()
        );

        CaffeineCache featureFlags = new CaffeineCache(
                "featureFlags",
                Caffeine.newBuilder()
                        .expireAfterWrite(featureToggleCacheTtlSeconds, TimeUnit.SECONDS)
                        .maximumSize(1_000)
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(eligibilityChecks, featureFlags));
        return manager;
    }
}

