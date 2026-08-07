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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
@DisplayName("UsuarioViewController â€” Testes das Views (Thymeleaf)")
class UsuarioViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserInfoService userInfoService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserConnectionRepository userConnectionRepository;

    @MockitoBean
    private UserConnectionService userConnectionService;

    // Mockar o filtro impede que ele quebre o carregamento do contexto da aplicaÃ§Ã£o
    @MockitoBean
    private AutenticacaoFilter autenticacaoFilter;

    private User mockUser;

    @BeforeEach
    void setUp() throws Exception {
        // Configura o mock do filtro para apenas continuar a cadeia de execuÃ§Ã£o (Filter Chain)
        doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(autenticacaoFilter).doFilter(any(), any(), any()); // Alterado de doFilterInternal para doFilter

        // Instancia o usuÃ¡rio mockado
        mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "nome", "User Teste");
        ReflectionTestUtils.setField(mockUser, "username", "peterson_bala");
        ReflectionTestUtils.setField(mockUser, "login", "user@teste.com");
        ReflectionTestUtils.setField(mockUser, "papel", Papel.USUARIO);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /registrar & GET /login
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /registrar â€” deve retornar a view de registro com o DTO populado")
    void exibirFormularioRegistro_deveRetornarViewCorreta() throws Exception {
        mockMvc.perform(get("/registrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registrar"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @DisplayName("GET /login â€” deve retornar a view de login padrÃ£o")
    void login_deveRetornarViewCorreta() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /minhaConta
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /minhaConta â€” deve carregar dados da conta com sucesso se autenticado")
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
    @DisplayName("GET /minhaConta â€” deve setar profileMissing se o userInfoService estourar Exception")
    void minhaConta_quandoNaoEncontraPerfil_deveSinalizarProfileMissing() throws Exception {
        when(userInfoService.buscarPorUsuarioId(1L)).thenThrow(new UserInfoNaoEncontradoException("NÃ£o encontrado"));

        mockMvc.perform(get("/minhaConta")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("minha-conta"))
                .andExpect(model().attribute("usuarioId", 1L))
                .andExpect(model().attribute("profileMissing", true));
    }

    @Test
    @DisplayName("GET /minhaConta â€” deve retornar a pÃ¡gina mesmo que o usuÃ¡rio nÃ£o tenha perfil (UserInfo) criado")
    void minhaConta_comPrincipalNuloOuInvalido_deveRetornarApenasAView() throws Exception {
        // ForÃ§amos o serviÃ§o a retornar que o perfil nÃ£o existe
        when(userInfoService.buscarPorUsuarioId(mockUser.getId()))
                .thenThrow(new UserInfoNaoEncontradoException("Perfil nÃ£o preenchido"));

        // Passamos o mockUser para que o fragmento 'sidebar' do Thymeleaf 
        // consiga ler as propriedades de autenticaÃ§Ã£o (#authentication.principal) sem estourar
        mockMvc.perform(get("/minhaConta")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("minha-conta"))
                .andExpect(model().attribute("profileMissing", true));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /user/{username}/profile
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /user/{username}/profile â€” deve lanÃ§ar 404 se o usuÃ¡rio alvo nÃ£o existir")
    void getProfilePage_usuarioInexistente_deveLancarException() throws Exception {
        when(userRepository.findByUsername("hacker")).thenReturn(null);

        mockMvc.perform(get("/user/hacker/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /user/{username}/profile â€” deve renderizar perfil com conexÃµes e token Bearer")
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
    @DisplayName("GET /user/{username}/profile â€” deve funcionar sem usuÃ¡rio logado (pÃºblico)")
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
                .andExpect(model().attributeExists("countConections"));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /solicitacoes
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /solicitacoes â€” redireciona para login se nÃ£o autenticado")
    void exibirSolicitacoes_naoAutenticado_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/solicitacoes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /solicitacoes â€” renderiza lista de pendÃªncias se autenticado")
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /atletas
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("GET /atletas â€” sem query string deve pesquisar string vazia e renderizar view")
    void searchAtletas_semQuery_devePesquisarVazioERetornarView() throws Exception {
        when(usuarioService.pesquisarUsuarios("")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/atletas")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("atletas"))
                .andExpect(model().attributeExists("atletas"))
                .andExpect(model().attribute("loggedInUserId", mockUser.getId()));
    }

    @Test
    @DisplayName("GET /atletas â€” com query string deve pesquisar o termo e renderizar view")
    void searchAtletas_comQuery_devePesquisarTermoERetornarView() throws Exception {
        when(usuarioService.pesquisarUsuarios("maria")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/atletas")
                        .param("query", "maria")
                        .with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("atletas"))
                .andExpect(model().attributeExists("atletas"))
                .andExpect(model().attribute("query", "maria"))
                .andExpect(model().attribute("loggedInUserId", mockUser.getId()));
    }
}
