package br.ufpb.dsc.corrida.audit;

import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RaceRepository raceRepository;

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
        private String tokenRedefinicao;
        private String resetToken;
        
        @lombok.ToString.Exclude
        private String confidentialNotes;
    }

    @BeforeEach
    void setUp() {
        auditable = mock(Auditable.class);
        lenient().when(auditable.action()).thenReturn("RACE_CREATED");
        lenient().when(auditable.resource()).thenReturn("Corrida");
    }

    /*@Test
    @DisplayName("Should successfully sanitize sensitive and excluded fields")
    void shouldSanitizeSensitiveData() {
        DummyClass dummy = new DummyClass(
                "john_doe", "pass123", "senha123", "tok123",
                "access123", "refresh123", "redef123", "reset123",
                "do not look"
        );

        Map<String, Object> cleanMap = AuditAspect.mapearParaMapLimpo(dummy);

        assertThat(cleanMap).isNotNull();
        assertThat(cleanMap.get("username")).isEqualTo("john_doe");
        
        // Excluded sensitive names
        assertThat(cleanMap).doesNotContainKey("password");
        assertThat(cleanMap).doesNotContainKey("senha");
        assertThat(cleanMap).doesNotContainKey("token");
        assertThat(cleanMap).doesNotContainKey("accessToken");
        assertThat(cleanMap).doesNotContainKey("refreshToken");
        assertThat(cleanMap).doesNotContainKey("tokenRedefinicao");
        assertThat(cleanMap).doesNotContainKey("resetToken");
        
        // Excluded @ToString.Exclude fields
        assertThat(cleanMap).doesNotContainKey("confidentialNotes");
    } */

    @Test
    @DisplayName("Should log successful method execution and publish success event")
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

        ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        AuditLogEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.isSuccess()).isTrue();
        assertThat(publishedEvent.getAuditLog().getAction()).isEqualTo("RACE_CREATED");
        assertThat(publishedEvent.getAuditLog().getResource()).isEqualTo("Corrida");
        assertThat(publishedEvent.getAuditLog().getTargetId()).isEqualTo("100");
        assertThat(publishedEvent.getAuditLog().getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should log failed method execution, append _FAILED to action, store error message, publish fail event and rethrow exception")
    void shouldAuditFailure() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"id"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{100L});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("Validation error"));

        assertThatThrownBy(() -> aspect.audit(joinPoint, auditable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Validation error");

        ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        AuditLogEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.isSuccess()).isFalse();
        assertThat(publishedEvent.getAuditLog().getAction()).isEqualTo("RACE_CREATED_FAILED");
        assertThat(publishedEvent.getAuditLog().getErrorMessage()).isEqualTo("Validation error");
        assertThat(publishedEvent.getAuditLog().getTargetId()).isEqualTo("100");
    }
}
