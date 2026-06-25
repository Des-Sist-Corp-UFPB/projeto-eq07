package br.ufpb.dsc.corrida.audit.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;
import java.util.Set;

public class AuditSanitizer {

    private static final ObjectMapper objectMapper;

    // Palavras-chave que indicam campos sensíveis que devem ser removidos do log de auditoria
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "senha", "token", "accesstoken", "refreshtoken",
            "tokenredefinicao", "resettoken", "secret", "credentials"
    );

    static {
        objectMapper = new ObjectMapper();
        // Registra módulo para suportar LocalDate, Instant, etc., sem quebrar a serialização
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Converte um objeto genérico para um Map estruturado (JSON like)
     * e sanitiza chaves sensíveis (ex: senhas, tokens).
     */
    public static Map<String, Object> sanitize(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // 1. Converte o Objeto para Map usando Jackson
            Map<String, Object> map = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
            
            // 2. Remove ou ofusca os campos sensíveis
            sanitizeMap(map);

            return map;
        } catch (Exception e) {
            // Em caso de erro de serialização (ex: proxies complexos ou ciclos),
            // registramos uma representação simplificada ao invés de quebrar a aplicação.
            return Map.of("error", "Não foi possível serializar o objeto para o log: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void sanitizeMap(Map<String, Object> map) {
        if (map == null) return;

        map.entrySet().removeIf(entry -> {
            String key = entry.getKey().toLowerCase();
            return SENSITIVE_KEYS.stream().anyMatch(key::contains);
        });

        // Chamada recursiva para processar sub-objetos
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                sanitizeMap((Map<String, Object>) entry.getValue());
            }
        }
    }
}
