package br.ufpb.dsc.corrida.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditContext — Unit Tests")
class AuditContextTest {

    @BeforeEach
    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    @Test
    @DisplayName("setters e getters armazenam e recuperam valores no ThreadLocal e MDC")
    void testSettersAndGetters() {
        AuditContext.setUserId("user-123");
        AuditContext.setRequestId("req-456");
        AuditContext.setClientIp("192.168.1.1");
        AuditContext.setUserAgent("Mozilla/5.0");
        AuditContext.setHttpMethod("POST");
        AuditContext.setResource("/api/races");
        AuditContext.setStatusCode(200);

        assertThat(AuditContext.getUserId()).isEqualTo("user-123");
        assertThat(AuditContext.getRequestId()).isEqualTo("req-456");
        assertThat(AuditContext.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(AuditContext.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(AuditContext.getHttpMethod()).isEqualTo("POST");
        assertThat(AuditContext.getResource()).isEqualTo("/api/races");
        assertThat(AuditContext.getStatusCode()).isEqualTo(200);

        AuditContextSnapshot snapshot = AuditContext.getSnapshot();
        assertThat(snapshot.userId()).isEqualTo("user-123");
        assertThat(snapshot.requestId()).isEqualTo("req-456");
        assertThat(snapshot.clientIp()).isEqualTo("192.168.1.1");
        assertThat(snapshot.userAgent()).isEqualTo("Mozilla/5.0");
        assertThat(snapshot.httpMethod()).isEqualTo("POST");
        assertThat(snapshot.resource()).isEqualTo("/api/races");
        assertThat(snapshot.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("clear() limpa todos os campos do ThreadLocal e MDC")
    void testClear() {
        AuditContext.setUserId("user-123");
        AuditContext.setRequestId("req-456");

        AuditContext.clear();

        assertThat(AuditContext.getUserId()).isNull();
        assertThat(AuditContext.getRequestId()).isNull();
        assertThat(AuditContext.getClientIp()).isNull();
        assertThat(AuditContext.getUserAgent()).isNull();
        assertThat(AuditContext.getHttpMethod()).isNull();
        assertThat(AuditContext.getResource()).isNull();
        assertThat(AuditContext.getStatusCode()).isNull();
    }
}
