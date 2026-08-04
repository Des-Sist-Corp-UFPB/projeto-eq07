package br.ufpb.dsc.corrida.inscricao;

import br.ufpb.dsc.corrida.exception.CpfObrigatorioException;
import br.ufpb.dsc.corrida.exception.MercadoPagoException;
import br.ufpb.dsc.corrida.pagamento.ComprovantePdfService;
import br.ufpb.dsc.corrida.pagamento.MercadoPagoService;
import br.ufpb.dsc.corrida.pagamento.Pagamento;
import br.ufpb.dsc.corrida.pagamento.PagamentoRepository;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import br.ufpb.dsc.corrida.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscricaoController — Unit Tests (MockMvc)")
class InscricaoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InscricaoService inscricaoService;

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private MercadoPagoService mercadoPagoService;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private ComprovantePdfService pdfService;

    @InjectMocks
    private InscricaoController inscricaoController;

    private User user;
    private Race race;
    private Inscricao inscricao;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setNome("Atleta Teste");

        race = new Race();
        race.setId(100L);
        race.setNome("Corrida de Teste");
        race.setSlug("corrida-de-teste");

        inscricao = new Inscricao();
        inscricao.setId(500L);
        inscricao.setUsuario(user);
        inscricao.setCorrida(race);
        inscricao.setStatus(StatusInscricao.CONFIRMADA);

        mockMvc = MockMvcBuilders.standaloneSetup(inscricaoController)
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(user))
                .build();
    }

    private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
        private final User user;

        TestAuthenticationPrincipalResolver(User user) {
            this.user = user;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && User.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return user;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /corridas/{id}/inscrever
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("inscrever() — sucesso em corrida gratuita redireciona para comprovante")
    void inscrever_sucessoGratuita() throws Exception {
        inscricao.setStatus(StatusInscricao.CONFIRMADA);
        when(inscricaoService.inscrever(any(), eq(100L), anyBoolean())).thenReturn(inscricao);

        mockMvc.perform(post("/corridas/100/inscrever")
                        .param("riskAcknowledged", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscricoes/500/comprovante"));
    }

    @Test
    @DisplayName("inscrever() — sucesso em corrida paga redireciona para página da corrida (slug)")
    void inscrever_sucessoPaga() throws Exception {
        inscricao.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        when(inscricaoService.inscrever(any(), eq(100L), anyBoolean())).thenReturn(inscricao);

        mockMvc.perform(post("/corridas/100/inscrever"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/corridas/corrida-de-teste"));
    }

    @Test
    @DisplayName("inscrever() — lança CpfObrigatorioException e redireciona para /minhaConta")
    void inscrever_cpfObrigatorio() throws Exception {
        when(inscricaoService.inscrever(any(), eq(100L), anyBoolean()))
                .thenThrow(new CpfObrigatorioException("CPF obrigatório"));

        mockMvc.perform(post("/corridas/100/inscrever"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/minhaConta?cpfObrigatorio=true"))
                .andExpect(flash().attribute("cpfObrigatorio", true));
    }

    @Test
    @DisplayName("inscrever() — lança MercadoPagoException e redireciona para a corrida com erroPagamento")
    void inscrever_mercadoPagoException() throws Exception {
        when(inscricaoService.inscrever(any(), eq(100L), anyBoolean()))
                .thenThrow(new MercadoPagoException("Erro MP"));
        when(raceRepository.findById(100L)).thenReturn(Optional.of(race));

        mockMvc.perform(post("/corridas/100/inscrever"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/corridas/corrida-de-teste?erroPagamento=true"))
                .andExpect(flash().attribute("erroPagamento", "Erro MP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /inscricoes/{id}/pagamento
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("telaPagamento() — exibe tela de pagamento para o dono com status AGUARDANDO_PAGAMENTO")
    @WithMockUser(username = "atleta@test.com", roles = {"USUARIO"})
    void telaPagamento_sucesso() throws Exception {
        inscricao.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        Pagamento pagamento = new Pagamento();
        pagamento.setExpirationDate(OffsetDateTime.now().plusHours(1));
        inscricao.setPagamento(pagamento);

        when(inscricaoRepository.findById(500L)).thenReturn(Optional.of(inscricao));

        mockMvc.perform(get("/inscricoes/500/pagamento")
                        .principal(() -> "atleta"))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/pagamento-pix"))
                .andExpect(model().attributeExists("inscricao", "pagamento", "expirado"));
    }

    @Test
    @DisplayName("telaPagamento() — redireciona para comprovante se já estiver confirmada")
    void telaPagamento_jaConfirmada() throws Exception {
        inscricao.setStatus(StatusInscricao.CONFIRMADA);
        when(inscricaoRepository.findById(500L)).thenReturn(Optional.of(inscricao));

        mockMvc.perform(get("/inscricoes/500/pagamento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscricoes/500/comprovante"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /inscricoes/{id}/pagamento/renovar
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("renovarPagamento() — renova cobrança Pix com sucesso")
    void renovarPagamento_sucesso() throws Exception {
        inscricao.setStatus(StatusInscricao.CANCELADA);
        when(inscricaoRepository.findById(500L)).thenReturn(Optional.of(inscricao));

        mockMvc.perform(post("/inscricoes/500/pagamento/renovar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscricoes/500/pagamento"));

        verify(mercadoPagoService).criarCobrancaPix(inscricao);
    }

    @Test
    @DisplayName("renovarPagamento() — com erro no MercadoPago adiciona flash attribute")
    void renovarPagamento_erro() throws Exception {
        when(inscricaoRepository.findById(500L)).thenReturn(Optional.of(inscricao));
        doThrow(new MercadoPagoException("Erro ao renovar")).when(mercadoPagoService).criarCobrancaPix(any());

        mockMvc.perform(post("/inscricoes/500/pagamento/renovar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscricoes/500/pagamento"))
                .andExpect(flash().attribute("erroPagamento", "Erro ao renovar"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /inscricoes/{id}/comprovante
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("telaComprovante() — exibe tela de comprovante para o dono")
    void telaComprovante_sucesso() throws Exception {
        when(inscricaoRepository.findById(500L)).thenReturn(Optional.of(inscricao));

        mockMvc.perform(get("/inscricoes/500/comprovante"))
                .andExpect(status().isOk())
                .andExpect(view().name("corrida/comprovante-inscricao"))
                .andExpect(model().attributeExists("inscricao"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/inscricoes/{id}/status
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("statusInscricao() — retorna JSON com o status quando encontrado")
    void statusInscricao_sucesso() throws Exception {
        inscricao.setStatus(StatusInscricao.CONFIRMADA);
        when(inscricaoRepository.findById(500L)).thenReturn(Optional.of(inscricao));

        mockMvc.perform(get("/api/inscricoes/500/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("statusInscricao() — retorna 404 quando não encontrada")
    void statusInscricao_naoEncontrada() throws Exception {
        when(inscricaoRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/inscricoes/999/status"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /inscricoes/{id}/cancelar
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelar() — cancela a inscrição e redireciona para /minhas-inscricoes")
    void cancelar_sucesso() throws Exception {
        mockMvc.perform(post("/inscricoes/500/cancelar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/minhas-inscricoes"));

        verify(inscricaoService).cancelar(any(), eq(500L));
    }
}
