package br.ufpb.dsc.corrida.exception;

/**
 * Lançada quando uma chamada a um serviço externo (ex: OpenRouteService) falha
 * por timeout, erro de servidor ou problema de conectividade.
 *
 * <p>A mensagem é localizada e adequada para exibição direta ao usuário.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}
