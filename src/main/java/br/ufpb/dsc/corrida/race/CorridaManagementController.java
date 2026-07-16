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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Autowired
    private InscricaoService inscricaoService;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private RaceRepository raceRepository;

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

    @PostMapping("/organizacao/{orgId}/corridas/{id}/publicar")
    public String publicarCorrida(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @AuthenticationPrincipal User usuarioLogado) {

        service.publicarCorrida(id, usuarioLogado);
        return "redirect:/organizacao/" + orgId + "/corridas";
    }

    @GetMapping("/organizacao/{orgId}/corridas/{id}/participantes")
    public String listarParticipantes(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {
        
        Race race = service.buscarPorId(id);
        if (!race.getOrganization().getId().equals(orgId)) {
            throw new br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException("Acesso negado");
        }

        Pageable pageable = PageRequest.of(page, 50);
        Page<Inscricao> inscricoes = inscricaoRepository.findByCorridaId(race.getId(), pageable);
        
        System.out.println("TOTAL ELEMENTOS: " + inscricoes.getTotalElements());
        System.out.println("TOTAL PAGES: " + inscricoes.getTotalPages());
        System.out.println("CONTENT SIZE: " + inscricoes.getContent().size());

        model.addAttribute("inscricoes", inscricoes);
        model.addAttribute("race", race);
        model.addAttribute("orgId", orgId);
        return "corrida/corrida-participantes";
    }

    @org.springframework.web.bind.annotation.PatchMapping("/organizacao/{orgId}/inscricoes/{inscricaoId}/check-in")
    public String checkinHTMX(
            @PathVariable Long orgId,
            @PathVariable Long inscricaoId,
            @RequestParam boolean presente,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {
        
        inscricaoService.marcarPresenca(usuarioLogado, inscricaoId, presente);
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElseThrow();
        model.addAttribute("inscricao", inscricao);
        model.addAttribute("orgId", orgId);
        return "corrida/fragments/inscricao-tr :: inscricaoRow";
    }

    @PostMapping("/organizacao/{orgId}/corridas/{id}/encerrar")
    public String encerrarCorrida(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @AuthenticationPrincipal User usuarioLogado) {
        
        Race race = service.buscarPorId(id);
        if (!race.getOrganization().getId().equals(orgId)) {
            throw new br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException("Acesso negado");
        }

        // Muda status (poderia ter um método no CorridaService, fazemos aqui por simplicidade ou invocamos service)
        race.setStatus(StatusCorrida.ENCERRADA);
        raceRepository.save(race);
        inscricaoService.processarEncerramentoCorrida(race);
        return "redirect:/organizacao/" + orgId + "/corridas";
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void populateFormModel(Model model, Long orgId, boolean isEdicao) {
        model.addAttribute("orgId", orgId);
        model.addAttribute("categorias", Arrays.asList(CategoriaCorrida.values()));
        model.addAttribute("beneficios", Arrays.asList(BeneficioCorrida.values()));
        model.addAttribute("terrenos", Arrays.asList(Terreno.values()));
        model.addAttribute("climas", Arrays.asList(ClimaEsperado.values()));
        model.addAttribute("niveis", Arrays.asList(NivelDificuldade.values()));
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
