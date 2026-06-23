package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.ExternalServiceException;
import br.ufpb.dsc.corrida.race.dto.CriarCorridaDTO;
import br.ufpb.dsc.corrida.race.dto.EditarCorridaDTO;
import br.ufpb.dsc.corrida.user.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Controller
public class CorridaManagementController {

    @Autowired
    private CorridaService service;

    @PostMapping("/organizacao/{orgId}/corridas")
    public String criarCorrida(
            @PathVariable Long orgId,
            @ModelAttribute("corrida") @Valid CriarCorridaDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateFormModel(model, orgId, false);
            model.addAttribute("corrida", dto);
            return "corrida/corrida-form";
        }

        try {
            service.criarCorrida(dto, orgId, usuarioLogado);
            return "redirect:/organizacao/" + orgId + "/corridas";
        } catch (ExternalServiceException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model, orgId, false);
            model.addAttribute("corrida", dto);
            return "corrida/corrida-form";
        }
    }

    @PostMapping("/organizacao/{orgId}/corridas/{id}/editar")
    public String editarCorrida(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @ModelAttribute("corrida") @Valid EditarCorridaDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {

        if (bindingResult.hasErrors()) {
            populateFormModel(model, orgId, true);
            model.addAttribute("raceId", id);
            return "corrida/corrida-form";
        }

        try {
            service.editarCorrida(id, dto, usuarioLogado);
            return "redirect:/organizacao/" + orgId + "/corridas";
        } catch (ExternalServiceException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateFormModel(model, orgId, true);
            model.addAttribute("raceId", id);
            return "corrida/corrida-form";
        }
    }

    @PostMapping("/organizacao/{orgId}/corridas/{id}/cancelar")
    public String cancelarCorrida(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @AuthenticationPrincipal User usuarioLogado) {

        service.cancelarCorrida(id, usuarioLogado);
        return "redirect:/organizacao/" + orgId + "/corridas";
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void populateFormModel(Model model, Long orgId, boolean isEdicao) {
        model.addAttribute("orgId", orgId);
        model.addAttribute("categorias", Arrays.asList(CategoriaCorrida.values()));
        model.addAttribute("beneficios", Arrays.asList(BeneficioCorrida.values()));
        model.addAttribute("isEdicao", isEdicao);
    }

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(OffsetDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                setValue(LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        .atZone(ZoneId.of("America/Recife"))
                        .toOffsetDateTime());
            }
        });
    }
}
