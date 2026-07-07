package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InscricaoViewController {

    private final InscricaoRepository inscricaoRepository;

    public InscricaoViewController(InscricaoRepository inscricaoRepository) {
        this.inscricaoRepository = inscricaoRepository;
    }

    @GetMapping("/minhas-inscricoes")
    public String minhasInscricoes(@AuthenticationPrincipal User user, Model model) {
        List<Inscricao> inscricoes = inscricaoRepository.findByUsuarioAndStatus(user, StatusInscricao.ATIVA);
        model.addAttribute("inscricoes", inscricoes);
        return "user/minhas-inscricoes";
    }
}
