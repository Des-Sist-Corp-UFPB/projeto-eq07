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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureToggleAspect — Unit Tests")
class FeatureToggleAspectTest {

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private FeatureToggleAspect aspect;

    static class SampleService {
        @FeatureToggle("MY_FEATURE")
        public String sampleMethod() {
            return "real_result";
        }

        @FeatureToggle(value = "MY_FEATURE_WITH_FALLBACK", fallbackMethod = "myFallback")
        public String sampleMethodWithFallback() {
            return "real_result";
        }

        public String myFallback() {
            return "fallback_result";
        }
    }

    private SampleService targetObject;

    @BeforeEach
    void setUp() {
        targetObject = new SampleService();
    }

    @Test
    @DisplayName("aroundFeatureToggle() — quando a feature está ativada, prossegue a execução normalmente")
    void aroundFeatureToggle_featureEnabled() throws Throwable {
        Method method = SampleService.class.getMethod("sampleMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(featureToggleService.isFeatureEnabled("MY_FEATURE")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("real_result");

        Object result = aspect.aroundFeatureToggle(joinPoint);

        assertThat(result).isEqualTo("real_result");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("aroundFeatureToggle() — quando a feature está desativada e não há fallback, lança FeatureDisabledException")
    void aroundFeatureToggle_featureDisabledNoFallback() throws Throwable {
        Method method = SampleService.class.getMethod("sampleMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(targetObject);
        when(featureToggleService.isFeatureEnabled("MY_FEATURE")).thenReturn(false);

        assertThatThrownBy(() -> aspect.aroundFeatureToggle(joinPoint))
                .isInstanceOf(FeatureDisabledException.class);

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("aroundFeatureToggle() — quando a feature está desativada mas possui método de fallback, executa o fallback")
    void aroundFeatureToggle_featureDisabledWithFallback() throws Throwable {
        Method method = SampleService.class.getMethod("sampleMethodWithFallback");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(targetObject);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(featureToggleService.isFeatureEnabled("MY_FEATURE_WITH_FALLBACK")).thenReturn(false);

        Object result = aspect.aroundFeatureToggle(joinPoint);

        assertThat(result).isEqualTo("fallback_result");
        verify(joinPoint, never()).proceed();
    }
}
