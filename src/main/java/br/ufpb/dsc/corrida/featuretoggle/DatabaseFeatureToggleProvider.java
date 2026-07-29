package br.ufpb.dsc.corrida.featuretoggle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import br.ufpb.dsc.corrida.user.User;

/**
 * Implementação de {@link FeatureToggleProvider} que consulta o banco de dados PostgreSQL
 * via {@link FeatureFlagRepository} e aplica uma camada de cache em memória (Caffeine).
 *
 * <h3>Política de Cache</h3>
 * <ul>
 *   <li>Resultados {@code true} são cacheados no cache {@code "featureFlags"} com TTL configurável
 *       via {@code feature-toggle.cache-ttl-seconds} (padrão: 300 s).</li>
 *   <li>Resultados {@code false} <b>não são cacheados</b> ({@code unless = "#result == false"}),
 *       permitindo que flags recém-habilitadas sejam reconhecidas na próxima chamada ao banco.</li>
 * </ul>
 *
 * <h3>Evicção de Cache</h3>
 * Chame {@link #evictCache(String)} após atualizar programaticamente um flag no banco para
 * garantir propagação imediata da mudança.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseFeatureToggleProvider implements FeatureToggleProvider {

    private final FeatureFlagRepository featureFlagRepository;
    private final UserFeatureFlagRepository userFeatureFlagRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private DatabaseFeatureToggleProvider self;

    /**
     * {@inheritDoc}
     *
     * <p>Verifica o banco globalmente via cache. Se não estiver ativo globalmente,
     * verifica a permissão específica do usuário logado (sem cache global).
     */
    @Override
    public boolean isEnabled(String featureKey) {
        boolean globalEnabled = self.isGlobalEnabled(featureKey);

        if (globalEnabled) {
            return true;
        }

        // Verifica permissão específica do usuário logado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return userFeatureFlagRepository.existsByUserIdAndFeatureName(user.getId(), featureKey);
        }

        return false;
    }

    /**
     * Verifica globalmente se a flag está ativa e faz cache se for `true`.
     */
    @Cacheable(value = "featureFlags", key = "#featureKey", unless = "#result == false")
    public boolean isGlobalEnabled(String featureKey) {
        return featureFlagRepository.findByKeyName(featureKey)
                .map(FeatureFlag::isEnabled)
                .orElse(false);
    }

    /**
     * Remove a entrada do cache para o feature flag especificado.
     *
     * <p>Deve ser chamado sempre que um flag for atualizado programaticamente ou via API
     * administrativa para garantir que a mudança seja propagada imediatamente.
     *
     * @param featureKey chave do feature flag a ser removida do cache
     */
    @CacheEvict(value = "featureFlags", key = "#featureKey")
    public void evictCache(String featureKey) {
        log.info("[FeatureToggle] Cache eviccionado para o flag: '{}'", featureKey);
    }
}
