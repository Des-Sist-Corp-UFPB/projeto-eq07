package br.ufpb.dsc.corrida.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;
import br.ufpb.dsc.corrida.user.dto.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;

@Controller
public class UsuarioViewController {

    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private UsuarioService service;
    
    @GetMapping("/registrar")
    public String exibirFormularioRegistro(Model model) {
        model.addAttribute("usuario", new RegistrarUsuarioDTO("", "", "", ""));
        return "auth/registrar";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/minhaConta")
    public String minhaConta(@AuthenticationPrincipal User usuario, Model model) {
        System.out.println(usuario);
        if (usuario != null) {
            try {
                UserInfoRespostaDTO userInfo = userInfoService.buscarPorUsuarioId(usuario.getId());
                model.addAttribute("userInfo", userInfo);
                model.addAttribute("usuarioId", usuario.getId());
            } catch (UserInfoNaoEncontradoException e) {
                model.addAttribute("usuarioId", usuario.getId());
                model.addAttribute("profileMissing", true);
            }
        }
        return "minha-conta";
    }

    @GetMapping("/user/{username}/profile")
    public ModelAndView getProfilePage(@PathVariable String username) {
        PerfilPublicoDTO perfil = service.buscarPerfilPublico(username);
        ModelAndView mv = new ModelAndView("perfil-publico");
        mv.addObject("perfil", perfil);
        return mv;
    }
}
