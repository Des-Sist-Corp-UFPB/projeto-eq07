package br.ufpb.dsc.corrida.featuretoggle;

/**
 * Contrato de abstração para consulta de Feature Flags.
 *
 * <p>O desacoplamento desta interface do aspecto AOP {@link FeatureToggleAspect}
 * permite substituir a implementação de backend (banco de dados, arquivo de configuração,
 * serviço remoto) sem alterar a lógica de interceptação.
 */
public interface FeatureToggleProvider {

    /**
     * Verifica se o Feature Flag identificado por {@code featureKey} está habilitado.
     *
     * @param featureKey chave única do feature flag (ex: "PAYMENT_V2")
     * @return {@code true} se o flag estiver habilitado; {@code false} caso contrário ou se a chave não existir
     */
    boolean isEnabled(String featureKey);
}
