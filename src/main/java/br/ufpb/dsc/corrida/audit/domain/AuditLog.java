package br.ufpb.dsc.corrida.audit.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String action;

    @Indexed
    private String operator;

    private String ip;

    private String userAgent;

    private String httpMethod;

    private String resource;

    private String targetId;

    private String errorMessage;

    private Map<String, Object> stateBefore;

    private Map<String, Object> stateAfter;

    // expireAfterSeconds = 7776000 equivale a 90 dias
    @Indexed(expireAfterSeconds = 7776000)
    private Instant timestamp;
}
