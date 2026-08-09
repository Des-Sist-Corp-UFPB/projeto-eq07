package br.ufpb.dsc.corrida.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de extensão para métricas customizadas do OpenTelemetry (OTel).
 *
 * <p>Esta classe obtém o {@link Meter} global registrado pelo Java Agent do OTel
 * (ou SDK local) e disponibiliza beans de métricas para a aplicação.
 *
 * <h3>Como usar em outros serviços/controllers:</h3>
 * <pre>{@code
 * @Autowired
 * private LongCounter inscricoesCounter;
 *
 * public void realizarInscricao(...) {
 *     // ... lógica de negócio ...
 *     inscricoesCounter.add(1);
 * }
 * }</pre>
 */
@Configuration
public class MetricsConfig {

    private static final String INSTRUMENTATION_SCOPE_NAME = "br.ufpb.dsc.corrida";

    @Bean
    public Meter openTelemetryMeter() {
        return GlobalOpenTelemetry.getMeter(INSTRUMENTATION_SCOPE_NAME);
    }

    /**
     * Exemplo de contador de métrica customizada para inscrições realizadas.
     */
    @Bean
    public LongCounter inscricoesCounter(Meter meter) {
        return meter.counterBuilder("corrida.inscricoes.total")
                .setDescription("Número total de inscrições realizadas nas corridas")
                .setUnit("1")
                .build();
    }

    /**
     * Exemplo de contador de métrica customizada para corridas criadas.
     */
    @Bean
    public LongCounter corridasCriadasCounter(Meter meter) {
        return meter.counterBuilder("corrida.criadas.total")
                .setDescription("Número total de corridas criadas por organizadores")
                .setUnit("1")
                .build();
    }
}
