package br.ufpb.dsc.corrida.race;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Controlador do webhook do Mercado Pago.
 *
 * <p>Fluxo de segurança:
 * <ol>
 *   <li>Valida assinatura HMAC-SHA256 via cabeçalho {@code x-signature}.</li>
 *   <li>Verifica idempotência: pagamentos já processados retornam 200 sem reprocessar.</li>
 *   <li>Double-check: consulta status autoritativo na API do MP antes de atualizar estado.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
public class MercadoPagoWebhookController {

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/mercadopago")
    @Transactional
    public ResponseEntity<Void> receberWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody Map<String, Object> payload) {

        log.info("[Webhook] Recebido: payload={}", payload);

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[Webhook] mercadopago.webhook-secret não configurado — rejeitando por segurança.");
            return ResponseEntity.status(500).build();
        }

        if (!validarAssinatura(xSignature, xRequestId, payload)) {
            log.warn("[Webhook] Assinatura inválida. xSignature={}", xSignature);
            return ResponseEntity.status(401).build();
        }

        // Extrai o ID do pagamento da notificação
        Long mpPaymentId = extrairMpPaymentId(payload);
        if (mpPaymentId == null) {
            log.warn("[Webhook] Payload sem payment ID reconhecível: {}", payload);
            return ResponseEntity.ok().build();
        }

        // 2. Idempotência: pagamento já aprovado não é reprocessado
        var pagamentoOpt = pagamentoRepository.findByMpPaymentId(mpPaymentId);
        if (pagamentoOpt.isPresent()) {
            Pagamento pagamento = pagamentoOpt.get();
            if (pagamento.getStatus() == StatusPagamento.APROVADO) {
                log.info("[Webhook] Idempotência: mpPaymentId={} já APROVADO.", mpPaymentId);
                return ResponseEntity.ok().build();
            }

            // 3. Double-check: consulta status autoritativo na API do MP
            String statusMP;
            try {
                statusMP = mercadoPagoService.consultarStatusPagamento(mpPaymentId);
            } catch (Exception e) {
                log.error("[Webhook] Falha ao consultar status mpPaymentId={}", mpPaymentId, e);
                return ResponseEntity.ok().build(); // Retorna 200 para evitar retry storm
            }

            // 4. Processa transição de estado
            if ("approved".equals(statusMP)) {
                pagamento.setStatus(StatusPagamento.APROVADO);
                pagamentoRepository.save(pagamento);

                Inscricao inscricao = pagamento.getInscricao();
                inscricao.setStatus(StatusInscricao.CONFIRMADA);
                inscricaoRepository.save(inscricao);

                log.info("[Webhook] inscricaoId={} CONFIRMADA após Pix aprovado.",
                        inscricao.getId());

                // Dispara e-mail assíncrono (não bloqueia a resposta)
                emailService.enviarComprovante(inscricao.getId());

            } else if ("rejected".equals(statusMP) || "cancelled".equals(statusMP)) {
                pagamento.setStatus(StatusPagamento.CANCELADO);
                pagamentoRepository.save(pagamento);

                Inscricao inscricao = pagamento.getInscricao();
                inscricao.setStatus(StatusInscricao.CANCELADA);
                inscricaoRepository.save(inscricao);

                log.info("[Webhook] inscricaoId={} CANCELADA. statusMP={}",
                        inscricao.getId(), statusMP);
            } else {
                log.info("[Webhook] Status ignorado para mpPaymentId={}: {}", mpPaymentId, statusMP);
            }
        } else {
            log.info("[Webhook] Notificação para mpPaymentId={} não encontrado no banco — ignorando.",
                    mpPaymentId);
        }

        return ResponseEntity.ok().build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Valida a assinatura HMAC-SHA256 do Mercado Pago.
     * Formato do header x-signature: "ts=<timestamp>,v1=<hash>"
     * Template de assinatura: "id:<mp_payment_id>;request-id:<x-request-id>;ts:<timestamp>"
     */
    private boolean validarAssinatura(String xSignature, String xRequestId,
                                   Map<String, Object> payload) {
        if (xSignature == null || xSignature.isBlank()) return false;

        try {
            String ts = null;
            String v1 = null;
            for (String parte : xSignature.split(",")) {
                String[] kv = parte.split("=", 2);
                if (kv.length == 2) {
                    if ("ts".equals(kv[0])) ts = kv[1];
                    if ("v1".equals(kv[0])) v1 = kv[1];
                }
            }

            if (ts == null || v1 == null) return false;

            Long dataId = extrairMpPaymentId(payload);

            // Corrige: header ausente deve virar string vazia, não "null"
            String requestIdSeguro = (xRequestId == null) ? "" : xRequestId;
            String template = "id:" + dataId + ";request-id:" + requestIdSeguro + ";ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(template.getBytes(StandardCharsets.UTF_8));
            String calculado = HexFormat.of().formatHex(hash);

            return calculado.equals(v1);
        } catch (Exception e) {
            log.error("[Webhook] Erro ao validar assinatura", e);
            return false;
        }
    }

    /** Extrai o ID do pagamento do payload do webhook (suporta formato payment e merchant_order). */
    @SuppressWarnings("unchecked")
    private Long extrairMpPaymentId(Map<String, Object> payload) {
        try {
            Object data = payload.get("data");
            if (data instanceof Map) {
                Object id = ((Map<String, Object>) data).get("id");
                if (id != null) return Long.parseLong(id.toString());
            }
        } catch (Exception e) {
            log.warn("[Webhook] Não foi possível extrair mpPaymentId do payload", e);
        }
        return null;
    }
}
