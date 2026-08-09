package br.ufpb.dsc.corrida.exception;

/**
 * Exceção lançada quando um atleta tenta se inscrever em uma corrida paga
 * sem ter CPF cadastrado no perfil.
 */
public class CpfObrigatorioException extends RuntimeException {

    public CpfObrigatorioException(String message) {
        super(message);
    }
}
