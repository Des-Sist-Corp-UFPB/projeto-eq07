package br.ufpb.dsc.corrida.organizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorCompletoDTO;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorStep1DTO;
import br.ufpb.dsc.corrida.organizer.dto.RegistrarOrganizadorStep2DTO;

@Controller
public class OrganizerViewController {

    @Autowired
    private OrganizerService organizerService;

    @GetMapping("/registrar/organizador")
    public String exibirFormularioRegistroOrganizador(Model model) {
        var step1 = new RegistrarOrganizadorStep1DTO("", "", "", "", "", "", "", "", "");
        var step2 = new RegistrarOrganizadorStep2DTO("", null, "", "", "", "", "");
        var completo = new RegistrarOrganizadorCompletoDTO(step1, step2);
        model.addAttribute("organizadorStep1", step1);
        model.addAttribute("organizadorStep2", step2);
        model.addAttribute("organizadorCompleto", completo);
        return "auth/registrar-organizador";
    }
    
    @GetMapping("/organizacao/{id}")
    public String exibirDetalhesOrganizacao(@PathVariable Long id, Model model) {
        Organization organizacao = organizerService.buscarOrganizacaoPorId(id);
        model.addAttribute("organizacao", organizacao);
        return "user/organizacao-detalhes";
    }

    @PostMapping("/registrar/organizador")
    public String registrarOrganizador(
            @ModelAttribute("organizadorCompleto") @jakarta.validation.Valid RegistrarOrganizadorCompletoDTO completoDTO,
            BindingResult bindingResult,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("organizadorStep1", completoDTO.step1());
            model.addAttribute("organizadorStep2", completoDTO.step2());
            return "auth/registrar-organizador";
        }

        try {
            organizerService.registrarOrganizador(completoDTO);
            return "redirect:/login?success=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("organizadorStep1", completoDTO.step1());
            model.addAttribute("organizadorStep2", completoDTO.step2());
            return "auth/registrar-organizador";
        } catch (Exception e) {
            model.addAttribute("error", "Ocorreu um erro inesperado. Tente novamente.");
            model.addAttribute("organizadorStep1", completoDTO.step1());
            model.addAttribute("organizadorStep2", completoDTO.step2());
            return "auth/registrar-organizador";
        }
    }
}
