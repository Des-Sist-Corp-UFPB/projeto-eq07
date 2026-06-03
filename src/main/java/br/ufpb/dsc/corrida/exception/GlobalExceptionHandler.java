package br.ufpb.dsc.corrida.exception;

import br.ufpb.dsc.corrida.dto.ErrorResponseDTO;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento centralizado de exceções para toda a API.
 *
 * <p>Essa classe captura as exceções lançadas em qualquer Controller
 * e retorna uma resposta padronizada ({@link ErrorResponseDTO}),
 * sem expor stack traces ao cliente.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // === Exceções de negócio (já existentes no projeto) ===

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> tratarUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Não encontrado",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(UsuarioJaExistenteException.class)
    public ResponseEntity<ErrorResponseDTO> tratarUsuarioJaExistenteException(UsuarioJaExistenteException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Conflito",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(AcessoNaoPermitidoException.class)
    public ResponseEntity<ErrorResponseDTO> tratarAcessoNaoPermitido(AcessoNaoPermitidoException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    // === Exceções do Spring Security (login) ===

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> tratarCredenciaisInvalidas(BadCredentialsException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Não autorizado",
                "Credenciais inválidas. Verifique seu login e senha."
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }

    // === Exceções de validação (@Valid) ===

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> tratarValidacao(MethodArgumentNotValidException ex) {
        // Pega a primeira mensagem de erro de validação
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados inválidos");

        var erro = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos",
                mensagem
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // === Fallback genérico (qualquer outra exceção) ===

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> tratarExcecaoGenerica(Exception ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
