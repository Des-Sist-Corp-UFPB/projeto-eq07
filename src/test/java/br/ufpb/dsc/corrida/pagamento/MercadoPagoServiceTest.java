package br.ufpb.dsc.corrida.pagamento;

import br.ufpb.dsc.corrida.config.MercadoPagoConfig;
import br.ufpb.dsc.corrida.exception.MercadoPagoException;
import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.StatusPagamento;
import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserInfo;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResponse;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MercadoPagoService — Unit Tests")
class MercadoPagoServiceTest {

    @Mock
    private MercadoPagoConfig mercadoPagoConfig;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    private MercadoPagoService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoService();
        ReflectionTestUtils.setField(service, "mercadoPagoConfig", mercadoPagoConfig);
        ReflectionTestUtils.setField(service, "pagamentoRepository", pagamentoRepository);
        ReflectionTestUtils.setField(service, "userInfoRepository", userInfoRepository);

        // Uso do lenient() para evitar UnnecessaryStubbingException nos testes que não usam o token
        lenient().when(mercadoPagoConfig.getAccessToken()).thenReturn("token-test");
    }

    @Test
    @DisplayName("Should create a Pix payment successfully and persist the generated payment")
    void shouldCreatePixPaymentSuccessfully() throws Exception {
        Inscricao inscricao = criarInscricaoValida();
        UserInfo userInfo = criarUserInfo(inscricao.getUsuario(), "123.456.789-00");
        Payment payment = mock(Payment.class);

        when(userInfoRepository.findByUsuarioId(10L)).thenReturn(Optional.of(userInfo));
        when(payment.getId()).thenReturn(987654321L);
        when(payment.getStatus()).thenReturn("pending");
        when(payment.getPointOfInteraction()).thenReturn(null);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedConstruction<PaymentClient> mockedConstruction = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.create(any(), any())).thenReturn(payment))) {
            Pagamento resultado = service.criarCobrancaPix(inscricao);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getMpPaymentId()).isEqualTo(987654321L);
            assertThat(resultado.getAmount()).isEqualByComparingTo(new BigDecimal("35.50"));
            assertThat(resultado.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
            assertThat(resultado.getPaymentMethod()).isEqualTo(MetodoPagamento.PIX);
            assertThat(resultado.getIdempotencyKey()).isNotBlank();
            assertThat(resultado.getExpirationDate()).isNotNull();
            assertThat(resultado.getInscricao()).isEqualTo(inscricao);

            PaymentClient constructedClient = mockedConstruction.constructed().get(0);
            verify(constructedClient).create(any(), any(MPRequestOptions.class));
        }
    }

    @Test
    @DisplayName("Should wrap Mercado Pago API failures into MercadoPagoException")
    void shouldWrapApiFailuresIntoMercadoPagoException() {
        Inscricao inscricao = criarInscricaoValida();
        UserInfo userInfo = criarUserInfo(inscricao.getUsuario(), "123.456.789-00");
        MPApiException apiException = mock(MPApiException.class);
        MPResponse apiResponse = mock(MPResponse.class);

        when(userInfoRepository.findByUsuarioId(10L)).thenReturn(Optional.of(userInfo));
        when(apiException.getStatusCode()).thenReturn(400);
        when(apiException.getApiResponse()).thenReturn(apiResponse);
        when(apiResponse.getContent()).thenReturn("invalid request");

        try (MockedConstruction<PaymentClient> mockedConstruction = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.create(any(), any())).thenThrow(apiException))) {
            assertThatThrownBy(() -> service.criarCobrancaPix(inscricao))
                    .isInstanceOf(MercadoPagoException.class)
                    .hasMessageContaining("Falha ao gerar cobrança Pix");

            assertThat(mockedConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user info is missing")
    void shouldThrowResourceNotFoundWhenUserInfoIsMissing() {
        Inscricao inscricao = criarInscricaoValida();

        when(userInfoRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarCobrancaPix(inscricao))
                .isInstanceOf(org.apache.velocity.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    @DisplayName("Should return the Mercado Pago payment status when lookup succeeds")
    void shouldReturnPaymentStatusWhenLookupSucceeds() {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("approved");

        try (MockedConstruction<PaymentClient> mockedConstruction = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.get(123456L)).thenReturn(payment))) {
            String status = service.consultarStatusPagamento(123456L);

            assertThat(status).isEqualTo("approved");
            assertThat(mockedConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("Should wrap lookup failures into MercadoPagoException")
    void shouldWrapLookupFailuresIntoMercadoPagoException() {
        MPException mpException = mock(MPException.class);

        try (MockedConstruction<PaymentClient> mockedConstruction = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.get(123456L)).thenThrow(mpException))) {
            assertThatThrownBy(() -> service.consultarStatusPagamento(123456L))
                    .isInstanceOf(MercadoPagoException.class)
                    .hasMessageContaining("Falha de comunicação");

            assertThat(mockedConstruction.constructed()).hasSize(1);
        }
    }

    private Inscricao criarInscricaoValida() {
        User usuario = new User();
        usuario.setId(10L);
        usuario.setNome("Ana Silva");
        usuario.setLogin("ana@example.com");

        Race corrida = new Race();
        corrida.setId(20L);
        corrida.setNome("Corrida do Sol");
        corrida.setValorInscricao(new BigDecimal("35.50"));

        Inscricao inscricao = new Inscricao();
        inscricao.setId(30L);
        inscricao.setUsuario(usuario);
        inscricao.setCorrida(corrida);
        return inscricao;
    }

    private UserInfo criarUserInfo(User usuario, String cpf) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsuario(usuario);
        userInfo.setCpf(cpf);
        return userInfo;
    }
}