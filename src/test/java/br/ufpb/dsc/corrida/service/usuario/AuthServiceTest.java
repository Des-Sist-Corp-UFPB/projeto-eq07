package br.ufpb.dsc.corrida.service.usuario;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.enums.Papel;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveCarregarUsuarioPorLoginComSucesso() {
        String login = "user_login";
        Usuario usuario = new Usuario(1L, "Nome", "user", login, "senha", Papel.USUARIO, false);

        when(repository.findByLogin(login)).thenReturn(usuario);

        UserDetails resultado = authService.loadUserByUsername(login);

        assertNotNull(resultado);
        assertEquals(login, resultado.getUsername());
        verify(repository, times(1)).findByLogin(login);
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoForEncontradoPorLogin() {
        String login = "user_inexistente";

        when(repository.findByLogin(login)).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> {
            authService.loadUserByUsername(login);
        });

        verify(repository, times(1)).findByLogin(login);
    }
}
