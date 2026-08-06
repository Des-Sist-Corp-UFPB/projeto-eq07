package br.ufpb.dsc.corrida.featuretoggle;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API pública que expõe o estado dos feature flags relevantes para o frontend.
 *
 * <p>Endpoint: {@code GET /api/v1/features}
 *
 * <p>O frontend deve consumir este endpoint ao carregar formulários ou fluxos
 * condicionais (ex: desabilitar campo de preço quando {@code PAYMENT_V2} estiver inativo).
 *
 * <p>Nota: este endpoint é público (não requer autenticação) para que o frontend
 * possa consultá-lo antes do login. Não exponha flags sensíveis de infraestrutura aqui.
 */
@RestController
@RequestMapping("/api/v1/features")
public class FeatureConfigController {

    private final FeatureToggleService featureToggleService;

    public FeatureConfigController(FeatureToggleService featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    /**
     * Retorna o estado dos feature flags conhecidos pelo frontend.
     *
     * <p>Exemplo de resposta:
     * <pre>{@code
     * {
     *   "PAYMENT_V2": false,
     *   "SEARCH_RACES": true,
     *   "CREATE_RACE": true
     * }
     * }</pre>
     *
     * @return mapa {@code flagKey -> enabled}
     */
    @GetMapping
    public ResponseEntity<Map<String, Boolean>> listarFlags() {
        Map<String, Boolean> flags = Map.of(
                "PAYMENT_V2",   featureToggleService.isFeatureEnabled("PAYMENT_V2"),
                "SEARCH_RACES", featureToggleService.isFeatureEnabled("SEARCH_RACES"),
                "CREATE_RACE",  featureToggleService.isFeatureEnabled("CREATE_RACE")
        );
        return ResponseEntity.ok(flags);
    }
}
