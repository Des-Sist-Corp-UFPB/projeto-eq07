package br.ufpb.dsc.corrida.pagamento;

import br.ufpb.dsc.corrida.config.MercadoPagoConfig;
import br.ufpb.dsc.corrida.exception.MercadoPagoException;
import br.ufpb.dsc.corrida.inscricao.Inscricao;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.StatusPagamento;
import br.ufpb.dsc.corrida.user.UserInfoRepository;

import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.net.Headers;
import lombok.extern.slf4j.Slf4j;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço de integração com o Mercado Pago para criação e consulta de cobranças Pix.
 */
@Slf4j
@Service
public class MercadoPagoService {

    private static final int EXPIRACAO_MINUTOS = 30;

    @Autowired
    private MercadoPagoConfig mercadoPagoConfig;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    /**
     * Cria uma cobrança Pix no Mercado Pago para a inscrição informada.
     *
     * <p>O CPF do atleta é garantido como preenchido antes de chegar aqui
     * (validado em InscricaoService antes da chamada).
     *
     * @param inscricao inscrição com corrida e usuário carregados
     * @return entidade Pagamento persistida com QR code e chave Pix
     * @throws MercadoPagoException em caso de falha de rede ou erro da API
     */
    @Transactional
    public Pagamento criarCobrancaPix(Inscricao inscricao) {
        Race corrida = inscricao.getCorrida();
        String email = inscricao.getUsuario().getLogin();
        String idempotencyKey = UUID.randomUUID().toString();

        log.info("[MP] Criando cobrança Pix: inscricaoId={} corridaId={} idempotencyKey={}",
                inscricao.getId(), corrida.getId(), idempotencyKey);

        var userInfoOptional = userInfoRepository.findByUsuarioId(inscricao.getUsuario().getId());
        if (!userInfoOptional.isPresent()) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }

        var userInfo = userInfoOptional.get();

        // 1. Limpar CPF para conter APENAS números (Remove . - e espaços)
        String cpf = null;
        if (userInfo.getCpf() != null && !userInfo.getCpf().isBlank()) {
            cpf = userInfo.getCpf().replaceAll("[^0-9]", "");
        }

        // 2. Extrair Nome e Sobrenome do usuário (obrigatório para PIX)
        String nomeCompleto = userInfo.getUsuario().getNome() != null ? userInfo.getUsuario().getNome().trim() : "Participante";
        String firstName = nomeCompleto;
        String lastName = "Corrida"; // Valor padrão caso não tenha sobrenome
        
        if (nomeCompleto.contains(" ")) {
            int idxEspaco = nomeCompleto.indexOf(" ");
            firstName = nomeCompleto.substring(0, idxEspaco);
            lastName = nomeCompleto.substring(idxEspaco + 1);
        }

        // 3. Formatar Expiracao mantendo Offset correto (-03:00 / UTC)
        OffsetDateTime expiracao = OffsetDateTime.now().plusMinutes(EXPIRACAO_MINUTOS);

        // Montagem do Payer
        PaymentPayerRequest.PaymentPayerRequestBuilder payerBuilder = PaymentPayerRequest.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName);

        if (cpf != null && !cpf.isBlank()) {
            payerBuilder.identification(
                IdentificationRequest.builder()
                    .type("CPF")
                    .number(cpf) // CPF apenas numérico
                    .build()
            );
        }

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .transactionAmount(corrida.getValorInscricao())
                .description("Inscrição — " + corrida.getNome())
                .paymentMethodId("pix")
                .payer(payerBuilder.build())
                .dateOfExpiration(expiracao)
                .build();

        MPRequestOptions options = MPRequestOptions.builder()
                .accessToken(mercadoPagoConfig.getAccessToken())
                .customHeaders(Map.of(
                        Headers.IDEMPOTENCY_KEY, idempotencyKey
                ))
                .build();

        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.create(request, options);

            log.info("[MP] Cobrança criada com sucesso: mpPaymentId={} status={}",
                    payment.getId(), payment.getStatus());

            Pagamento pagamento = new Pagamento();
            pagamento.setInscricao(inscricao);
            pagamento.setMpPaymentId(payment.getId());
            pagamento.setAmount(corrida.getValorInscricao());
            pagamento.setIdempotencyKey(idempotencyKey);
            pagamento.setExpirationDate(expiracao);
            pagamento.setStatus(StatusPagamento.PENDENTE);
            pagamento.setPaymentMethod(MetodoPagamento.PIX);

            if (payment.getPointOfInteraction() != null
                    && payment.getPointOfInteraction().getTransactionData() != null) {
                var txData = payment.getPointOfInteraction().getTransactionData();
                pagamento.setQrCodePix(txData.getQrCode());
                pagamento.setQrCodeBase64Pix(txData.getQrCodeBase64());
            }

            return pagamentoRepository.save(pagamento);

        } catch (MPApiException e) {
            log.error("[MP] Erro de API ao criar cobrança Pix: status={} mensagem={}",
                    e.getStatusCode(), e.getApiResponse().getContent(), e);
            throw new MercadoPagoException(
                    "Falha ao gerar cobrança Pix. Código: " + e.getStatusCode(), e);
        } catch (MPException e) {
            log.error("[MP] Erro de rede ao criar cobrança Pix", e);
            throw new MercadoPagoException("Falha de comunicação com o Mercado Pago. Tente novamente.", e);
        }
    }

    /**
     * Consulta o status autoritativo de um pagamento diretamente na API do Mercado Pago.
     * Usado pelo webhook para double-check antes de atualizar estado interno.
     *
     * @param mpPaymentId ID do pagamento no Mercado Pago
     * @return status retornado pela API ("approved", "pending", "rejected", etc.)
     * @throws MercadoPagoException em caso de falha
     */
    public String consultarStatusPagamento(Long mpPaymentId) {
        log.info("[MP] Consultando status do pagamento mpPaymentId={}", mpPaymentId);
        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(mpPaymentId);
            log.info("[MP] Status do pagamento mpPaymentId={}: {}", mpPaymentId, payment.getStatus());
            return payment.getStatus();
        } catch (MPApiException e) {
            log.error("[MP] Erro de API ao consultar pagamento mpPaymentId={}: {}",
                    mpPaymentId, e.getApiResponse().getContent(), e);
            throw new MercadoPagoException("Falha ao consultar pagamento no Mercado Pago.", e);
        } catch (MPException e) {
            log.error("[MP] Erro de rede ao consultar pagamento mpPaymentId={}", mpPaymentId, e);
            throw new MercadoPagoException("Falha de comunicação com o Mercado Pago.", e);
        }
    }
}
