package br.ufpb.dsc.corrida.audit;

import jakarta.servlet.http.HttpServletRequest;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

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

    @InjectMocks
    private AuditAspect aspect;

    private Auditable auditable;

    private MockedStatic<AuditContextUtils> contextUtilsMock;

    // ---------- Dummy classes used to exercise the reflection-based mapper ----------

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
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NestedClass {
        private String label;
        private DummyClass child;
    }

    @Getter
    @Setter
    public static class SelfReferencingClass {
        private String name;
        private SelfReferencingClass self;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectionHolder {
        private List<String> items;
        private Map<String, Object> attributes;
        private Instant createdAt;
        private BigDecimal amount;
    }

    public static class UnreadableClass {
        @SuppressWarnings("unused")
        private final Object trap = new Object() {
            // triggers no getter issue, just a plain nested anonymous field
        };
    }

    @BeforeEach
    void setUp() {
        auditable = mock(Auditable.class);
        contextUtilsMock = mockStatic(AuditContextUtils.class);
        contextUtilsMock.when(AuditContextUtils::getCurrentRequest).thenReturn(null);
        contextUtilsMock.when(() -> AuditContextUtils.getClientIp(any())).thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        contextUtilsMock.close();
        SecurityContextHolder.clearContext();
    }

    private void setupBasicArgs(Object[] args, String[] names) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(names);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    // ---------------------------------------------------------------------
    // Success / failure flows
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Should log successful method execution and publish success event")
    void shouldAuditSuccess() throws Throwable {
        lenient().when(auditable.action()).thenReturn("RACE_CREATED");
        lenient().when(auditable.resource()).thenReturn("Corrida");

        DummyClass result = new DummyClass("john", "pass", "senha", "tok", "acc", "ref", "redef", "reset");
        setupBasicArgs(new Object[]{100L}, new String[]{"id"});
        when(joinPoint.proceed()).thenReturn(result);

        Object returned = aspect.audit(joinPoint, auditable);

        assertThat(returned).isEqualTo(result);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        AuditLogEvent event = captor.getValue();
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getAuditLog().getAction()).isEqualTo("RACE_CREATED");
        assertThat(event.getAuditLog().getResource()).isEqualTo("Corrida");
        assertThat(event.getAuditLog().getTargetId()).isEqualTo("100");
        assertThat(event.getAuditLog().getErrorMessage()).isNull();
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("password");
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("senha");
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("token");
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("accessToken");
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("refreshToken");
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("tokenRedefinicao");
        assertThat(event.getAuditLog().getStateAfter()).doesNotContainKey("resetToken");
        assertThat(event.getAuditLog().getStateAfter()).containsEntry("username", "john");
    }

    @Test
    @DisplayName("Should return null stateAfter when proceed() returns null")
    void shouldHandleNullResult() throws Throwable {
        lenient().when(auditable.action()).thenReturn("RACE_VIEWED");
        lenient().when(auditable.resource()).thenReturn("Corrida");

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn(null);

        Object returned = aspect.audit(joinPoint, auditable);

        assertThat(returned).isNull();

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getStateAfter()).isNull();
    }

    @Test
    @DisplayName("Should log failed method execution, append _FAILED, store error, publish fail event and rethrow")
    void shouldAuditFailure() throws Throwable {
        lenient().when(auditable.action()).thenReturn("RACE_CREATED");
        lenient().when(auditable.resource()).thenReturn("Corrida");

        setupBasicArgs(new Object[]{100L}, new String[]{"id"});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("Validation error"));

        assertThatThrownBy(() -> aspect.audit(joinPoint, auditable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Validation error");

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        AuditLogEvent event = captor.getValue();
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getAuditLog().getAction()).isEqualTo("RACE_CREATED_FAILED");
        assertThat(event.getAuditLog().getErrorMessage()).isEqualTo("Validation error");
        assertThat(event.getAuditLog().getTargetId()).isEqualTo("100");
    }

    // ---------------------------------------------------------------------
    // Operator resolution branches
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Should use 'anonymous' when there is no authentication in context")
    void shouldUseAnonymousWhenNoAuth() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");
        SecurityContextHolder.clearContext();

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getOperator()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should use 'anonymous' when authentication exists but is not authenticated")
    void shouldUseAnonymousWhenNotAuthenticated() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getOperator()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Should use UserDetails username when principal is a UserDetails")
    void shouldUseUserDetailsUsername() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        UserDetails userDetails = User.withUsername("maria")
                .password("x")
                .authorities(Collections.emptyList())
                .build();
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getOperator()).isEqualTo("maria");
    }

    @Test
    @DisplayName("Should fall back to principal.toString() when principal is not a UserDetails")
    void shouldUsePrincipalToStringWhenNotUserDetails() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        Authentication auth = new UsernamePasswordAuthenticationToken("raw-principal", null, Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getOperator()).isEqualTo("raw-principal");
    }

    // ---------------------------------------------------------------------
    // HTTP request / header branches
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Should read userAgent and httpMethod from request when present")
    void shouldReadRequestHeadersWhenPresent() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getMethod()).thenReturn("POST");
        contextUtilsMock.when(AuditContextUtils::getCurrentRequest).thenReturn(request);
        contextUtilsMock.when(() -> AuditContextUtils.getClientIp(request)).thenReturn("10.0.0.1");

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AuditLog log = captor.getValue().getAuditLog();
        assertThat(log.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(log.getHttpMethod()).isEqualTo("POST");
        assertThat(log.getIp()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("Should default userAgent to 'unknown' and httpMethod to 'N/A' when request is null")
    void shouldDefaultWhenRequestIsNull() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AuditLog log = captor.getValue().getAuditLog();
        assertThat(log.getUserAgent()).isEqualTo("unknown");
        assertThat(log.getHttpMethod()).isEqualTo("N/A");
    }

    // ---------------------------------------------------------------------
    // extrairTargetId branches
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Should extract targetId from a parameter ending with 'id' (case-insensitive)")
    void shouldExtractTargetIdFromNamedParameter() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{"x", 42L}, new String[]{"name", "raceId"});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getTargetId()).isEqualTo("42");
    }

    @Test
    @DisplayName("Should fall back to first simple-type argument when no id-named parameter exists")
    void shouldFallBackToFirstSimpleArgument() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{"simpleArg", new DummyClass()}, new String[]{"text", "payload"});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getTargetId()).isEqualTo("simpleArg");
    }

    @Test
    @DisplayName("Should return null targetId when there are no usable arguments")
    void shouldReturnNullTargetIdWhenNoArgs() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getTargetId()).isNull();
    }

    @Test
    @DisplayName("Should return null targetId when parameterNames or args are null")
    void shouldReturnNullTargetIdWhenSignatureInfoMissing() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(null);
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getTargetId()).isNull();
        assertThat(captor.getValue().getAuditLog().getStateBefore()).isEmpty();
    }

    @Test
    @DisplayName("Should skip null arguments when mapping targetId/state and not throw NPE")
    void shouldSkipNullArguments() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{null, "value"}, new String[]{"raceId", "name"});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAuditLog().getTargetId()).isNull();
        assertThat(captor.getValue().getAuditLog().getStateBefore()).containsEntry("name", "value");
    }

    // ---------------------------------------------------------------------
    // mapearParaMapLimpo / sanitizarValor branches (static utility)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Should return null when mapping a null object")
    void shouldReturnNullForNullObject() {
        assertThat(AuditAspect.mapearParaMapLimpo(null)).isNull();
    }

    @Test
    @DisplayName("Should sanitize sensitive fields from a flat object")
    void shouldSanitizeSensitiveFields() {
        DummyClass dummy = new DummyClass("john_doe", "pass123", "senha123", "tok123",
                "access123", "refresh123", "redef123", "reset123");

        Map<String, Object> cleanMap = AuditAspect.mapearParaMapLimpo(dummy);

        assertThat(cleanMap).isNotNull();
        assertThat(cleanMap.get("username")).isEqualTo("john_doe");
        assertThat(cleanMap).doesNotContainKeys("password", "senha", "token", "accessToken",
                "refreshToken", "tokenRedefinicao", "resetToken");
    }

    @Test
    @DisplayName("Should recursively map nested non-java objects")
    void shouldMapNestedObjects() {
        DummyClass child = new DummyClass("child", "p", "s", "t", "a", "r", "rd", "rt");
        NestedClass nested = new NestedClass("parent-label", child);

        Map<String, Object> cleanMap = AuditAspect.mapearParaMapLimpo(nested);

        assertThat(cleanMap.get("label")).isEqualTo("parent-label");
        Object childValue = cleanMap.get("child");
        assertThat(childValue).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> childMap = (Map<String, Object>) childValue;
        assertThat(childMap.get("username")).isEqualTo("child");
        assertThat(childMap).doesNotContainKey("password");
    }

    @Test
    @DisplayName("Should detect circular references and emit a $ref placeholder instead of recursing infinitely")
    void shouldHandleCircularReferences() {
        SelfReferencingClass obj = new SelfReferencingClass();
        obj.setName("root");
        obj.setSelf(obj);

        Map<String, Object> cleanMap = AuditAspect.mapearParaMapLimpo(obj);

        assertThat(cleanMap.get("name")).isEqualTo("root");
        Object selfValue = cleanMap.get("self");
        assertThat(selfValue).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> selfMap = (Map<String, Object>) selfValue;
        assertThat(selfMap).containsKey("$ref");
        assertThat(((String) selfMap.get("$ref"))).contains("SelfReferencingClass@");
    }

    @Test
    @DisplayName("Should sanitize collections, maps, temporal values, and java.* values via toString")
    void shouldSanitizeCollectionsMapsAndTemporal() {
        CollectionHolder holder = new CollectionHolder(
                Arrays.asList("a", "b", null),
                new HashMap<>(Map.of("k1", "v1")),
                Instant.parse("2024-01-01T00:00:00Z"),
                new BigDecimal("10.50")
        );

        Map<String, Object> cleanMap = AuditAspect.mapearParaMapLimpo(holder);

        Object items = cleanMap.get("items");
        assertThat(items).isInstanceOf(List.class);
        assertThat((List<?>) items).asList().containsExactly("a", "b");

        Object attrs = cleanMap.get("attributes");
        assertThat(attrs).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) attrs).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("k1", "v1");

        assertThat(cleanMap.get("createdAt")).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(cleanMap.get("amount")).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    @DisplayName("Should swallow reflection errors and still return a (possibly partial) map")
    void shouldSwallowReflectionErrors() {
        Object obj = new Object() {
            private final String value = "ok";
        };

        Map<String, Object> result = AuditAspect.mapearParaMapLimpo(obj);

        assertThat(result).isNotNull();
    }

    // ---------------------------------------------------------------------
    // Transaction-state branch in publishEvent
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Should mark the audit event as transactional when an active transaction exists")
    void shouldMarkEventAsTransactionalWhenActive() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        try (MockedStatic<TransactionSynchronizationManager> txMock =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);

            aspect.audit(joinPoint, auditable);
        }

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isTransactional()).isTrue();
    }

    @Test
    @DisplayName("Should mark the audit event as non-transactional when there is no active transaction")
    void shouldMarkEventAsNonTransactionalWhenInactive() throws Throwable {
        lenient().when(auditable.action()).thenReturn("A");
        lenient().when(auditable.resource()).thenReturn("R");

        setupBasicArgs(new Object[]{}, new String[]{});
        when(joinPoint.proceed()).thenReturn("ok");

        try (MockedStatic<TransactionSynchronizationManager> txMock =
                     mockStatic(TransactionSynchronizationManager.class)) {
            txMock.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);

            aspect.audit(joinPoint, auditable);
        }

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isTransactional()).isFalse();
    }
}