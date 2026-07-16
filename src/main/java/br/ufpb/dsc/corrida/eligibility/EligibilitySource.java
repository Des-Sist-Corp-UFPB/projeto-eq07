package br.ufpb.dsc.corrida.eligibility;

/**
 * Indica a origem do resultado de elegibilidade retornado ao usuário.
 *
 * <p>Usado no log de auditoria {@code ELIGIBILITY_AUDIT} para diferenciar um {@code apto: true}
 * resultante de avaliação real da LLM daquele produzido por um caminho de fallback.
 */
public enum EligibilitySource {

    /** A LLM avaliou os dados e retornou um resultado válido. */
    LLM_ASSESSED,

    /** O usuário não concedeu consentimento para processamento de dados de saúde. */
    NO_CONSENT,

    /** A chamada à LLM excedeu o timeout configurado. */
    LLM_TIMEOUT,

    /** A LLM retornou erro ou resposta inválida após retentativa. */
    LLM_ERROR,

    /** O usuário excedeu o limite de requisições por minuto. */
    RATE_LIMITED,

    /** Resultado recuperado do cache — sem nova chamada à LLM. */
    CACHE_HIT
}
