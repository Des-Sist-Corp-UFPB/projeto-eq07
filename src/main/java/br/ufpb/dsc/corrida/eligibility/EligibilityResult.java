package br.ufpb.dsc.corrida.eligibility;

/**
 * Agrega o resultado da análise de elegibilidade com a sua fonte (origin).
 *
 * <p>A fonte é essencial para auditoria: um {@code apto: true} pode provir de avaliação
 * real da LLM ({@link EligibilitySource#LLM_ASSESSED}) ou de um caminho de fallback como
 * {@link EligibilitySource#LLM_TIMEOUT}, {@link EligibilitySource#NO_CONSENT}, etc.
 */
public record EligibilityResult(EligibilityResponse response, EligibilitySource source) {

    public static EligibilityResult of(EligibilityResponse response, EligibilitySource source) {
        return new EligibilityResult(response, source);
    }
}
