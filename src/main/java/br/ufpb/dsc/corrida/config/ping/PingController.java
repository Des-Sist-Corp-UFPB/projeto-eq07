package br.ufpb.dsc.corrida.config.ping;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    private static final Logger log = LoggerFactory.getLogger(PingController.class);

    private final JdbcTemplate jdbcTemplate;

    public PingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("service", "eq07");
        response.put("timestamp", Instant.now().toString());

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            log.warn("Falha de verificação de conexão com banco no /ping", e);
            response.put("status", "degraded");
            response.put("database", "down");
            response.put("databaseError", e.getMessage());
        }

        return response;
    }
}