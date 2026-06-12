package br.ufpb.dsc.corrida.user;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;

import br.ufpb.dsc.corrida.exception.userinfo.UserInfoNaoEncontradoException;
import br.ufpb.dsc.corrida.user.dto.PerfilPublicoDTO;
import br.ufpb.dsc.corrida.user.dto.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.user.dto.UserInfoRespostaDTO;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class UsuarioViewController {

    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private UsuarioService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserConnectionRepository userConnectionRepository;

    @Autowired
    private UserConnectionService userConnectionService;
    
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
    public ModelAndView getProfilePage(@PathVariable String username, 
                                    @AuthenticationPrincipal User loggedInUser, 
                                    HttpServletRequest request) {
        User profileUser = userRepository.findByUsername(username);
        if (profileUser == null) {
            throw new br.ufpb.dsc.corrida.exception.user.UsuarioNaoEncontradoException("Usuário não encontrado");
        }

        PerfilPublicoDTO perfil = service.buscarPerfilPublico(username);
        ModelAndView mv = new ModelAndView("perfil-publico");
        mv.addObject("perfil", perfil);
        mv.addObject("perfilId", profileUser.getId());

        // Adiciona o CSRF token — necessário para o template
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            mv.addObject("_csrf", csrfToken);
        }

        if (loggedInUser != null) {

            long countConections = userConnectionRepository.countConnectionsByUserId(profileUser.getId());
            mv.addObject("countConections", countConections);

            Optional<UserConnection> conexaoOpt = userConnectionRepository
                .findConnectionBetweenUsers(loggedInUser.getId(), profileUser.getId());
            conexaoOpt.ifPresent(conexao -> mv.addObject("conexao", conexao));

            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                mv.addObject("token", token.substring(7));
            }
        }

        return mv;
    }

    @GetMapping("/solicitacoes")
    public String exibirSolicitacoes(@AuthenticationPrincipal User loggedInUser, Model model, HttpServletRequest request) {
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        model.addAttribute("solicitacoes", userConnectionService.getPendingRequestsList(loggedInUser.getId()));
        return "user/solicitacoes";
    }
}
