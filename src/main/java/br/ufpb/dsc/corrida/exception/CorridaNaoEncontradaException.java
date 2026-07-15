package br.ufpb.dsc.corrida.exception;

/**
 * Lançada quando uma corrida não é encontrada pelo slug ou ID fornecido,
 * ou quando a corrida está CANCELADA e não deve ser acessível publicamente.
 */
public class CorridaNaoEncontradaException extends RuntimeException {
    public CorridaNaoEncontradaException(String message) {
        super(message);
    }
}
