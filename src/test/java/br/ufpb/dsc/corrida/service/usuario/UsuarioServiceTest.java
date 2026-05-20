package br.ufpb.dsc.corrida.service.usuario;

import br.ufpb.dsc.corrida.config.security.TokenService;
import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.user.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.dto.user.LoginDto;
import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.enums.Papel;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitido;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistente;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontrado;
import br.ufpb.dsc.corrida.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private AuthenticationManager authenticationManager;

    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService service;

    private SecurityContext originalSecurityContext;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        org.springframework.test.util.ReflectionTestUtils.setField(tokenService, "secret", "my-super-secret-test-key-which-has-to-be-long-enough-for-hmac256");

        service = new UsuarioService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "repository", repository);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "authenticationManager", authenticationManager);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "tokenService", tokenService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);

        originalSecurityContext = SecurityContextHolder.getContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(originalSecurityContext);
    }

    private void mockAuthentication(Usuario loggedUser) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            loggedUser, null, loggedUser.getAuthorities()
        );
        SecurityContextHolder.setContext(new org.springframework.security.core.context.SecurityContextImpl(auth));
    }

    // === REGISTRAR ===

    @Test
    void deveRegistrarUsuarioComSucesso() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("Nome", "username", "login", "senha");
        when(repository.existsByLogin("login")).thenReturn(false);
        when(repository.existsByUsername("username")).thenReturn(false);
        when(passwordEncoder.encode("senha")).thenReturn("senhaCripto");

        doAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        }).when(repository).save(any(Usuario.class));

        String token = service.registrar(dto);

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
        verify(repository, times(1)).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoRegistrarSeLoginJaExistir() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("Nome", "username", "login", "senha");
        when(repository.existsByLogin("login")).thenReturn(true);

        assertThrows(UsuarioJaExistente.class, () -> service.registrar(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoRegistrarSeUsernameJaExistir() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("Nome", "username", "login", "senha");
        when(repository.existsByLogin("login")).thenReturn(false);
        when(repository.existsByUsername("username")).thenReturn(true);

        assertThrows(UsuarioJaExistente.class, () -> service.registrar(dto));
        verify(repository, never()).save(any());
    }

    // === LOGIN ===

    @Test
    void deveAutenticarELogarComSucesso() {
        LoginDto dto = new LoginDto("login", "senha");
        Usuario usuario = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            usuario, null, usuario.getAuthorities()
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        String token = service.login(dto);

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
    }

    @Test
    void deveLancarExcecaoLoginSePrincipalInvalido() {
        LoginDto dto = new LoginDto("login", "senha");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "invalid_principal_string", null, java.util.Collections.emptyList()
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        assertThrows(AcessoNaoPermitido.class, () -> service.login(dto));
    }

    // === EDITAR ===

    @Test
    void deveEditarUsuarioComSucesso() {
        Usuario loggedUser = new Usuario(1L, "Nome Antigo", "user_antigo", "login_antigo", "senha_antiga", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        when(repository.findById(1L)).thenReturn(Optional.of(loggedUser));
        when(repository.existsByLoginAndIdNot("login_novo", 1L)).thenReturn(false);
        when(repository.existsByUsernameAndIdNot("user_novo", 1L)).thenReturn(false);

        EditarUsuarioDTO dto = new EditarUsuarioDTO("Nome Novo", "user_novo", "login_novo", "senha_nova");
        Usuario editado = service.editar(dto, 1L);

        assertEquals("Nome Novo", editado.getNome());
        assertEquals("user_novo", editado.getUserUsername());
        assertEquals("login_novo", editado.getLogin());
        assertEquals("senha_nova", editado.getSenha());
    }

    @Test
    void deveLancarExcecaoEditarSeUsuarioNaoEncontrado() {
        Usuario loggedUser = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        EditarUsuarioDTO dto = new EditarUsuarioDTO("Nome Novo", null, null, null);
        assertThrows(UsuarioNaoEncontrado.class, () -> service.editar(dto, 99L));
    }

    @Test
    void deveLancarExcecaoEditarSeAcessoNaoPermitido() {
        Usuario loggedUser = new Usuario(1L, "Nome Logado", "user1", "login1", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        Usuario outroUsuario = new Usuario(2L, "Nome Outro", "user2", "login2", "senha", Papel.USUARIO, false);
        when(repository.findById(2L)).thenReturn(Optional.of(outroUsuario));

        EditarUsuarioDTO dto = new EditarUsuarioDTO("Nome Novo", null, null, null);
        assertThrows(AcessoNaoPermitido.class, () -> service.editar(dto, 2L));
    }

    @Test
    void deveLancarExcecaoEditarSeLoginJaExistente() {
        Usuario loggedUser = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        when(repository.findById(1L)).thenReturn(Optional.of(loggedUser));
        when(repository.existsByLoginAndIdNot("login_novo", 1L)).thenReturn(true);

        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, null, "login_novo", null);
        assertThrows(UsuarioJaExistente.class, () -> service.editar(dto, 1L));
    }

    @Test
    void deveLancarExcecaoEditarSeUsernameJaExistente() {
        Usuario loggedUser = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        when(repository.findById(1L)).thenReturn(Optional.of(loggedUser));
        when(repository.existsByLoginAndIdNot("login_novo", 1L)).thenReturn(false);
        when(repository.existsByUsernameAndIdNot("user_novo", 1L)).thenReturn(true);

        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, "user_novo", "login_novo", null);
        assertThrows(UsuarioJaExistente.class, () -> service.editar(dto, 1L));
    }

    // === DELETAR ===

    @Test
    void deveDeletarUsuarioSendoProprioUsuarioComSucesso() {
        Usuario loggedUser = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        when(repository.findById(1L)).thenReturn(Optional.of(loggedUser));

        service.deletar(1L);

        assertTrue(loggedUser.getDeletado());
    }

    @Test
    void deveDeletarUsuarioSendoAdminComSucesso() {
        Usuario loggedAdmin = new Usuario(1L, "Admin", "admin", "admin_login", "senha", Papel.ADMINISTRADOR, false);
        mockAuthentication(loggedAdmin);

        Usuario outroUsuario = new Usuario(2L, "Usuario", "user", "user_login", "senha", Papel.USUARIO, false);
        when(repository.findById(2L)).thenReturn(Optional.of(outroUsuario));

        service.deletar(2L);

        assertTrue(outroUsuario.getDeletado());
    }

    @Test
    void deveLancarExcecaoDeletarUsuarioSemPermissao() {
        Usuario loggedUser = new Usuario(1L, "Usuario1", "user1", "login1", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        Usuario outroUsuario = new Usuario(2L, "Usuario2", "user2", "login2", "senha", Papel.USUARIO, false);
        when(repository.findById(2L)).thenReturn(Optional.of(outroUsuario));

        assertThrows(AcessoNaoPermitido.class, () -> service.deletar(2L));
        assertFalse(outroUsuario.getDeletado());
    }

    @Test
    void deveLancarExcecaoDeletarUsuarioInexistente() {
        Usuario loggedUser = new Usuario(1L, "Nome", "user", "login", "senha", Papel.USUARIO, false);
        mockAuthentication(loggedUser);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoEncontrado.class, () -> service.deletar(99L));
    }
}
