package br.ufpb.dsc.corrida.exception.user;

public class UsuarioNaoEncontrado extends RuntimeException{
    public UsuarioNaoEncontrado(String mensagem) {
        super(mensagem);
    }
}
