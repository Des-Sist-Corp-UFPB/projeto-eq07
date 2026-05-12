package br.ufpb.dsc.corrida.exception.user;

public class UsuarioJaExistente extends RuntimeException {
    public UsuarioJaExistente(String message) {
        super(message);
    }
}
