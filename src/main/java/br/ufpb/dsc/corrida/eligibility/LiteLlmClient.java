package br.ufpb.dsc.corrida.eligibility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para o proxy LiteLLM (compatível com a API OpenAI).
 *
 * <p>Usa JSON mode ({@code response_format: {type: "json_object"}}) para obter respostas
 * estruturadas. A temperatura é configurável via {@code litellm.temperature} e enviada
 * explicitamente em cada chamada, garantindo consistência entre execuções dentro da janela
 * de cache.
 *
 * <p>Em caso de timeout ou erro de rede, lança {@link LlmUnavailableException} para que
 * o chamador aplique o fallback seguro — a inscrição nunca deve ser bloqueada por falha
 * deste serviço auxiliar.
 */
@Component
public class LiteLlmClient {

    private static final Logger log = LoggerFactory.getLogger(LiteLlmClient.class);

    /** Temperatura padrão para chamadas de análise de risco. Baixa = resultados mais determinísticos. */
    private static final double DEFAULT_ELIGIBILITY_TEMPERATURE = 0.1;

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de informação sobre saúde e corridas de rua. \
            Sua única função é analisar os dados do perfil do atleta e as características da corrida \
            para identificar se há algum risco à saúde que mereça atenção antes da inscrição. \
            REGRAS OBRIGATÓRIAS:
            1. Você NÃO é um médico e NÃO emite laudos ou diagnósticos. \
            2. Quando identificar risco, apenas recomende que o usuário consulte um profissional de saúde. \
            3. Responda EXCLUSIVAMENTE no formato JSON a seguir, sem texto externo: \
               {"apto": true} ou {"apto": false, "resposta": "Justificativa clara e sem alarmismo."} \
            4. Use "apto: false" apenas quando os dados indicarem risco relevante e observável. \
            5. Nunca invente dados. Se não houver informações suficientes, retorne {"apto": true}. \
            6. Dê sempre prioridade absoluta ao valor numérico informado no campo 'Distância' (em km) para avaliar o esforço físico exigido pela corrida. A 'Categoria' da corrida é apenas uma classificação nominal e pode diferir da distância real do percurso.
            """;

    private final RestClient restClient;
    private final String model;
    private final double temperature;
    private final ObjectMapper objectMapper;

    public LiteLlmClient(
            @Value("${litellm.base-url:http://localhost:4000}") String baseUrl,
            @Value("${litellm.api-key:}") String apiKey,
            @Value("${litellm.model:gpt-4o}") String model,
            @Value("${litellm.temperature:0.1}") double temperature,
            @Value("${litellm.timeout-ms:10000}") int timeoutMs,
            ObjectMapper objectMapper) {
        this.model = model;
        this.temperature = temperature;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Executa a análise de risco via LLM.
     *
     * @param userContext  contexto do perfil do atleta (pré-sanitizado)
     * @param raceContext  contexto da corrida
     * @return resposta estruturada da LLM
     * @throws LlmUnavailableException se timeout, erro de rede ou resposta inválida (após retentativa)
     */
    public EligibilityResponse check(String userContext, String raceContext) {
        log.info("[LiteLlmClient] Iniciando checagem via LLM. Modelo: {}, Temp: {}", model, temperature);
        log.debug("[LiteLlmClient] Contexto Usuario: {}", userContext);
        log.debug("[LiteLlmClient] Contexto Corrida: {}", raceContext);
        String userMessage = buildUserMessage(userContext, raceContext);
        EligibilityResponse response = callWithRetry(userMessage, 2);
        log.info("[LiteLlmClient] Checagem via LLM retornou: apto={}, resposta={}", response.apto(), response.resposta());
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private EligibilityResponse callWithRetry(String userMessage, int attemptsLeft) {
        try {
            return doCall(userMessage);
        } catch (LlmUnavailableException e) {
            if (attemptsLeft > 1) {
                log.warn("[LiteLlmClient] LLM retornou erro/timeout. Retentando... ({} tentativas restantes)", attemptsLeft - 1);
                return callWithRetry(userMessage, attemptsLeft - 1);
            }
            throw e;
        }
    }

    private EligibilityResponse doCall(String userMessage) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", temperature,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        try {
            log.info("[LiteLlmClient] Enviando payload HTTP POST para o proxy...");
            String rawJson = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.debug("[LiteLlmClient] Resposta JSON recebida: {}", rawJson);
            return parseResponse(rawJson);

        } catch (ResourceAccessException e) {
            log.error("[LiteLlmClient] Timeout ou erro de rede ao chamar LiteLLM", e);
            throw new LlmUnavailableException("LLM timeout", e);
        } catch (RestClientResponseException e) {
            log.error("[LiteLlmClient] LiteLLM retornou erro HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmUnavailableException("LLM HTTP error " + e.getStatusCode(), e);
        }
    }

    private EligibilityResponse parseResponse(String rawJson) {
        try {
            // Extrai o conteúdo da mensagem do payload de resposta do OpenAI
            var root = objectMapper.readTree(rawJson);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            log.info("[LiteLlmClient] Mensagem extraida da resposta: {}", content);
            return objectMapper.readValue(content, EligibilityResponse.class);
        } catch (JsonProcessingException e) {
            log.error("[LiteLlmClient] Falha ao parsear resposta da LLM: {}", rawJson);
            throw new LlmUnavailableException("LLM invalid JSON response", e);
        }
    }

    private String buildUserMessage(String userContext, String raceContext) {
        return "PERFIL DO ATLETA:\n" + userContext + "\n\nDETALHES DA CORRIDA:\n" + raceContext;
    }
}
