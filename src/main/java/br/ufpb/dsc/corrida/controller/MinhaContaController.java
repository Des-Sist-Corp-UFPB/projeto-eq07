package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.domain.Usuario;
import br.ufpb.dsc.corrida.dto.userinfo.UserInfoRespostaDTO;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.service.userinfo.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller responsável por renderizar a página "Minha Conta".
 *
 * <p>Carrega as informações do corredor autenticado e as disponibiliza no modelo
 * para o template Thymeleaf. Se o corredor ainda não preencheu seu perfil,
 * o atributo {@code userInfo} não será definido no modelo, ativando o banner
 * de alerta no template.</p>
 */
@Controller
@RequestMapping("/minha-conta")
public class MinhaContaController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * Renderiza a página de perfil do corredor autenticado.
     *
     * @param usuario usuário atualmente autenticado (injetado pelo Spring Security)
     * @param model   modelo Thymeleaf
     * @return nome do template a renderizar
     */
    @GetMapping
    public String minhaConta(@AuthenticationPrincipal Usuario usuario, Model model) {
        if (usuario != null) {
            try {
                UserInfoRespostaDTO userInfo = userInfoService.buscarPorUsuarioId(usuario.getId());
                model.addAttribute("userInfo", userInfo);
                model.addAttribute("usuarioId", usuario.getId());
            } catch (UserInfoNaoEncontradoException e) {
                // Perfil ainda não preenchido — o template exibirá o banner de alerta
                model.addAttribute("usuarioId", usuario.getId());
                model.addAttribute("profileMissing", true);
            }
        }
        return "minha-conta";
    }
}
