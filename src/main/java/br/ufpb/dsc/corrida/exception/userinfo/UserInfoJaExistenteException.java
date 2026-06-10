package br.ufpb.dsc.corrida.exception.userinfo;

/**
 * Lançada quando já existe um registro UserInfo para o usuário informado.
 * Resulta em HTTP 409 Conflict.
 */
public class UserInfoJaExistenteException extends RuntimeException {
    public UserInfoJaExistenteException(String mensagem) {
        super(mensagem);
    }
}
