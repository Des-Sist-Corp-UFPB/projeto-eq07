package br.ufpb.dsc.corrida.inscricao;

import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import br.ufpb.dsc.corrida.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API REST para listagem de inscritos de uma corrida.
 *
 * <p>Endpoint: {@code GET /api/v1/corridas/{id}/inscritos}
 *
 * <h3>Autorização</h3>
 * <ul>
 *   <li>Requer autenticação (filtrado via SecurityConfig — {@code .anyRequest().authenticated()}).</li>
 *   <li>Verifica no servidor que o usuário autenticado é o organizador da corrida.
 *       Não-organizadores recebem {@code 403 Forbidden}.</li>
 * </ul>
 *
 * <h3>Resposta</h3>
 * Lista de {@link InscritoDTO} com nome, username, data de inscrição e status.
 * Apenas inscrições ativas (AGUARDANDO_PAGAMENTO, ATIVA, CONFIRMADA) são retornadas.
 */
@RestController
@RequestMapping("/api/v1/corridas")
public class InscritosController {

    private static final Logger log = LoggerFactory.getLogger(InscritosController.class);

    private final RaceRepository raceRepository;
    private final InscricaoRepository inscricaoRepository;

    public InscritosController(RaceRepository raceRepository,
                               InscricaoRepository inscricaoRepository) {
        this.raceRepository = raceRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    /**
     * Lista os inscritos de uma corrida.
     *
     * @param id            ID da corrida
     * @param usuarioLogado usuário autenticado (injetado pelo Spring Security)
     * @return lista de inscritos ou 403 se o requester não for o organizador
     */
    @GetMapping("/{id}/inscritos")
    public ResponseEntity<List<InscritoDTO>> listarInscritos(
            @PathVariable Long id,
            @AuthenticationPrincipal User usuarioLogado) {

        Race race = raceRepository.findById(id)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada: " + id));

        // Verificação de propriedade: somente o organizador da corrida pode ver os inscritos
        Long organizadorUserId;
        try {
            organizadorUserId = race.getOrganization()
                    .getOrganizer()
                    .getUsuario()
                    .getId();
        } catch (Exception e) {
            log.warn("[InscritosController] Corrida id={} sem organização/organizador configurado.", id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso negado: organização da corrida não configurada.");
        }

        if (usuarioLogado == null || !usuarioLogado.getId().equals(organizadorUserId)) {
            log.warn("[InscritosController] Acesso negado: usuárioId={} tentou acessar inscritos de corridaId={}",
                    usuarioLogado != null ? usuarioLogado.getId() : "anon", id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso negado: apenas o organizador da corrida pode visualizar os inscritos.");
        }

        // Busca todas as inscrições da corrida (inclui todos os status)
        List<InscritoDTO> inscritos = inscricaoRepository.findByCorridaId(id,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(InscritoDTO::from)
                .toList();

        log.info("[InscritosController] corridaId={} — {} inscritos retornados para organizadorId={}",
                id, inscritos.size(), organizadorUserId);

        return ResponseEntity.ok(inscritos);
    }
}
