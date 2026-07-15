package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InscricaoController {

    @Autowired
    private InscricaoService inscricaoService;

    @PostMapping("/corridas/{id}/inscrever")
    public String inscrever(@AuthenticationPrincipal User user, @PathVariable("id") Long raceId) {
        Inscricao inscricao = inscricaoService.inscrever(user, raceId);
        // Retorna para a página da corrida, com HTMX ou redirecionamento padrão
        return "redirect:/corridas/" + inscricao.getCorrida().getSlug(); 
        // OBS: Idealmente deve pegar o slug. O redirecionamento pode ser pro painel "Minhas Inscrições"
        // Retornando redirect direto para a aba de inscricoes:
        // return "redirect:/minhas-inscricoes";
    }


    @PostMapping("/inscricoes/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal User user, @PathVariable("id") Long inscricaoId) {
        inscricaoService.cancelar(user, inscricaoId);
        return "redirect:/minhas-inscricoes";
    }

}
