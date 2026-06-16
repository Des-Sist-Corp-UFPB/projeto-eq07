package br.ufpb.dsc.corrida.user;

import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.userConections.UserConnection;
import br.ufpb.dsc.corrida.userConections.UserConnectionRepository;
import br.ufpb.dsc.corrida.userConections.UserConnectionService;
import br.ufpb.dsc.corrida.config.security.AutenticacaoFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UsuarioViewController — Testes das Views (Thymeleaf)")
class UsuarioViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserInfoService userInfoService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserConnectionRepository userConnectionRepository;

    @MockBean
    private UserConnectionService userConnectionService;

    // Mockar o filtro impede que ele quebre o carregamento do contexto da aplicação
    @MockBean
    private AutenticacaoFilter autenticacaoFilter;

    private User mockUser;

    @BeforeEach
    void setUp() throws Exception {
        // Configura o mock do filtro para apenas continuar a cadeia de execução (Filter Chain)
        doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(autenticacaoFilter).doFilter(any(), any(), any()); // Alterado de doFilterInternal para doFilter

        // Instancia o usuário mockado
        mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "nome", "User Teste");
        ReflectionTestUtils.setField(mockUser, "username", "peterson_bala");
        ReflectionTestUtils.setField(mockUser, "login", "user@teste.com");
        ReflectionTestUtils.setField(mockUser, "papel", Papel.USUARIO);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /registrar & GET /login
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /registrar — deve retornar a view de registro com o DTO populado")
    void exibirFormularioRegistro_deveRetornarViewCorreta() throws Exception {
        mockMvc.perform(get("/registrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registrar"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @DisplayName("GET /login — deve retornar a view de login padrão")
    void login_deveRetornarViewCorreta() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /minhaConta
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /minhaConta — deve carregar dados da conta com sucesso se autenticado")
    void minhaConta_comUsuarioAutenticado_deveRetornarDados() throws Exception {
        UserInfoRespostaDTO userInfoMock = Mockito.mock(UserInfoRespostaDTO.class);
        when(userInfoService.buscarPorUsuarioId(1L)).thenReturn(userInfoMock);

        mockMvc.perform(get("/minhaConta")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("minha-conta"))
                .andExpect(model().attribute("userInfo", userInfoMock))
                .andExpect(model().attribute("usuarioId", 1L));
    }

    @Test
    @DisplayName("GET /minhaConta — deve setar profileMissing se o userInfoService estourar Exception")
    void minhaConta_quandoNaoEncontraPerfil_deveSinalizarProfileMissing() throws Exception {
        when(userInfoService.buscarPorUsuarioId(1L)).thenThrow(new UserInfoNaoEncontradoException("Não encontrado"));

        mockMvc.perform(get("/minhaConta")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("minha-conta"))
                .andExpect(model().attribute("usuarioId", 1L))
                .andExpect(model().attribute("profileMissing", true));
    }

    @Test
    @DisplayName("GET /minhaConta — deve retornar a página mesmo que o usuário não tenha perfil (UserInfo) criado")
    void minhaConta_comPrincipalNuloOuInvalido_deveRetornarApenasAView() throws Exception {
        // Forçamos o serviço a retornar que o perfil não existe
        when(userInfoService.buscarPorUsuarioId(mockUser.getId()))
                .thenThrow(new UserInfoNaoEncontradoException("Perfil não preenchido"));

        // Passamos o mockUser para que o fragmento 'sidebar' do Thymeleaf 
        // consiga ler as propriedades de autenticação (#authentication.principal) sem estourar
        mockMvc.perform(get("/minhaConta")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("minha-conta"))
                .andExpect(model().attribute("profileMissing", true));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /user/{username}/profile
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /user/{username}/profile — deve lançar 404 se o usuário alvo não existir")
    void getProfilePage_usuarioInexistente_deveLancarException() throws Exception {
        when(userRepository.findByUsername("hacker")).thenReturn(null);

        mockMvc.perform(get("/user/hacker/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /user/{username}/profile — deve renderizar perfil com conexões e token Bearer")
    void getProfilePage_comUsuarioAutenticadoEToken_deveMontarModelAndViewCompleto() throws Exception {
        User alvoUser = new User();
        ReflectionTestUtils.setField(alvoUser, "id", 2L);

        PerfilPublicoDTO perfilMock = Mockito.mock(PerfilPublicoDTO.class);
        UserConnection conexaoMock = new UserConnection();
        CsrfToken csrfMock = Mockito.mock(CsrfToken.class);

        when(userRepository.findByUsername("alvo_user")).thenReturn(alvoUser);
        when(usuarioService.buscarPerfilPublico("alvo_user")).thenReturn(perfilMock);
        when(userConnectionRepository.countConnectionsByUserId(2L)).thenReturn(5L);
        when(userConnectionRepository.findConnectionBetweenUsers(1L, 2L)).thenReturn(Optional.of(conexaoMock));

        mockMvc.perform(get("/user/alvo_user/profile")
                        .with(user(mockUser))
                        .requestAttr(CsrfToken.class.getName(), csrfMock)
                        .header("Authorization", "Bearer meu-token-jwt"))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil-publico"))
                .andExpect(model().attribute("perfil", perfilMock))
                .andExpect(model().attribute("perfilId", 2L))
                .andExpect(model().attribute("_csrf", csrfMock))
                .andExpect(model().attribute("countConections", 5L))
                .andExpect(model().attribute("conexao", conexaoMock))
                .andExpect(model().attribute("token", "meu-token-jwt"));
    }

    @Test
    @DisplayName("GET /user/{username}/profile — deve funcionar sem usuário logado (público)")
    void getProfilePage_anonimo_deveRenderizarApenasDadosBasicos() throws Exception {
        User alvoUser = new User();
        ReflectionTestUtils.setField(alvoUser, "id", 2L);
        PerfilPublicoDTO perfilMock = Mockito.mock(PerfilPublicoDTO.class);

        when(userRepository.findByUsername("alvo_user")).thenReturn(alvoUser);
        when(usuarioService.buscarPerfilPublico("alvo_user")).thenReturn(perfilMock);

        mockMvc.perform(get("/user/alvo_user/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil-publico"))
                .andExpect(model().attribute("perfil", perfilMock))
                .andExpect(model().attribute("perfilId", 2L))
                .andExpect(model().attributeDoesNotExist("countConections"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /solicitacoes
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /solicitacoes — redireciona para login se não autenticado")
    void exibirSolicitacoes_naoAutenticado_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/solicitacoes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /solicitacoes — renderiza lista de pendências se autenticado")
    void exibirSolicitacoes_autenticado_deveRetornarListaEView() throws Exception {
        CsrfToken csrfMock = Mockito.mock(CsrfToken.class);
        when(userConnectionService.getPendingRequestsList(1L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/solicitacoes")
                        .with(user(mockUser))
                        .requestAttr(CsrfToken.class.getName(), csrfMock))
                .andExpect(status().isOk())
                .andExpect(view().name("user/solicitacoes"))
                .andExpect(model().attribute("_csrf", csrfMock))
                .andExpect(model().attributeExists("solicitacoes"));
    }
}