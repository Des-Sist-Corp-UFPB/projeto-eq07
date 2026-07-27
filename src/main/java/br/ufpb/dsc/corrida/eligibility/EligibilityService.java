package br.ufpb.dsc.corrida.eligibility;

import br.ufpb.dsc.corrida.audit.Auditable;
import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import br.ufpb.dsc.corrida.user.UserInfo;
import br.ufpb.dsc.corrida.user.UserInfoRepository;
import br.ufpb.dsc.corrida.exception.CorridaNaoEncontradaException;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço que orquestra a análise de risco de elegibilidade para inscrição em corridas.
 *
 * <h2>Fluxo de execução</h2>
 * <ol>
 *   <li>Verifica se o usuário concedeu consentimento para processamento de dados de saúde.</li>
 *   <li>Aplica rate limiting (máx. 5 checagens/usuário/minuto).</li>
 *   <li>Monta o contexto do atleta e da corrida, sanitizando texto livre.</li>
 *   <li>Consulta o cache {@code eligibilityChecks} antes de chamar a LLM.</li>
 *   <li>Em caso de timeout, erro ou resposta inválida, retorna fallback seguro ({@code apto: true}).</li>
 * </ol>
 *
 * <h2>Auditoria</h2>
 * Cada checagem é registrada no logger {@code ELIGIBILITY_AUDIT} com os campos:
 * userId, raceId, timestamp, prompt enviado, resposta bruta, resultado final e {@link EligibilitySource source}.
 *
 * <p><strong>ATENÇÃO:</strong> a saída do logger {@code ELIGIBILITY_AUDIT} contém dados sensíveis
 * de saúde e deve ser direcionada, em produção, a um destino com acesso restrito e política
 * de retenção definida — não deve ser enviada para logs de acesso geral.
 */
