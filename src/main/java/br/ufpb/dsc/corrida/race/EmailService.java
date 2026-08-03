package br.ufpb.dsc.corrida.race;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.OffsetDateTime;

/**
 * Serviço de envio assíncrono de e-mails.
 * Executado em thread separada via {@code @Async} para não bloquear o webhook.
 */
@Slf4j
@Service
public class EmailService {

    private static final int MAX_TENTATIVAS = 3;
    private static final long BACKOFF_MS = 3000L;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ComprovantePdfService pdfService;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Value("${spring.mail.username:noreply@corridadsc.com}")
    private String remetente;

    /**
     * Envia o e-mail de confirmação com o comprovante PDF em anexo.
     * Tenta até {@value MAX_TENTATIVAS} vezes com backoff entre tentativas.
     *
     * @param inscricaoId ID da inscrição confirmada
     */
    @Async
    @Transactional
    public void enviarComprovante(Long inscricaoId) {
        log.info("[Email] Iniciando envio de comprovante para inscricaoId={}", inscricaoId);

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElse(null);
        if (inscricao == null) {
            log.error("[Email] inscricaoId={} não encontrada — envio cancelado.", inscricaoId);
            return;
        }

        byte[] pdf = pdfService.gerarComprovante(inscricao);
        String destinatario = inscricao.getUsuario().getLogin();
        String nomeAtleta = inscricao.getUsuario().getNome();
        String nomeCorrida = inscricao.getCorrida().getNome();

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                MimeMessage mensagem = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
                helper.setFrom(remetente);
                helper.setTo(destinatario);
                helper.setSubject("✅ Inscrição confirmada — " + nomeCorrida);
                helper.setText(
                        "<p>Olá, <strong>" + nomeAtleta + "</strong>!</p>" +
                        "<p>Sua inscrição na corrida <strong>" + nomeCorrida + "</strong> foi confirmada.</p>" +
                        "<p>Segue em anexo o comprovante de inscrição em PDF.</p>" +
                        "<p>Boa corrida! 🏃</p>",
                        true
                );
                helper.addAttachment(
                        "comprovante-inscricao-" + inscricao.getId() + ".pdf",
                        new org.springframework.core.io.ByteArrayResource(pdf),
                        "application/pdf"
                );
                mailSender.send(mensagem);

                inscricao.setEmailEnviado(true);
                inscricao.setEmailEnviadoEm(OffsetDateTime.now());
                inscricaoRepository.save(inscricao);

                log.info("[Email] Comprovante enviado para {} (inscricaoId={})",
                        destinatario, inscricaoId);
                return;

            } catch (MessagingException e) {
                log.error("[Email] Tentativa {}/{} falhou para inscricaoId={}: {}",
                        tentativa, MAX_TENTATIVAS, inscricaoId, e.getMessage());
                if (tentativa < MAX_TENTATIVAS) {
                    try { Thread.sleep(BACKOFF_MS * tentativa); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }

        log.error("[Email] Todas as {} tentativas falharam para inscricaoId={}. " +
                "emailEnviado=false — suporte pode reenviar manualmente.",
                MAX_TENTATIVAS, inscricaoId);
    }
}
