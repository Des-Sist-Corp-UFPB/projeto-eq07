package br.ufpb.dsc.corrida.exception;

/**
 * Exceção lançada quando ocorre falha na comunicação com a API do Mercado Pago
 * (timeout, erro 5xx, resposta inesperada, etc.).
 */
public class MercadoPagoException extends RuntimeException {

    public MercadoPagoException(String message) {
        super(message);
    }

    public MercadoPagoException(String message, Throwable cause) {
        super(message, cause);
    }
}