@Service
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    /** Logger dedicado para auditoria de checagens de elegibilidade. Acesso restrito em produção. */
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("ELIGIBILITY_AUDIT");

    private static final int MAX_MEDICAL_NOTES_LENGTH = 500;
    private static final int RATE_LIMIT_PER_MINUTE = 5;

    private final LiteLlmClient llmClient;
    private final RaceRepository raceRepository;
    private final UserInfoRepository userInfoRepository;

    /** Bucket simples em memória para rate limiting por userId. */
    private final Map<Long, RateBucket> rateBuckets = new ConcurrentHashMap<>();

    @Autowired
    public EligibilityService(LiteLlmClient llmClient,
                              RaceRepository raceRepository,
                              UserInfoRepository userInfoRepository) {
        this.llmClient = llmClient;
        this.raceRepository = raceRepository;
        this.userInfoRepository = userInfoRepository;
    }

    /**
     * Avalia o risco de elegibilidade de um usuário para uma corrida.
     *
     * <p>O resultado é cacheado por {@code (userId, raceId, hashPerfil)} — portanto, mudanças
     * no perfil de saúde do usuário invalidam naturalmente o cache na próxima checagem
     * (via chave diferente).
     *
     * @param userId ID do usuário autenticado
     * @param raceId ID da corrida
     * @return resultado da análise com campo {@code source} indicando a origem
     */
    @WithSpan("eligibility.check-risk")
    @Cacheable(value = "eligibilityChecks", key = "#userId + '-' + #raceId + '-' + T(br.ufpb.dsc.corrida.eligibility.EligibilityService).profileHash(#userId, @userInfoRepository)")
    @Auditable(action = "ELIGIBILITY_CHECK", resource = "ELIGIBILITY", idParam = "raceId")
    public EligibilityResult check(@SpanAttribute("user.id") Long userId, @SpanAttribute("race.id") Long raceId) {
        log.info("[EligibilityService] Iniciando checagem de elegibilidade para usuarioId={} e corridaId={}", userId, raceId);

        // ── 1. Buscar dados ────────────────────────────────────────────────────
        Optional<UserInfo> userInfoOpt = userInfoRepository.findByUsuarioId(userId);
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new CorridaNaoEncontradaException("Corrida não encontrada: " + raceId));

        // ── 2. Verificar consentimento ─────────────────────────────────────────
        if (userInfoOpt.isEmpty() || !Boolean.TRUE.equals(userInfoOpt.get().getConsentimentoSaude())) {
            log.info("[EligibilityService] Checagem abortada: usuarioId={} nao concedeu consentimento para processamento de dados de saude", userId);
            AUDIT_LOG.info("userId={} raceId={} timestamp={} source={} result=apto:true",
                    userId, raceId, Instant.now(), EligibilitySource.NO_CONSENT);
            return EligibilityResult.of(EligibilityResponse.fallback(), EligibilitySource.NO_CONSENT);
        }

        UserInfo userInfo = userInfoOpt.get();

        // ── 3. Rate limiting ───────────────────────────────────────────────────
        if (isRateLimited(userId)) {
            log.warn("[EligibilityService] Checagem abortada: usuarioId={} atingiu o limite de requisicoes (rate limit)", userId);
            AUDIT_LOG.warn("userId={} raceId={} timestamp={} source={} result=apto:true (rate limited)",
                    userId, raceId, Instant.now(), EligibilitySource.RATE_LIMITED);
            return EligibilityResult.of(EligibilityResponse.fallback(), EligibilitySource.RATE_LIMITED);
        }

        // ── 4. Montar contextos ────────────────────────────────────────────────
        String userContext = buildUserContext(userInfo);
        String raceContext = buildRaceContext(race);

        // ── 5. Chamar LLM ──────────────────────────────────────────────────────
        Instant timestamp = Instant.now();
        String rawResponse = null;
        try {
            log.info("[EligibilityService] Chamando a LLM para fazer a avaliacao de risco do atleta...");
            EligibilityResponse response = llmClient.check(userContext, raceContext);
            rawResponse = response.toString();

            log.info("[EligibilityService] LLM retornou com sucesso. Apto: {}", response.apto());
            AUDIT_LOG.info("userId={} raceId={} timestamp={} source={} apto={} prompt=[{}|{}] raw={}",
                    userId, raceId, timestamp, EligibilitySource.LLM_ASSESSED,
                    response.apto(), userContext, raceContext, rawResponse);

            return EligibilityResult.of(response, EligibilitySource.LLM_ASSESSED);

        } catch (LlmUnavailableException e) {
            EligibilitySource source = e.getMessage().contains("timeout")
                    ? EligibilitySource.LLM_TIMEOUT
                    : EligibilitySource.LLM_ERROR;

            log.error("[EligibilityService] Erro ou timeout na chamada da LLM. Aplicando fallback de seguranca (apto: true).", e);
            AUDIT_LOG.error("userId={} raceId={} timestamp={} source={} error={} result=apto:true (fallback)",
                    userId, raceId, timestamp, source, e.getMessage());

            return EligibilityResult.of(EligibilityResponse.fallback(), source);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String buildUserContext(UserInfo info) {
        int age = Period.between(info.getDataNasc(), LocalDate.now()).getYears();
        String nivel = info.getNivelCondicionamento() != null
                ? info.getNivelCondicionamento().name() : "Não informado";
        String notas = sanitizeMedicalNotes(info.getNotasMedicas());
        return String.format(
                "Idade: %d anos | Gênero: %s | Nível de condicionamento: %s | "
                + "KM totais percorridos: %.1f km | Observações médicas: %s",
                age, info.getGenero().name(), nivel, info.getTotalKmRun(), notas);
    }

    private String buildRaceContext(Race race) {
        return String.format(
                "Categoria: %s | Distância: %s km | Duração estimada: %s min | "
                + "Terreno: %s | Clima esperado: %s | Nível de dificuldade: %s | "
                + "Ganho de elevação: %s m",
                race.getCategoria().getLabel(),
                race.getDistanciaKm() != null ? race.getDistanciaKm().toPlainString() : "não informada",
                race.getDuracaoEstimadaMin() != null ? race.getDuracaoEstimadaMin() : "não informado",
                race.getTerreno() != null ? race.getTerreno().getLabel() : "não informado",
                race.getClimaEsperado() != null ? race.getClimaEsperado().getLabel() : "não informado",
                race.getNivelDificuldade() != null ? race.getNivelDificuldade().getLabel() : "não informado",
                race.getGanhoElevacao() != null ? race.getGanhoElevacao() : "não informado");
    }

    /**
     * Sanitiza o texto livre de observações médicas para reduzir risco de prompt injection.
     * Remove padrões de instrução e limita o tamanho.
     */
    private String sanitizeMedicalNotes(String notes) {
        if (notes == null || notes.isBlank()) return "Nenhuma";
        String cleaned = notes
                .replaceAll("(?i)(ignore\\s+(previous|all|above|prior)\\s+instruction)", "[REMOVIDO]")
                .replaceAll("(?i)(you\\s+are\\s+now)", "[REMOVIDO]")
                .replaceAll("(?i)(act\\s+as)", "[REMOVIDO]")
                .replaceAll("(?i)(system\\s*:)", "[REMOVIDO]")
                .replaceAll("(?i)(assistant\\s*:)", "[REMOVIDO]")
                .strip();
        if (cleaned.length() > MAX_MEDICAL_NOTES_LENGTH) {
            cleaned = cleaned.substring(0, MAX_MEDICAL_NOTES_LENGTH) + "...";
        }
        return cleaned;
    }

    private boolean isRateLimited(Long userId) {
        RateBucket bucket = rateBuckets.computeIfAbsent(userId, k -> new RateBucket());
        return !bucket.tryConsume();
    }

    /**
     * Computa um hash SHA-256 simples do perfil de saúde do usuário para composição da chave de cache.
     * Chamado estaticamente pela SpEL expression no @Cacheable.
     */
    public static String profileHash(Long userId, UserInfoRepository repo) {
        Optional<UserInfo> opt = repo.findByUsuarioId(userId);
        if (opt.isEmpty()) return "no-profile";
        UserInfo u = opt.get();
        String raw = userId + "|" + u.getDataNasc() + "|" + u.getNivelCondicionamento()
                + "|" + u.getNotasMedicas() + "|" + u.getTotalKmRun();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes());
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rate limiting: bucket deslizante por minuto
    // ─────────────────────────────────────────────────────────────────────────

    private static class RateBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                // Nova janela
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= RATE_LIMIT_PER_MINUTE;
        }
    }
}
