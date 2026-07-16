package br.ufpb.dsc.corrida.eligibility;

/**
 * Lançada quando o serviço LiteLLM está indisponível ou retorna resposta inválida.
 *
 * <p>O serviço de elegibilidade captura essa exceção e aplica o fallback seguro
 * ({@code apto: true}), garantindo que o fluxo de inscrição nunca seja bloqueado
 * por falha deste serviço auxiliar.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
