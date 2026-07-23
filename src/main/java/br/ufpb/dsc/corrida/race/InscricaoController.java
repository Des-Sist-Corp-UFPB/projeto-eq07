package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InscricaoController {

    private static final Logger log = LoggerFactory.getLogger(InscricaoController.class);

    @Autowired
    private InscricaoService inscricaoService;

    @PostMapping("/corridas/{id}/inscrever")
    public String inscrever(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long raceId,
            @RequestParam(value = "riskAcknowledged", required = false, defaultValue = "false") boolean riskAcknowledged) {
        log.info("[InscricaoController] Recebida solicitação de inscrição para corridaId={} (usuarioId={})", raceId, user != null ? user.getId() : null);
        Inscricao inscricao = inscricaoService.inscrever(user, raceId, riskAcknowledged);
        log.info("[InscricaoController] Inscrição realizada com sucesso: id={}", inscricao.getId());
        return "redirect:/corridas/" + inscricao.getCorrida().getSlug();
    }

    @PostMapping("/inscricoes/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal User user, @PathVariable("id") Long inscricaoId) {
        log.info("[InscricaoController] Recebida solicitação de cancelamento da inscriçãoId={} (usuarioId={})", inscricaoId, user != null ? user.getId() : null);
        inscricaoService.cancelar(user, inscricaoId);
        log.info("[InscricaoController] Inscrição cancelada com sucesso: id={}", inscricaoId);
        return "redirect:/minhas-inscricoes";
    }

}
