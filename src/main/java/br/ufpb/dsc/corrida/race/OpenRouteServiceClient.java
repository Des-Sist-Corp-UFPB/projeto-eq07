package br.ufpb.dsc.corrida.race;

import br.ufpb.dsc.corrida.exception.ExternalServiceException;
import br.ufpb.dsc.corrida.race.dto.RotaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Cliente HTTP para a API OpenRouteService.
 *
 * <p>Realiza duas operações:</p>
 * <ul>
 *   <li>Cálculo de rota entre dois pontos (directions/foot-walking).</li>
 *   <li>Proxy de geocodificação (geocode/search).</li>
 * </ul>
 *
 * <p>Falhas de rede ou erros 5xx/4xx do ORS são capturadas e relançadas como
 * {@link ExternalServiceException} com mensagem localizada — nunca como stack
 * trace raw para o usuário.</p>
 */
@Component
public class OpenRouteServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouteServiceClient.class);

    private static final String MSG_ROTA_FALHOU =
            "Não foi possível calcular a rota neste momento, tente novamente mais tarde.";
    private static final String MSG_GEO_FALHOU =
            "Não foi possível buscar o endereço neste momento, tente novamente mais tarde.";

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public OpenRouteServiceClient(
            @Value("${ors.base-url:https://api.openrouteservice.org}") String baseUrl,
            @Value("${ors.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Calcula a rota a pé entre dois pontos via ORS Directions API.
     *
     * @param largadaLng longitude do ponto de largada
     * @param largadaLat latitude do ponto de largada
     * @param chegadaLng longitude do ponto de chegada
     * @param chegadaLat latitude do ponto de chegada
     * @return {@link RotaDTO} com GeoJSON, distância e duração estimada
     * @throws ExternalServiceException se ORS falhar ou timeout ocorrer
     */
    public RotaDTO calcularRota(double largadaLng, double largadaLat,
                                double chegadaLng, double chegadaLat) {
        var payload = Map.of(
                "coordinates", new double[][]{{largadaLng, largadaLat}, {chegadaLng, chegadaLat}}
        );

        try {
            String responseBody = restClient.post()
                    .uri("/v2/directions/foot-walking/geojson")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseRotaResponse(responseBody);

        } catch (RestClientResponseException | ResourceAccessException e) {
            logger.error("Falha ao calcular rota via ORS. lat/lng: [{},{}] -> [{},{}]",
                    largadaLat, largadaLng, chegadaLat, chegadaLng, e);
            throw new ExternalServiceException(MSG_ROTA_FALHOU);
        }
    }

    /**
     * Busca endereços via ORS Geocode API (proxy para o front-end).
     *
     * @param text texto de busca do usuário
     * @return JSON bruto retornado pelo ORS
     * @throws ExternalServiceException se ORS falhar
     */
    public String geocodificarEndereco(String text) {
        try {
            return restClient.get()
                    .uri("/geocode/search?text={text}&size=5&api_key={apiKey}", text, apiKey)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException | ResourceAccessException e) {
            logger.error("Falha na geocodificação ORS para query: {}", text, e);
            throw new ExternalServiceException(MSG_GEO_FALHOU);
        }
    }

    // ---------------------------------------------------------------------------

    private RotaDTO parseRotaResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode summary = root
                    .path("features").get(0)
                    .path("properties")
                    .path("summary");

            double distanceMeters = summary.path("distance").asDouble();
            double durationSeconds = summary.path("duration").asDouble();

            BigDecimal distanciaKm = BigDecimal.valueOf(distanceMeters / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);
            int duracaoMin = (int) Math.ceil(durationSeconds / 60.0);

            return new RotaDTO(json, distanciaKm, duracaoMin);

        } catch (Exception e) {
            logger.error("Erro ao parsear resposta do ORS", e);
            throw new ExternalServiceException(MSG_ROTA_FALHOU);
        }
    }
}
