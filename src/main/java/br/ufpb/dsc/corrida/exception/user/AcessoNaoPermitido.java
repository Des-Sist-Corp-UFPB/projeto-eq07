package br.ufpb.dsc.corrida.exception.user;

public class AcessoNaoPermitido extends RuntimeException {
    public AcessoNaoPermitido(String message) {
        super(message);
    }
}
