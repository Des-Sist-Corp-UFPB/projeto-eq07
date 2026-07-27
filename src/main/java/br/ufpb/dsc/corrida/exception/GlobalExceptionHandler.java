package br.ufpb.dsc.corrida.exception;

import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoJaExistenteException;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.exception.race.InscricaoException;
import br.ufpb.dsc.corrida.featuretoggle.FeatureDisabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Tratamento centralizado de exceções para toda a API.
 *
 * <p>Essa classe captura as exceções lançadas em qualquer Controller
 * e retorna uma resposta padronizada ({@link ErrorResponseDTO}),
 * sem expor stack traces ao cliente.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // === Exceções de negócio (já existentes no projeto) ===

    @ExceptionHandler(CorridaNaoEncontradaException.class)
    public ResponseEntity<ErrorResponseDTO> tratarCorridaNaoEncontrada(CorridaNaoEncontradaException ex) {
        log.warn("[Exception] Corrida não encontrada: {}", ex.getMessage());
        var erro = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Não encontrado",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(UserInfoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> tratarUserInfoNaoEncontrado(UserInfoNaoEncontradoException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Não encontrado",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(UserInfoJaExistenteException.class)
    public ResponseEntity<ErrorResponseDTO> tratarUserInfoJaExistente(UserInfoJaExistenteException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Conflito",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponseDTO> tratarArgumentoOuEstadoIlegal(RuntimeException ex) {
        var erro = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

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



    @ExceptionHandler(InscricaoException.class)
    public ResponseEntity<String> tratarInscricaoException(InscricaoException ex, jakarta.servlet.http.HttpServletRequest request) {
        
        if (request.getHeader("HX-Request") != null) {
            String html = String.format(
                "<div id='toast-error' class='fixed bottom-4 right-4 bg-red-600 text-white px-6 py-4 rounded-xl shadow-lg font-bold flex items-center gap-3 z-50' " +
                "hx-swap-oob='true'>" +
                "<span>%s</span>" +
                "<button onclick=\"this.parentElement.remove()\" class='text-white/80 hover:text-white'>&times;</button>" +
                "</div>", 
                ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(html);
        }
        throw ex; 
    }

    @ExceptionHandler(AcessoNaoPermitidoException.class)
    public ResponseEntity<ErrorResponseDTO> tratarAcessoNaoPermitido(AcessoNaoPermitidoException ex, jakarta.servlet.http.HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        
        // Se a requisição veio de uma página HTML (Web), deixa a exceção passar direto
        // para o Spring aplicar o @ResponseStatus(HttpStatus.FORBIDDEN) da sua classe
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            throw ex;
        }

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

    // === Feature Toggle ===

    /**
     * Trata tentativas de acesso a funcionalidades protegidas por Feature Flag desabilitada.
     * Retorna HTTP 503 Service Unavailable para sinalizar indisponibilidade temporária do recurso.
     */
    @ExceptionHandler(FeatureDisabledException.class)
    public ResponseEntity<Map<String, String>> tratarFeatureDesabilitada(FeatureDisabledException ex) {
        log.warn("[FeatureToggle] Acesso bloqueado: feature '{}' está desabilitada.", ex.getFeatureKey());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    // === Fallback genérico (qualquer outra exceção) ===

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> tratarExcecaoGenerica(Exception ex) {
        log.error("[Exception] Exceção não tratada capturada pelo GlobalExceptionHandler: {}", ex.getMessage(), ex);
        var erro = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
