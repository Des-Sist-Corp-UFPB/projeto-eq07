package br.ufpb.dsc.corrida.eligibility;

import br.ufpb.dsc.corrida.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API REST para checagem de elegibilidade de risco antes da inscrição em corridas.
 *
 * <p>Expõe {@code POST /api/races/{raceId}/eligibility-check}, consumido assincronamente
 * via {@code fetch} no front-end após o carregamento da página de detalhes da corrida.
 *
 * <p>O resultado é informativo — nunca deve bloquear o fluxo de inscrição. Se o serviço
 * falhar, o front-end trata como {@code apto: true} e a inscrição prossegue normalmente.
 */
@RestController
@RequestMapping("/api/races")
public class EligibilityApiController {

    private final EligibilityService eligibilityService;

    public EligibilityApiController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /**
     * Executa a análise de risco para o par (usuário autenticado, corrida).
     *
     * @param raceId ID da corrida
     * @param user   usuário autenticado pelo Spring Security
     * @return JSON {@code {"apto": boolean, "resposta": String|null}}
     */
    @PostMapping("/{raceId}/eligibility-check")
    public ResponseEntity<Map<String, Object>> check(
            @PathVariable Long raceId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            // Usuário não autenticado — retorna apto para não bloquear visitantes
            return ResponseEntity.ok(Map.of("apto", true));
        }

        

        EligibilityResult result = eligibilityService.check(user.getId(), raceId);
        EligibilityResponse resp = result.response();

        Map<String, Object> body = resp.apto()
                ? Map.of("apto", true)
                : Map.of("apto", false, "resposta", resp.resposta() != null ? resp.resposta() : "");

        return ResponseEntity.ok(body);
    }
}
