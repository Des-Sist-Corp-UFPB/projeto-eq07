package br.ufpb.dsc.corrida.featuretoggle;

/**
 * Exceção lançada pelo {@link FeatureToggleAspect} quando um método anotado com
 * {@link FeatureToggle} é invocado e o respectivo Feature Flag está desabilitado
 * no banco de dados, sem que um {@code fallbackMethod} tenha sido configurado.
 *
 * <p>Esta exceção é tratada pelo {@code GlobalExceptionHandler} e resulta em uma
 * resposta HTTP <b>503 Service Unavailable</b>.
 */
public class FeatureDisabledException extends RuntimeException {

    private final String featureKey;

    /**
     * Cria uma nova instância para a chave de feature flag especificada.
     *
     * @param featureKey chave do feature flag desabilitado (ex: "PAYMENT_V2")
     */
    public FeatureDisabledException(String featureKey) {
        super("Feature '" + featureKey + "' is currently disabled");
        this.featureKey = featureKey;
    }

    /**
     * @return a chave do feature flag que estava desabilitado
     */
    public String getFeatureKey() {
        return featureKey;
    }
}
