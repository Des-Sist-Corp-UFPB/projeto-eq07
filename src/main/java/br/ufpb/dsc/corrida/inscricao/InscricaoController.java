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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
public class InscricaoController {

    private static final Logger log = LoggerFactory.getLogger(InscricaoController.class);

    @Autowired
    private InscricaoService inscricaoService;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private RaceRepository raceRepository;

    /** Inscrição em uma corrida. Redireciona para pagamento se a corrida for paga. */
    @PostMapping("/corridas/{id}/inscrever")
    public String inscrever(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long raceId,
            @RequestParam(value = "riskAcknowledged", required = false, defaultValue = "false")
                boolean riskAcknowledged,
            RedirectAttributes redirectAttributes) {

        log.info("[InscricaoController] Solicitação de inscrição corridaId={} usuárioId={}",
                raceId, user != null ? user.getId() : null);

        try {
            Inscricao inscricao = inscricaoService.inscrever(user, raceId, riskAcknowledged);

            if (inscricao.getStatus() == StatusInscricao.CONFIRMADA) {
                // Corrida gratuita — vai para comprovante
                return "redirect:/inscricoes/" + inscricao.getId() + "/comprovante";
            } else {
                // Corrida paga — redireciona para a página da corrida exibindo o Pix pendente
                return "redirect:/corridas/" + inscricao.getCorrida().getSlug();
            }

        } catch (CpfObrigatorioException e) {
            log.warn("[InscricaoController] CPF obrigatório: usuárioId={}", user.getId());
            redirectAttributes.addFlashAttribute("cpfObrigatorio", true);
            redirectAttributes.addFlashAttribute("msgCpf", e.getMessage());
            return "redirect:/minhaConta?cpfObrigatorio=true";

        } catch (MercadoPagoException e) {
            log.error("[InscricaoController] Falha ao criar Pix para corridaId={}", raceId, e);
            redirectAttributes.addFlashAttribute("erroPagamento", e.getMessage());
            Race race = raceRepository.findById(raceId).orElse(null);
            String target = (race != null && race.getSlug() != null) ? race.getSlug() : raceId.toString();
            return "redirect:/corridas/" + target + "?erroPagamento=true";
        }
    }

    /**
     * Tela de pagamento Pix.
     * Redireciona automaticamente se já aprovado ou mostra opção de renovar se expirado.
     */
    @GetMapping("/inscricoes/{id}/pagamento")
    public String telaPagamento(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long inscricaoId,
            Model model) {

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        // Segurança: somente o dono pode ver
        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            return "redirect:/";
        }

        if (inscricao.getStatus() == StatusInscricao.CONFIRMADA
                || inscricao.getStatus() == StatusInscricao.ATIVA) {
            return "redirect:/inscricoes/" + inscricaoId + "/comprovante";
        }

        Pagamento pagamento = inscricao.getPagamento();
        boolean expirado = pagamento != null
                && pagamento.getExpirationDate() != null
                && pagamento.getExpirationDate().isBefore(java.time.OffsetDateTime.now());

        model.addAttribute("inscricao", inscricao);
        model.addAttribute("pagamento", pagamento);
        model.addAttribute("expirado", expirado);
        return "corrida/pagamento-pix";
    }

    /** Regenera uma cobrança Pix expirada. */
    @PostMapping("/inscricoes/{id}/pagamento/renovar")
    public String renovarPagamento(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long inscricaoId,
            RedirectAttributes redirectAttributes) {

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            return "redirect:/";
        }

        try {
            // Recria a inscrição em AGUARDANDO_PAGAMENTO e gera novo Pix
            inscricao.setStatus(StatusInscricao.AGUARDANDO_PAGAMENTO);
            inscricaoRepository.save(inscricao);
            mercadoPagoService.criarCobrancaPix(inscricao);
            return "redirect:/inscricoes/" + inscricaoId + "/pagamento";
        } catch (MercadoPagoException e) {
            redirectAttributes.addFlashAttribute("erroPagamento", e.getMessage());
            return "redirect:/inscricoes/" + inscricaoId + "/pagamento";
        }
    }

    /** Tela de comprovante de inscrição. */
    @GetMapping("/inscricoes/{id}/comprovante")
    public String telaComprovante(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long inscricaoId,
            Model model) {

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            return "redirect:/";
        }

        model.addAttribute("inscricao", inscricao);
        return "corrida/comprovante-inscricao";
    }

    /** Download do comprovante em PDF. */
    @GetMapping("/inscricoes/{id}/comprovante/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long inscricaoId,
            @Autowired ComprovantePdfService pdfService) {

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada."));

        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        byte[] pdf = pdfService.gerarComprovante(inscricao);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"comprovante-" + inscricaoId + ".pdf\"")
                .body(pdf);
    }

    /** Endpoint de polling — retorna o status atual da inscrição como JSON. */
    @GetMapping("/api/inscricoes/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> statusInscricao(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long inscricaoId) {

        Optional<Inscricao> opt = inscricaoRepository.findById(inscricaoId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Inscricao inscricao = opt.get();
        if (!inscricao.getUsuario().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(Map.of("status", inscricao.getStatus().name()));
    }

    @PostMapping("/inscricoes/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal User user,
                           @PathVariable("id") Long inscricaoId) {
        log.info("[InscricaoController] Cancelamento inscricaoId={} usuárioId={}",
                inscricaoId, user != null ? user.getId() : null);
        inscricaoService.cancelar(user, inscricaoId);
        return "redirect:/minhas-inscricoes";
    }
}
