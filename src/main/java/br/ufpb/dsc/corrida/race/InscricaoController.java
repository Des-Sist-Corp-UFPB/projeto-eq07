package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InscricaoController {

    @Autowired
    private InscricaoService inscricaoService;

    @PostMapping("/corridas/{id}/inscrever")
    public String inscrever(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long raceId,
            @RequestParam(value = "riskAcknowledged", required = false, defaultValue = "false") boolean riskAcknowledged) {
        Inscricao inscricao = inscricaoService.inscrever(user, raceId, riskAcknowledged);
        return "redirect:/corridas/" + inscricao.getCorrida().getSlug();
    }


    @PostMapping("/inscricoes/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal User user, @PathVariable("id") Long inscricaoId) {
        inscricaoService.cancelar(user, inscricaoId);
        return "redirect:/minhas-inscricoes";
    }

}
