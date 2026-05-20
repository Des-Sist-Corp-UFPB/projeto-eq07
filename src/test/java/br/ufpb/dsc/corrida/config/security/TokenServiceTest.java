package br.ufpb.dsc.corrida.config.security;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.enums.Papel;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        // Sets the secret using reflection since it relies on Spring's @Value
        ReflectionTestUtils.setField(tokenService, "secret", "my-super-secret-test-key-which-has-to-be-long-enough");
    }

    @Test
    void deveCriarTokenJWTValido() {
        Usuario usuario = new Usuario(1L, "Nome", "username", "login", "senha", Papel.USUARIO, false);

        String token = tokenService.criarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.startsWith("eyJ")); // Base64 representation of standard JWT header
    }

    @Test
    void deveExtrairSubjectComSucesso() {
        Usuario usuario = new Usuario(123L, "Nome Teste", "user_teste", "login_teste", "senha_teste", Papel.ADMINISTRADOR, false);
        String token = tokenService.criarToken(usuario);

        String subject = tokenService.getSubject(token);

        assertEquals("login_teste", subject);
    }

    @Test
    void deveLancarExcecaoAoValidarTokenInvalido() {
        String tokenInvalido = "invalid-token-signature";

        assertThrows(JWTVerificationException.class, () -> {
            tokenService.getSubject(tokenInvalido);
        });
    }

    @Test
    void deveCriarTokenComExpiracaoCorreta() {
        Instant expiracao = tokenService.createExpireToken();

        assertNotNull(expiracao);
        assertTrue(expiracao.isAfter(Instant.now()));
    }
}
