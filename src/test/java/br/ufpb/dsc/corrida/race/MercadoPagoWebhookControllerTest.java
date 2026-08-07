package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.inscricao.InscricaoRepository;
import br.ufpb.dsc.corrida.inscricao.StatusInscricao;
import br.ufpb.dsc.corrida.pagamento.MercadoPagoService;
import br.ufpb.dsc.corrida.pagamento.Pagamento;
import br.ufpb.dsc.corrida.pagamento.PagamentoRepository;
import br.ufpb.dsc.corrida.user.Papel;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mercadopago.webhook-secret=test-secret")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MercadoPagoWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MercadoPagoService mercadoPagoService;

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private br.ufpb.dsc.corrida.organizer.OrganizationRepository organizationRepository;

    @Autowired
    private br.ufpb.dsc.corrida.organizer.OrganizerRepository organizerRepository;

    private Pagamento pagamento;
    private Inscricao inscricao;

    private String gerarSignatureValida(Long paymentId, String requestId, String ts) throws Exception {
        String template = "id:" + paymentId + ";request-id:" + requestId + ";ts:" + ts + ";";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("test-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(template.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String v1 = java.util.HexFormat.of().formatHex(hash);
        return "ts=" + ts + ",v1=" + v1;
    }

    private Race criarCorridaValida() {
        long timestamp = System.currentTimeMillis();

        User user = new User();
        user.setNome("Peterson Treinador teste");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");

        user.setLogin("user_" + timestamp + "@gmail.com");
        user.setUsername("atleta_" + System.currentTimeMillis());

        var userSaved = userRepository.save(user);

        br.ufpb.dsc.corrida.organizer.Organizer organizer = new br.ufpb.dsc.corrida.organizer.Organizer();
        organizer.setUsuario(userSaved);
        long timestampCref = System.currentTimeMillis();
        organizer.setCref((timestampCref % 100000) + "-G/PB");
        organizer.setEmail("org_" + timestamp + "@test.com");
        organizer.setWhatsapp("83999999999");
        organizer.setUfConselho("PB");

        String cpfDinamico = String.format("%011d", timestamp % 100000000000L);
        organizer.setCpf(cpfDinamico);

        organizer = organizerRepository.save(organizer);

        br.ufpb.dsc.corrida.organizer.Organization org = new br.ufpb.dsc.corrida.organizer.Organization();
        org.setName("Org Webhook");
        org.setFoundedAt(java.time.LocalDate.now());
        org.setCity("João Pessoa");
        org.setState("PB");
        org.setOrganizer(organizer);
        org = organizationRepository.save(org);

        Race corrida = new Race();
        corrida.setNome("Corrida Webhook Test");
        corrida.setSlug("corrida-1-vaga-" + System.currentTimeMillis());
        corrida.setDescricao("Descrição válida de teste webhook");
        corrida.setDataInicio(java.time.OffsetDateTime.now().plusDays(10));
        corrida.setValorInscricao(new BigDecimal("30.00"));
        corrida.setStatus(StatusCorrida.PUBLICADA);
        corrida.setCategoria(CategoriaCorrida.C5K);
        corrida.setLargadaLat(-7.1195);
        corrida.setLargadaLng(-34.8450);
        corrida.setLargadaEndereco("Av. Cabo Branco");
        corrida.setChegadaLat(-7.1195);
        corrida.setChegadaLng(-34.8450);
        corrida.setChegadaEndereco("Busto de Tamandaré");
        corrida.setOrganization(org);
        return raceRepository.save(corrida);
    }

    @BeforeEach
    void setUp() {
        Race corrida = criarCorridaValida();
        long timestamp = System.currentTimeMillis();

        User user = new User();
        user.setNome("Peterson Treinador teste");
        user.setPapel(Papel.ORGANIZADOR);
        user.setSenha("12345678");

        user.setLogin("user_" + timestamp + "@gmail.com");
        user.setUsername("atleta_" + System.currentTimeMillis());

        var userSaved = userRepository.save(user);

        inscricao = new Inscricao();
        inscricao.setUsuario(userSaved);
        inscricao.setCorrida(corrida);
        inscricao.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
        inscricao = inscricaoRepository.save(inscricao);

        pagamento = new Pagamento();
        pagamento.setInscricao(inscricao);
        pagamento.setMpPaymentId(999888777L);
        pagamento.setAmount(new BigDecimal("30.00"));
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento = pagamentoRepository.save(pagamento);
    }

    @Test
    @DisplayName("Webhook: Notificação de pagamento aprovado deve confirmar inscrição e disparar e-mail")
    void webhook_pagamentoAprovado_deveConfirmarInscricao() throws Exception {
        when(mercadoPagoService.consultarStatusPagamento(999888777L)).thenReturn("approved");

        String jsonPayload = """
            {
                "action": "payment.updated",
                "data": { "id": "999888777" }
            }
            """;

        String requestId = "req-123";
        String ts = String.valueOf(System.currentTimeMillis());
        String signature = gerarSignatureValida(999888777L, requestId, ts);

        mockMvc.perform(post("/api/v1/webhooks/mercadopago")
                        .header("x-signature", signature)
                        .header("x-request-id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        verify(emailService, times(1)).enviarComprovante(inscricao.getId());
    }

    @Test
    @DisplayName("Webhook Idempotência: Notificação para pagamento já APROVADO deve retornar 200 sem reprocessar")
    void webhook_idempotencia_naoDeveReprocessar() throws Exception {
        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamentoRepository.save(pagamento);

        String jsonPayload = """
            {
                "action": "payment.updated",
                "data": { "id": "999888777" }
            }
            """;

        String requestId = "req-456";
        String ts = String.valueOf(System.currentTimeMillis());
        String signature = gerarSignatureValida(999888777L, requestId, ts);

        mockMvc.perform(post("/api/v1/webhooks/mercadopago")
                        .header("x-signature", signature)
                        .header("x-request-id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        verify(mercadoPagoService, never()).consultarStatusPagamento(anyLong());
        verify(emailService, never()).enviarComprovante(anyLong());
    }
}
