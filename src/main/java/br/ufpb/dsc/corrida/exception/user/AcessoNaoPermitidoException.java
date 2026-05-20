package br.ufpb.dsc.corrida.exception.user;

public class AcessoNaoPermitidoException extends RuntimeException {
    public AcessoNaoPermitidoException(String message) {
        super(message);
    }
}
