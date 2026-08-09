package br.ufpb.dsc.corrida.featuretoggle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço de fachada para verificação de Feature Flags.
 *
 * <p>Delega a consulta ao {@link FeatureToggleProvider} (com cache) e expõe
 * uma API de alto nível para uso direto em código de negócio, independentemente do AOP.
 *
 * <p>Exemplo de uso programático:
 * <pre>{@code
 * if (featureToggleService.isFeatureEnabled("PAYMENT_V2")) {
 *     // caminho novo
 * } else {
 *     // caminho legado
 * }
 * }</pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FeatureToggleProvider featureToggleProvider;

    /**
     * Verifica se o Feature Flag está habilitado.
     *
     * @param featureKey chave do feature flag (ex: "PAYMENT_V2")
     * @return {@code true} se habilitado; {@code false} caso contrário ou se a chave não existir
     */
    public boolean isFeatureEnabled(String featureKey) {
        boolean enabled = featureToggleProvider.isEnabled(featureKey);
        log.debug("[FeatureToggle] Flag '{}' = {}", featureKey, enabled);
        return enabled;
    }
}
