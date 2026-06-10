package br.ufpb.dsc.corrida.exception.userinfo;

/**
 * Lançada quando não é encontrado um registro UserInfo para o usuário informado.
 * Resulta em HTTP 404 Not Found.
 */
public class UserInfoNaoEncontradoException extends RuntimeException {
    public UserInfoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
