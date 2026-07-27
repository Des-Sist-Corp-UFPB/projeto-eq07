package br.ufpb.dsc.corrida.featuretoggle;

import br.ufpb.dsc.corrida.audit.AuditContext;
import br.ufpb.dsc.corrida.audit.AuditContextSnapshot;
import br.ufpb.dsc.corrida.audit.AuditLog;
import br.ufpb.dsc.corrida.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspecto AOP responsável por interceptar métodos anotados com {@link FeatureToggle}
 * e controlar sua execução com base no estado do Feature Flag no banco de dados.
 *
 * <h3>Precedência de Execução</h3>
 * Este aspecto é anotado com {@code @Order(Ordered.HIGHEST_PRECEDENCE)}, garantindo que
 * seja executado <b>antes</b> do {@code AuditAspect}. Isso evita que tentativas bloqueadas
 * por flags desabilitadas sejam auditadas como execuções normais.
 *
 * <h3>Resolução de Anotação (Precedência)</h3>
 * <ol>
 *   <li>Verifica primeiro se o método interceptado possui {@link FeatureToggle}.</li>
 *   <li>Se não, verifica se a classe do bean alvo possui a anotação.</li>
 *   <li>A anotação de <b>MÉTODO sempre sobrepõe</b> a de CLASSE.</li>
 * </ol>
 *
 * <h3>Fluxo de Execução</h3>
 * <ul>
 *   <li>Flag <b>habilitada</b>: executa o método normalmente via {@code joinPoint.proceed()}.</li>
 *   <li>Flag <b>desabilitada</b> + {@code fallbackMethod} configurado:
 *       invoca o método de fallback via reflexão (com cache interno para eliminar overhead).</li>
 *   <li>Flag <b>desabilitada</b> + sem {@code fallbackMethod}: lança {@link FeatureDisabledException}.</li>
 * </ul>
 *
 * <h3>Limitação de Auto-invocação (Self-Invocation)</h3>
 * O Spring AOP utiliza proxies dinâmicos (JDK/CGLIB). Chamadas internas entre métodos da
 * mesma classe ({@code this.metodo()}) <b>não passam pelo proxy</b> e portanto <b>não serão
 * interceptadas</b> por este aspecto. Sempre invoque métodos anotados através de referências
 * a beans gerenciados pelo Spring (ex: injetando o próprio serviço via {@code @Autowired}).
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class FeatureToggleAspect {

    private final FeatureToggleService featureToggleService;
    private final AuditLogService auditLogService;

    /**
     * Cache de métodos de fallback resolvidos via reflexão.
     * Chave: "ClassName#methodName(ParamType1,ParamType2,...)"
     * Elimina overhead de reflexão em chamadas repetidas.
     */
    private final ConcurrentHashMap<String, Method> fallbackMethodCache = new ConcurrentHashMap<>();

    @Around("@annotation(br.ufpb.dsc.corrida.featuretoggle.FeatureToggle) " +
            "|| @within(br.ufpb.dsc.corrida.featuretoggle.FeatureToggle)")
    public Object aroundFeatureToggle(ProceedingJoinPoint joinPoint) throws Throwable {
        FeatureToggle featureToggle = resolveAnnotation(joinPoint);

        // Caso extremo: nenhuma anotação encontrada (não deve ocorrer com o pointcut atual)
        if (featureToggle == null) {
            return joinPoint.proceed();
        }

        String featureKey = featureToggle.value();

        if (featureToggleService.isFeatureEnabled(featureKey)) {
            return joinPoint.proceed();
        }

        // Flag desabilitada — registrar tentativa de acesso bloqueada
        recordBlockedAttemptAuditLog(joinPoint, featureKey);

        String fallbackMethodName = featureToggle.fallbackMethod();
        if (fallbackMethodName == null || fallbackMethodName.isBlank()) {
            throw new FeatureDisabledException(featureKey);
        }

        return invokeFallback(joinPoint, fallbackMethodName, featureKey);
    }

    // =========================================================================
    // Resolução de Anotação (Método > Classe)
    // =========================================================================

    /**
     * Resolve a anotação {@link FeatureToggle} aplicável ao join point atual.
     * A anotação no nível de MÉTODO sempre tem precedência sobre a de CLASSE.
     *
     * @param joinPoint join point interceptado
     * @return a anotação resolvida, ou {@code null} se não encontrada
     */
    private FeatureToggle resolveAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. Anotação no nível de método (maior precedência)
        FeatureToggle methodAnnotation = method.getAnnotation(FeatureToggle.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // 2. Anotação no nível de classe (menor precedência)
        return joinPoint.getTarget().getClass().getAnnotation(FeatureToggle.class);
    }

    // =========================================================================
    // Invocação de Fallback via Reflexão (com cache)
    // =========================================================================

    /**
     * Invoca o método de fallback configurado na anotação.
     *
     * <p>O {@link Method} resolvido é cacheado em {@link #fallbackMethodCache} para evitar
     * overhead de reflexão em chamadas repetidas ao mesmo método.
     *
     * <p>Se o método não existir, lança {@link IllegalStateException} (falha explícita).
     * Se o método lançar uma exceção durante a invocação, ela é desembrulhada do
     * {@link InvocationTargetException} e relançada como a causa original, preservando
     * semântica de transação e tratamento de erros.
     *
     * @param joinPoint        join point original
     * @param fallbackName     nome do método de fallback
     * @param featureKey       chave do flag (para mensagem de erro)
     * @return resultado da invocação do fallback
     * @throws Throwable se o fallback lançar uma exceção
     */
    private Object invokeFallback(ProceedingJoinPoint joinPoint, String fallbackName, String featureKey) throws Throwable {
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?>[] paramTypes = signature.getParameterTypes();

        String cacheKey = buildFallbackCacheKey(target.getClass(), fallbackName, paramTypes);

        Method fallback = fallbackMethodCache.computeIfAbsent(cacheKey, k -> {
            try {
                Method m = target.getClass().getMethod(fallbackName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });

        if (fallback == null) {
            // Remove a entrada nula para não poluir o cache
            fallbackMethodCache.remove(cacheKey);
            String errorMsg = String.format(
                    "[FeatureToggle] Fallback method '%s(%s)' não encontrado na classe '%s' para o flag '%s'. " +
                    "Verifique se a assinatura do fallback corresponde exatamente ao método original.",
                    fallbackName,
                    buildParamTypesString(paramTypes),
                    target.getClass().getSimpleName(),
                    featureKey
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        try {
            return fallback.invoke(target, args);
        } catch (InvocationTargetException e) {
            // Desembrulha a exceção original para preservar semântica de transação e tratamento de erros
            throw e.getCause();
        }
    }

    /**
     * Constrói a chave de cache para o mapa de métodos de fallback resolvidos.
     *
     * @param clazz      classe que contém o método de fallback
     * @param methodName nome do método de fallback
     * @param paramTypes tipos dos parâmetros do método
     * @return chave única no formato "ClassName#methodName(ParamType1,...)"
     */
    private String buildFallbackCacheKey(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        return clazz.getName() + "#" + methodName + "(" + buildParamTypesString(paramTypes) + ")";
    }

    private String buildParamTypesString(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(paramTypes[i].getSimpleName());
        }
        return sb.toString();
    }

    // =========================================================================
    // Integração com Audit Log
    // =========================================================================

    /**
     * Registra de forma assíncrona a tentativa de acesso bloqueada por feature flag desabilitada.
     *
     * <p>Erros durante o salvamento do log de auditoria são capturados e registrados via
     * {@code log.error} para garantir que a exceção de auditoria <b>nunca interrompa</b>
     * o fluxo principal (lançamento de {@link FeatureDisabledException} ou invocação do fallback).
     *
     * @param joinPoint  join point interceptado
     * @param featureKey chave do feature flag que estava desabilitado
     */
    private void recordBlockedAttemptAuditLog(ProceedingJoinPoint joinPoint, String featureKey) {
        try {
            AuditContextSnapshot snapshot = AuditContext.getSnapshot();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            String methodName = joinPoint.getTarget().getClass().getSimpleName()
                    + "." + signature.getMethod().getName();
            String action = methodName + "_FEATURE_DISABLED";

            String resource = (snapshot.resource() != null && !snapshot.resource().isBlank())
                    ? snapshot.resource()
                    : "FEATURE_TOGGLE: " + featureKey;

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .resource(resource)
                    .userId(snapshot.userId())
                    .requestId(snapshot.requestId())
                    .clientIp(snapshot.clientIp())
                    .userAgent(snapshot.userAgent())
                    .httpMethod(snapshot.httpMethod())
                    .statusCode(503)
                    .errorMessage("Access denied: Feature flag '" + featureKey + "' is disabled.")
                    .createdAt(Instant.now())
                    .build();

            auditLogService.saveAuditLogAsync(auditLog);

        } catch (Exception e) {
            log.error("[FeatureToggle] Falha ao registrar audit log para flag desabilitada '{}'. " +
                      "O fluxo principal não será interrompido.", featureKey, e);
        }
    }
}
