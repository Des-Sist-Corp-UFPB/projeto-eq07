package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.user.AcessoNaoPermitidoException;
import br.ufpb.dsc.corrida.organizer.Organization;
import br.ufpb.dsc.corrida.organizer.OrganizerService;
import br.ufpb.dsc.corrida.race.dto.CriarCorridaDTO;
import br.ufpb.dsc.corrida.race.dto.EditarCorridaDTO;
import br.ufpb.dsc.corrida.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.List;

@Controller
public class CorridaViewController {

    @Autowired
    private CorridaService service;

    @Autowired
    private OrganizerService organizerService;

    // =========================================================================
    // Feed Público
    // =========================================================================

    @GetMapping("/corridas")
    public String listarCorridasPublicas(Model model) {
        List<Race> corridas = service.listarCorridasPublicas();
        model.addAttribute("corridas", corridas);
        model.addAttribute("titulo", "Próximas Corridas");
        model.addAttribute("isHistorico", false);
        model.addAttribute("activePage", "corridas");
        return "corrida/corridas-lista";
    }

    @GetMapping("/corridas/encerradas")
    public String listarHistorico(Model model) {
        List<Race> corridas = service.listarHistorico();
        model.addAttribute("corridas", corridas);
        model.addAttribute("titulo", "Histórico de Corridas");
        model.addAttribute("isHistorico", true);
        model.addAttribute("activePage", "corridas");
        return "corrida/corridas-lista";
    }

    @GetMapping("/corridas/{slug}")
    public String exibirDetalhes(@PathVariable String slug, Model model) {
        Race race = service.buscarPorSlug(slug);
        model.addAttribute("corrida", race);
        model.addAttribute("activePage", "corridas");
        return "corrida/corrida-detalhes";
    }

    // =========================================================================
    // Gestão do Organizador
    // =========================================================================

    @GetMapping("/organizacao/{orgId}/corridas")
    public String gerenciarCorridas(
            @PathVariable Long orgId,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {
        verificarPropriedade(orgId, usuarioLogado);

        List<Race> corridas = service.listarPorOrganizacao(orgId);
        model.addAttribute("corridas", corridas);
        model.addAttribute("orgId", orgId);
        model.addAttribute("activePage", "minhas-corridas");
        return "corrida/corridas-gerenciar";
    }

    @GetMapping("/organizacao/{orgId}/corridas/nova")
    public String exibirFormularioCriacao(
            @PathVariable Long orgId,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {
        verificarPropriedade(orgId, usuarioLogado);

        CriarCorridaDTO emptyForm = new CriarCorridaDTO(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        model.addAttribute("corrida", emptyForm);
        model.addAttribute("orgId", orgId);
        model.addAttribute("categorias", Arrays.asList(CategoriaCorrida.values()));
        model.addAttribute("beneficios", Arrays.asList(BeneficioCorrida.values()));
        model.addAttribute("isEdicao", false);
        model.addAttribute("activePage", "minhas-corridas");
        return "corrida/corrida-form";
    }

    @GetMapping("/organizacao/{orgId}/corridas/{id}/editar")
    public String exibirFormularioEdicao(
            @PathVariable Long orgId,
            @PathVariable Long id,
            @AuthenticationPrincipal User usuarioLogado,
            Model model) {
        verificarPropriedade(orgId, usuarioLogado);

        Race race = service.buscarPorId(id);
        if (!race.getOrganization().getId().equals(orgId)) {
            throw new AcessoNaoPermitidoException("Esta corrida não pertence à organização especificada.");
        }

        EditarCorridaDTO form = new EditarCorridaDTO(
                race.getNome(),
                race.getDescricao(),
                race.getBannerUrl(),
                race.getValorInscricao(),
                race.getMaxInscricoes(),
                race.getDataInicio(),
                race.getCategoria(),
                race.getLargadaLat(),
                race.getLargadaLng(),
                race.getLargadaEndereco(),
                race.getChegadaLat(),
                race.getChegadaLng(),
                race.getChegadaEndereco(),
                race.getBeneficios()
        );

        model.addAttribute("corrida", form);
        model.addAttribute("orgId", orgId);
        model.addAttribute("raceId", id);
        model.addAttribute("categorias", Arrays.asList(CategoriaCorrida.values()));
        model.addAttribute("beneficios", Arrays.asList(BeneficioCorrida.values()));
        model.addAttribute("isEdicao", true);
        model.addAttribute("activePage", "minhas-corridas");
        return "corrida/corrida-form";
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void verificarPropriedade(Long orgId, User loggedInUser) {
        if (loggedInUser == null) {
            throw new AcessoNaoPermitidoException("Acesso negado: Usuário não autenticado");
        }
        Organization org = organizerService.buscarOrganizacaoPorId(orgId);
        boolean eDono = organizerService.buscarOrganizadorPorUsuarioId(loggedInUser.getId())
                .map(organizer -> organizer.getId().equals(org.getOrganizer().getId()))
                .orElse(false);
        if (!eDono) {
            throw new AcessoNaoPermitidoException("Acesso negado: Você não é o proprietário desta organização");
        }
    }
}
