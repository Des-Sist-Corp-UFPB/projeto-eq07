package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InscricaoViewController {

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @GetMapping("/minhas-inscricoes")
    public String minhasInscricoes(@AuthenticationPrincipal User user, Model model) {
        List<Inscricao> inscricoes = inscricaoRepository.findByUsuarioAndStatusInOrderByIdDesc(
                user, List.of(StatusInscricao.AGUARDANDO_PAGAMENTO, StatusInscricao.CONFIRMADA, StatusInscricao.ATIVA)
        );
        model.addAttribute("inscricoes", inscricoes);
        return "user/minhas-inscricoes";
    }
}
