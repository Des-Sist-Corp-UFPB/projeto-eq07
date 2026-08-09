package br.ufpb.dsc.corrida.exception.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AcessoNaoPermitidoException extends RuntimeException {
    public AcessoNaoPermitidoException(String message) {
        super(message);
    }
}
