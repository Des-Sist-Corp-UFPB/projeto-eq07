package br.ufpb.dsc.corrida.config.security;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.enums.Papel;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepository repository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AutenticacaoFilter filter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarComTokenValido() throws ServletException, IOException {
        String token = "valid_jwt_token";
        String subject = "user_login";
        Usuario usuario = new Usuario(1L, "Nome", "username", subject, "senha", Papel.USUARIO, false);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.getSubject(token)).thenReturn(subject);
        when(repository.findByLogin(subject)).thenReturn(usuario);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(usuario, authentication.getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void deveIgnorarFiltroSemToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void deveExtrairTokenComSucesso() {
        when(request.getHeader("Authorization")).thenReturn("Bearer my_secret_token");
        String token = filter.getToken(request);
        assertEquals("my_secret_token", token);
    }

    @Test
    void deveRetornarNullSeHeaderNaoContiverAuthorization() {
        when(request.getHeader("Authorization")).thenReturn(null);
        String token = filter.getToken(request);
        assertNull(token);
    }
}
