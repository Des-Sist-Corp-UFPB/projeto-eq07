package br.ufpb.dsc.corrida.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private Map<String, Object> stateBefore;

    private Map<String, Object> stateAfter;

    private String errorMessage;

    @Indexed(expireAfterSeconds = 7776000)
    private Instant timestamp;
}
