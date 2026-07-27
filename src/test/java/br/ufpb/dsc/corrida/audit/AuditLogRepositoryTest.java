package br.ufpb.dsc.corrida.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AuditLogRepository — JPA Persistence Integration Tests")
public class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Should persist AuditLog entity with UUID primary key and query ordered by createdAt DESC")
    void shouldPersistAndQueryAuditLogs() {
        AuditLog log1 = AuditLog.builder()
                .action("USER_UPDATE")
                .resource("/user/1")
                .userId("user1")
                .clientIp("127.0.0.1")
                .statusCode(200)
                .createdAt(Instant.now().minusSeconds(100))
                .build();

        AuditLog log2 = AuditLog.builder()
                .action("RACE_CREATE")
                .resource("/races")
                .userId("admin")
                .clientIp("10.0.0.1")
                .statusCode(201)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();

        assertThat(logs).hasSizeGreaterThanOrEqualTo(2);
        assertThat(logs.get(0).getAction()).isEqualTo("RACE_CREATE");
        assertThat(logs.get(0).getId()).isNotNull();
    }
}
