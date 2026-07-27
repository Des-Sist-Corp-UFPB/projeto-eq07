package br.ufpb.dsc.corrida.audit;

import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.user.dto.LoginDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditAspect — Unit Tests")
public class AuditAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditAspect aspect;

    private Auditable auditable;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DummyClass {
        private String username;
        private String password;
        private String senha;
        private String token;
        private String accessToken;
        private String refreshToken;
        private String cpf;
    }

    @BeforeEach
    void setUp() {
        auditable = mock(Auditable.class);
        lenient().when(auditable.action()).thenReturn("RACE_CREATED");
        lenient().when(auditable.resource()).thenReturn("Corrida");
        lenient().when(auditable.entityClass()).thenReturn((Class) Void.class);
        lenient().when(auditable.idParam()).thenReturn("");

        AuditContext.setUserId("user_test");
        AuditContext.setRequestId("req_123");
        AuditContext.setClientIp("192.168.1.100");
        AuditContext.setUserAgent("JUnit/TestAgent");
        AuditContext.setHttpMethod("POST");
        AuditContext.setResource("/api/races");
        AuditContext.setStatusCode(201);
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    @Test
    @DisplayName("Should successfully sanitize sensitive fields into *****")
    void shouldSanitizeSensitiveData() {
        DummyClass dummy = new DummyClass(
                "john_doe", "pass123", "senha123", "tok123",
                "access123", "refresh123", "12345678900"
        );

        Map<String, Object> cleanMap = AuditAspect.mapearParaMapLimpo(dummy);

        assertThat(cleanMap).isNotNull();
        assertThat(cleanMap.get("username")).isEqualTo("john_doe");
        assertThat(cleanMap.get("password")).isEqualTo("*****");
        assertThat(cleanMap.get("senha")).isEqualTo("*****");
        assertThat(cleanMap.get("token")).isEqualTo("*****");
        assertThat(cleanMap.get("accessToken")).isEqualTo("*****");
        assertThat(cleanMap.get("refreshToken")).isEqualTo("*****");
        assertThat(cleanMap.get("cpf")).isEqualTo("*****");
    }

    @Test
    @DisplayName("Should log successful method execution and call saveAuditLogAsync")
    void shouldAuditSuccess() throws Throwable {
        Race mockRace = new Race();
        mockRace.setId(100L);
        mockRace.setNome("Corrida de Teste");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{100L});
        when(joinPoint.proceed()).thenReturn(mockRace);

        Object result = aspect.audit(joinPoint, auditable);

        assertThat(result).isEqualTo(mockRace);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).saveAuditLogAsync(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo("RACE_CREATED");
        assertThat(savedLog.getResource()).isEqualTo("Corrida");
        assertThat(savedLog.getUserId()).isEqualTo("user_test");
        assertThat(savedLog.getRequestId()).isEqualTo("req_123");
        assertThat(savedLog.getClientIp()).isEqualTo("192.168.1.100");
        assertThat(savedLog.getUserAgent()).isEqualTo("JUnit/TestAgent");
        assertThat(savedLog.getHttpMethod()).isEqualTo("POST");
        assertThat(savedLog.getStatusCode()).isEqualTo(201);
        assertThat(savedLog.getTargetId()).isEqualTo("100");
        assertThat(savedLog.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should audit LOGIN action correctly and extract username from LoginDto even if unauthenticated prior to call")
    void shouldAuditLoginAction() throws Throwable {
        AuditContext.clear(); // unauthenticated initially
        Auditable loginAuditable = mock(Auditable.class);
        when(loginAuditable.action()).thenReturn("LOGIN");
        when(loginAuditable.resource()).thenReturn("USER");
        when(loginAuditable.entityClass()).thenReturn((Class) Void.class);
        when(loginAuditable.idParam()).thenReturn("");

        LoginDto credenciais = new LoginDto("john_login", "pass123");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"credenciais"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{credenciais});
        when(joinPoint.proceed()).thenReturn("eyJhbGciOiJIUzI1NiJ9.fakeTokenString");

        Object result = aspect.audit(joinPoint, loginAuditable);

        assertThat(result).isNotNull();

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).saveAuditLogAsync(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo("LOGIN");
        assertThat(savedLog.getResource()).isEqualTo("USER");
        assertThat(savedLog.getUserId()).isEqualTo("john_login");
        assertThat(savedLog.getEntityAfter()).contains("*****");
    }

    @Test
    @DisplayName("Should log failed method execution, append _FAILED to action, store error message and rethrow exception")
    void shouldAuditFailure() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{100L});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("Validation error"));

        assertThatThrownBy(() -> aspect.audit(joinPoint, auditable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Validation error");

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).saveAuditLogAsync(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo("RACE_CREATED_FAILED");
        assertThat(savedLog.getErrorMessage()).isEqualTo("Validation error");
        assertThat(savedLog.getTargetId()).isEqualTo("100");
        assertThat(savedLog.getUserId()).isEqualTo("user_test");
    }
}
