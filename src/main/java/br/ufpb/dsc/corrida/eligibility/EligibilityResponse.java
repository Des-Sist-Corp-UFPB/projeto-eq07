package br.ufpb.dsc.corrida.eligibility;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta estruturada retornada pela LLM para análise de risco.
 *
 * <p>{@code apto = true} — nenhum risco relevante identificado.<br>
 * {@code apto = false} — risco identificado; {@code resposta} contém a justificativa.
 *
 * <p><strong>Framing:</strong> este resultado NÃO é um laudo médico nem uma aprovação/reprovação
 * de participação. É um alerta informativo gerado automaticamente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EligibilityResponse(boolean apto, String resposta) {

    /** Resultado de fallback seguro — nenhuma informação disponível, não bloquear o usuário. */
    public static EligibilityResponse fallback() {
        return new EligibilityResponse(true, null);
    }
}
