package br.ufpb.dsc.corrida.ors.dto;

import java.math.BigDecimal;

/**
 * Resposta simplificada do endpoint ORS /v2/directions/foot-walking/geojson.
 *
 * <p>O campo {@code geoJson} armazena a string JSON completa da rota,
 * que será persistida em {@code corrida.rota_geojson} e posteriormente
 * carregada pelo Leaflet.js no front-end.
 */
public record RotaDTO(
        String geoJson,
        BigDecimal distanciaKm,
        Integer duracaoEstimadaMin
) {}
