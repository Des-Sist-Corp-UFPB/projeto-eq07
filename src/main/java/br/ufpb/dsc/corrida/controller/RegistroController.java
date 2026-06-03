package br.ufpb.dsc.corrida.controller;

import br.ufpb.dsc.corrida.dto.user.RegistrarUsuarioDTO;
import br.ufpb.dsc.corrida.exception.user.UsuarioJaExistenteException;
import br.ufpb.dsc.corrida.service.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller responsável pela exibição e processamento do formulário de registro de novos usuários.
 */
@Controller
public class RegistroController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/registrar")
    public String exibirFormularioRegistro(Model model) {
        model.addAttribute("usuario", new RegistrarUsuarioDTO("", "", "", ""));
        return "auth/registrar";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(
            @ModelAttribute("usuario") @Valid RegistrarUsuarioDTO usuarioDTO,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/registrar";
        }

        try {
            usuarioService.registrar(usuarioDTO);
            return "redirect:/login?success=true";
        } catch (UsuarioJaExistenteException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/registrar";
        } catch (Exception e) {
            model.addAttribute("error", "Ocorreu um erro inesperado. Tente novamente.");
            return "auth/registrar";
        }
    }
}
