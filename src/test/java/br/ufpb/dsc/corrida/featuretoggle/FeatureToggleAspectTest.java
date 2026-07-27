package br.ufpb.dsc.corrida.featuretoggle;

import br.ufpb.dsc.corrida.audit.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link FeatureToggleAspect}.
 *
 * <p>Estratégia: puro Mockito sem contexto Spring, substituindo os colaboradores
 * por mocks e configurando o {@link ProceedingJoinPoint} de forma programática
 * para cada cenário de teste.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureToggleAspect - Testes Unitários")
class FeatureToggleAspectTest {

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FeatureToggleAspect aspect;

    // -------------------------------------------------------------------------
    // Beans de suporte para os cenários de teste
    // -------------------------------------------------------------------------

    /** Bean de suporte: método com flag no nível de MÉTODO. */
    static class MethodLevelBean {

        @FeatureToggle(value = "PAYMENT_V2")
        public String protectedMethod() {
            return "original-result";
        }

        @FeatureToggle(value = "PAYMENT_V2", fallbackMethod = "fallback")
        public String protectedWithFallback(String arg) {
            return "original-result";
        }

        public String fallback(String arg) {
            return "fallback-result";
        }

        @FeatureToggle(value = "PAYMENT_V2", fallbackMethod = "nonExistentMethod")
        public String protectedWithBadFallback() {
            return "original-result";
        }

        @FeatureToggle(value = "PAYMENT_V2", fallbackMethod = "throwingFallback")
        public String protectedWithThrowingFallback() {
            return "original-result";
        }

        public String throwingFallback() throws IllegalArgumentException {
            throw new IllegalArgumentException("Erro original no fallback");
        }
    }

    /** Bean de suporte: flag somente no nível de CLASSE. */
    @FeatureToggle("CLASS_FLAG")
    static class ClassLevelBean {

        public String classLevelMethod() {
            return "class-level-result";
        }
    }

    /** Bean de suporte: CLASSE com flag, MÉTODO com flag diferente (método sobrescreve). */
    @FeatureToggle("CLASS_FLAG")
    static class MixedAnnotationBean {

        @FeatureToggle("METHOD_FLAG")
        public String methodOverridesClass() {
            return "method-level-result";
        }
    }

    // -------------------------------------------------------------------------
    // Helpers para construir o ProceedingJoinPoint
    // -------------------------------------------------------------------------

    private ProceedingJoinPoint buildJoinPoint(Object target, String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = target.getClass().getMethod(methodName, paramTypes);
        return buildJoinPoint(target, method, new Object[paramTypes.length]);
    }

    private ProceedingJoinPoint buildJoinPoint(Object target, String methodName, Object[] args, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = target.getClass().getMethod(methodName, paramTypes);
        return buildJoinPoint(target, method, args);
    }

