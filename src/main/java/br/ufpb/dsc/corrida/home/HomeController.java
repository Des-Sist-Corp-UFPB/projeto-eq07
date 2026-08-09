package br.ufpb.dsc.corrida.home;

import br.ufpb.dsc.corrida.race.CorridaService;
import br.ufpb.dsc.corrida.user.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller da página inicial.
 *
 * <p>Popula o model com as duas seções dinâmicas:
 * <ul>
 *   <li>{@code proximasCorridas} — top-6 corridas publicadas com data futura, ordenadas
 *       por {@code dataInicio} ascendente (query no repositório, sem filtragem em memória).</li>
 *   <li>{@code usuariosRecentes} — top-6 usuários cadastrados mais recentemente,
 *       ordenados por {@code id} decrescente (proxy de data de cadastro).</li>
 * </ul>
 * </p>
 */
@Controller
public class HomeController {

    @Autowired
    private CorridaService corridaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("proximasCorridas", corridaService.listarProximasCorridas());
        model.addAttribute("usuariosRecentes", usuarioService.listarUsuariosRecentes());
        return "index";
    }
}
