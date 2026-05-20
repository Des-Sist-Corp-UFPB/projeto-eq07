package br.ufpb.dsc.corrida.exception.user;

public class UsuarioJaExistenteException extends RuntimeException {
    public UsuarioJaExistenteException(String message) {
        super(message);
    }
}
