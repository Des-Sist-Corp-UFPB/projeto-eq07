package br.ufpb.dsc.corrida.user;

import br.ufpb.dsc.corrida.config.security.TokenService;
import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException;
import br.ufpb.dsc.corrida.user.dto.EditarUsuarioDTO;
import br.ufpb.dsc.corrida.user.dto.LoginDto;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;
import br.ufpb.dsc.corrida.user.dto.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.storage.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — Testes Unitários")
class UsuarioServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UsuarioService service;

    private User usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = mock(User.class);
        lenient().when(usuarioMock.getId()).thenReturn(1L);
        lenient().when(usuarioMock.getNome()).thenReturn("João Silva");
        lenient().when(usuarioMock.getUserUsername()).thenReturn("joaosilva");
        lenient().when(usuarioMock.getLogin()).thenReturn("joao@email.com");
        lenient().when(usuarioMock.getPapel()).thenReturn(Papel.USUARIO);
    }

    // ─────────────────────────────────────────────
    // registrar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("registrar() — persiste usuário e retorna token com sucesso")
    void registrar_devePersistirERetornarToken_comSucesso() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("João Silva", "joaosilva", "joao@email.com", "senha123");

        when(repository.existsByLogin(dto.login())).thenReturn(false);
        when(repository.existsByUsername(dto.username())).thenReturn(false);
        when(repository.save(any(User.class))).thenReturn(usuarioMock);
        when(tokenService.criarToken(any(User.class))).thenReturn("token-jwt");

        String token = service.registrar(dto);

        assertThat(token).isEqualTo("token-jwt");
        verify(repository).save(any(User.class));
        verify(tokenService).criarToken(any(User.class));
    }

    @Test
    @DisplayName("registrar() — lança conflito quando login já está em uso")
    void registrar_deveLancarConflito_quandoLoginJaExiste() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("João Silva", "joaosilva", "joao@email.com", "senha123");

        when(repository.existsByLogin(dto.login())).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(dto))
                .isInstanceOf(UsuarioJaExistenteException.class)
                .hasMessageContaining("Login");

        verify(repository, never()).save(any());
        verify(tokenService, never()).criarToken(any());
    }

    @Test
    @DisplayName("registrar() — lança conflito quando username já está em uso")
    void registrar_deveLancarConflito_quandoUsernameJaExiste() {
        RegistrarUsuarioDTO dto = new RegistrarUsuarioDTO("João Silva", "joaosilva", "joao@email.com", "senha123");

        when(repository.existsByLogin(dto.login())).thenReturn(false);
        when(repository.existsByUsername(dto.username())).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(dto))
                .isInstanceOf(UsuarioJaExistenteException.class)
                .hasMessageContaining("Username");

        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // login()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("login() — retorna token quando credenciais são válidas")
    void login_deveRetornarToken_comCredenciaisValidas() {
        LoginDto dto = new LoginDto("joao@email.com", "senha123");

        Authentication autenticacao = mock(Authentication.class);
        when(autenticacao.getPrincipal()).thenReturn(usuarioMock);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(autenticacao);
        when(tokenService.criarToken(usuarioMock)).thenReturn("token-jwt");

        String token = service.login(dto);

        assertThat(token).isEqualTo("token-jwt");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).criarToken(usuarioMock);
    }

    @Test
    @DisplayName("login() — lança BadCredentialsException quando credenciais são inválidas")
    void login_deveLancarExcecao_quandoCredenciaisInvalidas() {
        LoginDto dto = new LoginDto("joao@email.com", "senhaErrada");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BadCredentialsException.class);

        verify(tokenService, never()).criarToken(any());
    }

    // ─────────────────────────────────────────────
    // editar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("editar() — atualiza e retorna usuário com sucesso")
    void editar_deveAtualizarERetornar_comSucesso() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Novo Nome", null, null, null);

        configurarSecurityContext(usuarioMock);
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioMock.getId()).thenReturn(1L);

        User resultado = service.editar(dto, 1L);

        assertThat(resultado).isNotNull();
        verify(usuarioMock).editar(dto);
    }

    @Test
    @DisplayName("editar() — lança não-encontrado quando usuário não existe")
    void editar_deveLancarNaoEncontrado_quandoUsuarioNaoExiste() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Novo Nome", null, null, null);

        configurarSecurityContext(usuarioMock);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(dto, 99L))
                .isInstanceOf(UsuarioNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("editar() — lança acesso negado quando usuário tenta editar outro")
    void editar_deveLancarAcessoNegado_quandoEditandoOutroUsuario() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO("Hacker", null, null, null);

        User outroUsuario = mock(User.class);
        when(outroUsuario.getId()).thenReturn(2L);

        configurarSecurityContext(usuarioMock);  // logado como id=1
        when(repository.findById(2L)).thenReturn(Optional.of(outroUsuario));

        assertThatThrownBy(() -> service.editar(dto, 2L))
                .isInstanceOf(AcessoNaoPermitidoException.class);

        verify(outroUsuario, never()).editar(any());
    }

    @Test
    @DisplayName("editar() — lança conflito quando novo login já está em uso por outro usuário")
    void editar_deveLancarConflito_quandoLoginDuplicado() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, null, "duplicado@email.com", null);

        configurarSecurityContext(usuarioMock);
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(repository.existsByLoginAndIdNot("duplicado@email.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(dto, 1L))
                .isInstanceOf(UsuarioJaExistenteException.class)
                .hasMessageContaining("login");

        verify(usuarioMock, never()).editar(any());
    }

    @Test
    @DisplayName("editar() — lança conflito quando novo username já está em uso por outro usuário")
    void editar_deveLancarConflito_quandoUsernameDuplicado() {
        EditarUsuarioDTO dto = new EditarUsuarioDTO(null, "usernameDuplicado", null, null);

        configurarSecurityContext(usuarioMock);
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(repository.existsByUsernameAndIdNot("usernameDuplicado", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.editar(dto, 1L))
                .isInstanceOf(UsuarioJaExistenteException.class)
                .hasMessageContaining("username");

        verify(usuarioMock, never()).editar(any());
    }

    // ─────────────────────────────────────────────
    // deletar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deletar() — deleta o próprio usuário com sucesso")
    void deletar_deveDeletar_comSucesso() {
        configurarSecurityContext(usuarioMock);
        when(repository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        service.deletar(1L);

        verify(usuarioMock).changeDeletado();
    }

    @Test
    @DisplayName("deletar() — lança não-encontrado quando usuário não existe")
    void deletar_deveLancarNaoEncontrado_quandoUsuarioNaoExiste() {
        configurarSecurityContext(usuarioMock);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(99L))
                .isInstanceOf(UsuarioNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("deletar() — lança acesso negado quando USUARIO tenta deletar outro usuário")
    void deletar_deveLancarAcessoNegado_quandoUsuarioTentaDeletarOutro() {
        User outroUsuario = mock(User.class);
        when(outroUsuario.getId()).thenReturn(2L);

        when(usuarioMock.getPapel()).thenReturn(Papel.USUARIO);
        configurarSecurityContext(usuarioMock); // logado como id=1

        when(repository.findById(2L)).thenReturn(Optional.of(outroUsuario));

        assertThatThrownBy(() -> service.deletar(2L))
                .isInstanceOf(AcessoNaoPermitidoException.class);

        verify(outroUsuario, never()).changeDeletado();
    }

    @Test
    @DisplayName("deletar() — ADMINISTRADOR consegue deletar qualquer usuário")
    void deletar_devePermitir_quandoAdministrador() {
        User admin = mock(User.class);
        // lenient() evita UnnecessaryStubbingException caso o service
        // não precise checar getId() do admin no fluxo de ADMINISTRADOR
        lenient().when(admin.getId()).thenReturn(1L);
        lenient().when(admin.getPapel()).thenReturn(Papel.ADMINISTRADOR);

        User alvo = mock(User.class);
        lenient().when(alvo.getId()).thenReturn(2L);

        configurarSecurityContext(admin);
        when(repository.findById(2L)).thenReturn(Optional.of(alvo));

        service.deletar(2L);

        verify(alvo).changeDeletado();
    }

    // ─────────────────────────────────────────────
    // buscarPerfilPublico()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("buscarPerfilPublico() — retorna PerfilPublicoDTO com sucesso")
    void buscarPerfilPublico_deveRetornarPerfil_comSucesso() {
        when(repository.findByUsername("joaosilva")).thenReturn(usuarioMock);
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        PerfilPublicoDTO resultado = service.buscarPerfilPublico("joaosilva");

        assertThat(resultado).isNotNull();
        assertThat(resultado.username()).isEqualTo("joaosilva");
        assertThat(resultado.nome()).isEqualTo("João Silva");
        assertThat(resultado.totalKmRun()).isEqualTo(0.0f);
    }

    @Test
    @DisplayName("buscarPerfilPublico() — retorna foto e km quando userInfo existe")
    void buscarPerfilPublico_deveRetornarFotoEKm_quandoUserInfoExiste() {
        var userInfo = mock(br.ufpb.dsc.corrida.user.UserInfo.class);
        when(userInfo.getFotoPerfil()).thenReturn("/uploads/foto.png");
        when(userInfo.getTotalKmRun()).thenReturn(42.5f);

        when(repository.findByUsername("joaosilva")).thenReturn(usuarioMock);
        when(userInfoRepository.findByUsuarioId(1L)).thenReturn(Optional.of(userInfo));
        when(storageService.getPresignedUrl("/uploads/foto.png")).thenReturn("url-presigned");

        PerfilPublicoDTO resultado = service.buscarPerfilPublico("joaosilva");

        assertThat(resultado.fotoPerfil()).isEqualTo("url-presigned");
        assertThat(resultado.totalKmRun()).isEqualTo(42.5f);
    }

    @Test
    @DisplayName("buscarPerfilPublico() — lança não-encontrado quando username não existe")
    void buscarPerfilPublico_deveLancarNaoEncontrado_quandoNaoExiste() {
        when(repository.findByUsername("fantasma")).thenReturn(null);

        assertThatThrownBy(() -> service.buscarPerfilPublico("fantasma"))
                .isInstanceOf(UsuarioNaoEncontradoException.class)
                .hasMessageContaining("fantasma");
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    /**
     * Injeta um usuário autenticado no SecurityContextHolder para simular
     * o comportamento de SecurityContextHolder.getContext().getAuthentication()
     * que é usado internamente em editar() e deletar().
     */
    private void configurarSecurityContext(User usuario) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(usuario);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);
    }
}