    private ProceedingJoinPoint buildJoinPoint(Object target, Method method, Object[] args) {
        MethodSignature signature = mock(MethodSignature.class);
        
        // Uso do lenient() para evitar UnnecessaryStubbingException quando 
        // determinados métodos do JoinPoint não forem chamados em certos cenários de teste.
        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(signature.getParameterTypes()).thenReturn(method.getParameterTypes());

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getTarget()).thenReturn(target);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        
        return joinPoint;
    }

    // =========================================================================
    // Testes
    // =========================================================================

    /**
     * TC-01: Quando o flag está HABILITADO, o método original deve ser executado
     *        e seu valor de retorno deve ser preservado.
     */
    @Test
    @DisplayName("TC-01: Flag habilitada → executa método original e retorna seu resultado")
    void quandoFlagHabilitada_deveExecutarMetodoOriginal() throws Throwable {
        // Arrange
        MethodLevelBean target = new MethodLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "protectedMethod");
        when(featureToggleService.isFeatureEnabled("PAYMENT_V2")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("original-result");

        // Act
        Object result = aspect.aroundFeatureToggle(joinPoint);

        // Assert
        assertThat(result).isEqualTo("original-result");
        verify(joinPoint).proceed();
        verify(auditLogService, never()).saveAuditLogAsync(any());
    }

    /**
     * TC-02: Quando o flag está DESABILITADO e nenhum fallback foi configurado,
     *        deve lançar {@link FeatureDisabledException}.
     */
    @Test
    @DisplayName("TC-02: Flag desabilitada + sem fallback → lança FeatureDisabledException")
    void quandoFlagDesabilitadaSemFallback_deveLancarFeatureDisabledException() throws Throwable {
        // Arrange
        MethodLevelBean target = new MethodLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "protectedMethod");
        when(featureToggleService.isFeatureEnabled("PAYMENT_V2")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> aspect.aroundFeatureToggle(joinPoint))
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("PAYMENT_V2");

        verify(joinPoint, never()).proceed();
    }

    /**
     * TC-03: Quando o flag está DESABILITADO e um {@code fallbackMethod} válido foi configurado,
     *        deve invocar o fallback e retornar o seu resultado.
     */
    @Test
    @DisplayName("TC-03: Flag desabilitada + fallback configurado → invoca fallback e retorna seu resultado")
    void quandoFlagDesabilitadaComFallback_deveInvocarFallback() throws Throwable {
        // Arrange
        MethodLevelBean target = new MethodLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(
                target, "protectedWithFallback", new Object[]{"input"}, String.class);
        when(featureToggleService.isFeatureEnabled("PAYMENT_V2")).thenReturn(false);

        // Act
        Object result = aspect.aroundFeatureToggle(joinPoint);

        // Assert
        assertThat(result).isEqualTo("fallback-result");
        verify(joinPoint, never()).proceed();
    }

    /**
     * TC-04: Quando a anotação está no nível de CLASSE, todos os métodos da classe
     *        devem ser controlados pelo flag da classe.
     */
    @Test
    @DisplayName("TC-04: Anotação de CLASSE → flag da classe é respeitado em métodos sem anotação própria")
    void quandoAnotacaoNoNivelDeClasse_deveUsarFlagDaClasse() throws Throwable {
        // Arrange
        ClassLevelBean target = new ClassLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "classLevelMethod");
        when(featureToggleService.isFeatureEnabled("CLASS_FLAG")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("class-level-result");

        // Act
        Object result = aspect.aroundFeatureToggle(joinPoint);

        // Assert
        assertThat(result).isEqualTo("class-level-result");
        verify(featureToggleService).isFeatureEnabled("CLASS_FLAG");
        verify(joinPoint).proceed();
    }

    /**
     * TC-05: Quando há anotação tanto no MÉTODO quanto na CLASSE, a anotação do MÉTODO
     *        deve ter precedência sobre a da classe.
     */
    @Test
    @DisplayName("TC-05: Anotação de MÉTODO sobrescreve anotação de CLASSE")
    void quandoAnotacaoNoMetodoEClasse_deveUsarFlagDoMetodo() throws Throwable {
        // Arrange
        MixedAnnotationBean target = new MixedAnnotationBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "methodOverridesClass");
        when(featureToggleService.isFeatureEnabled("METHOD_FLAG")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("method-level-result");

        // Act
        Object result = aspect.aroundFeatureToggle(joinPoint);

        // Assert
        assertThat(result).isEqualTo("method-level-result");
        // Deve usar a flag do MÉTODO, nunca a flag da CLASSE
        verify(featureToggleService).isFeatureEnabled("METHOD_FLAG");
        verify(featureToggleService, never()).isFeatureEnabled("CLASS_FLAG");
    }

    /**
     * TC-06: Quando o {@code fallbackMethod} configurado não existe na classe alvo,
     *        deve lançar {@link IllegalStateException} explicitamente.
     */
    @Test
    @DisplayName("TC-06: fallbackMethod inexistente → lança IllegalStateException explicitamente")
    void quandoFallbackMethodNaoExiste_deveLancarIllegalStateException() throws Throwable {
        // Arrange
        MethodLevelBean target = new MethodLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "protectedWithBadFallback");
        when(featureToggleService.isFeatureEnabled("PAYMENT_V2")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> aspect.aroundFeatureToggle(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonExistentMethod")
                .hasMessageContaining("MethodLevelBean");

        verify(joinPoint, never()).proceed();
    }

    /**
     * TC-07: Quando o {@code fallbackMethod} lança uma exceção durante a invocação,
     *        a exceção deve ser desembrulhada do {@link java.lang.reflect.InvocationTargetException}
     *        e relançada como a causa original.
     */
    @Test
    @DisplayName("TC-07: Exceção dentro do fallback → relançada como causa original (desembrulhada de InvocationTargetException)")
    void quandoFallbackLancaExcecao_deveRelanclarCausaOriginal() throws Throwable {
        // Arrange
        MethodLevelBean target = new MethodLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "protectedWithThrowingFallback");
        when(featureToggleService.isFeatureEnabled("PAYMENT_V2")).thenReturn(false);

        // Act & Assert
        // A exceção lançada deve ser a CAUSA ORIGINAL, não InvocationTargetException
        assertThatThrownBy(() -> aspect.aroundFeatureToggle(joinPoint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Erro original no fallback");

        verify(joinPoint, never()).proceed();
    }

    /**
     * TC-08: Uma falha no salvamento do log de auditoria (saveAuditLogAsync) NÃO deve
     *        interferir no lançamento da {@link FeatureDisabledException} nem alterar
     *        o fluxo principal.
     */
    @Test
    @DisplayName("TC-08: Falha assíncrona no audit log não interfere no lançamento de FeatureDisabledException")
    void quandoAuditLogFalha_naoDeveInterferirNoFluxoPrincipal() throws Throwable {
        // Arrange
        MethodLevelBean target = new MethodLevelBean();
        ProceedingJoinPoint joinPoint = buildJoinPoint(target, "protectedMethod");
        when(featureToggleService.isFeatureEnabled("PAYMENT_V2")).thenReturn(false);
        doThrow(new RuntimeException("Falha de persistência simulada"))
                .when(auditLogService).saveAuditLogAsync(any());

        // Act & Assert
        // A exceção do audit log NÃO deve suprimir nem alterar a FeatureDisabledException
        assertThatThrownBy(() -> aspect.aroundFeatureToggle(joinPoint))
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("PAYMENT_V2");

        // Confirma que a tentativa de salvar foi feita (o aspecto tentou auditar)
        verify(auditLogService).saveAuditLogAsync(any());
        verify(joinPoint, never()).proceed();
    }
}
