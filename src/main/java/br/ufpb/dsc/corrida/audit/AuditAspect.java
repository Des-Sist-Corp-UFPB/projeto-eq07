package br.ufpb.dsc.corrida.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aspecto AOP para interceptação de métodos anotados com {@link Auditable}.
 *
 * <p><strong>Limitação de Auto-invocação (Self-Invocation Limitation):</strong><br>
 * O Spring AOP utiliza proxies dinâmicos (JDK / CGLIB). Chamadas internas entre métodos
 * da mesma classe (ex: {@code this.metodoAuditavel()}) NÃO passam pelo proxy e portanto
 * NÃO serão interceptadas por este aspecto. Sempre invoque métodos auditáveis através de
 * referências a beans gerenciados pelo Spring.
 */
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private br.ufpb.dsc.corrida.featuretoggle.FeatureToggleService featureToggleService;

    @Autowired(required = false)
    private EntityManager entityManager;

    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {

        // 0. Verifica se a pipeline de auditoria está ativa
        if (featureToggleService != null && !featureToggleService.isFeatureEnabled("AUDIT_NEW_PIPELINE")) {
            return joinPoint.proceed();
        }

        // 1. Snapshot dos dados do contexto na thread síncrona HTTP
        AuditContextSnapshot snapshot = AuditContext.getSnapshot();

        String action = auditable.action();
        if (action.isBlank()) {
            action = joinPoint.getSignature().getName();
        }

        String resource = auditable.resource();
        if (resource.isBlank() && snapshot.resource() != null) {
            resource = snapshot.resource();
        }

        String targetId = extrairTargetId(joinPoint, auditable);
        String entityBeforeJson = capturarEntityBeforeJson(joinPoint, auditable, targetId);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            String userId = resolveUserId(snapshot, joinPoint, null);
            Integer errorStatusCode = snapshot.statusCode() != null ? snapshot.statusCode() : 500;

            AuditLog failLog = AuditLog.builder()
                    .action(action + "_FAILED")
                    .resource(resource)
                    .userId(userId)
                    .requestId(snapshot.requestId())
                    .clientIp(snapshot.clientIp())
                    .userAgent(snapshot.userAgent())
                    .httpMethod(snapshot.httpMethod())
                    .statusCode(errorStatusCode)
                    .targetId(targetId)
                    .entityBefore(entityBeforeJson)
                    .errorMessage(throwable.getMessage())
                    .createdAt(Instant.now())
                    .build();

            auditLogService.saveAuditLogAsync(failLog);
            throw throwable;
        }

        // Sucesso
        String userId = resolveUserId(snapshot, joinPoint, result);
        String entityAfterJson = serializarParaJsonMascarado(result);

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .resource(resource)
                .userId(userId)
                .requestId(snapshot.requestId())
                .clientIp(snapshot.clientIp())
                .userAgent(snapshot.userAgent())
                .httpMethod(snapshot.httpMethod())
                .statusCode(snapshot.statusCode() != null ? snapshot.statusCode() : 200)
                .targetId(targetId)
                .entityBefore(entityBeforeJson)
                .entityAfter(entityAfterJson)
                .createdAt(Instant.now())
                .build();

        auditLogService.saveAuditLogAsync(auditLog);

        return result;
    }

    private String resolveUserId(AuditContextSnapshot snapshot, ProceedingJoinPoint joinPoint, Object result) {
        if (snapshot.userId() != null && !snapshot.userId().isBlank() && !"anonymous".equalsIgnoreCase(snapshot.userId())) {
            return snapshot.userId();
        }

        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        if (args != null && paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null) continue;

                if (arg instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                    return ud.getUsername();
                }

                // Verifica DTOs com campo/getter de login
                try {
                    var method = arg.getClass().getMethod("login");
                    Object val = method.invoke(arg);
                    if (val != null && !val.toString().isBlank()) return val.toString();
                } catch (Exception ignored) {}

                try {
                    var method = arg.getClass().getMethod("username");
                    Object val = method.invoke(arg);
                    if (val != null && !val.toString().isBlank()) return val.toString();
                } catch (Exception ignored) {}

                if ("login".equalsIgnoreCase(paramNames[i]) || "username".equalsIgnoreCase(paramNames[i])) {
                    return arg.toString();
                }
            }
        }

        if (result instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }

        return "anonymous";
    }

    private String capturarEntityBeforeJson(ProceedingJoinPoint joinPoint, Auditable auditable, String targetId) {
        try {
            Class<?> targetEntityClass = resolveEntityClass(joinPoint, auditable);

            if (entityManager != null && targetEntityClass != null && targetId != null) {
                Object entityId = converterId(targetId, targetEntityClass);
                if (entityId != null) {
                    Object existingEntity = entityManager.find(targetEntityClass, entityId);
                    if (existingEntity != null) {
                        return serializarParaJsonMascarado(existingEntity);
                    }

                    // Caso especial: UserInfo por usuario.id se a busca por PK retornar nulo
                    if ("UserInfo".equals(targetEntityClass.getSimpleName())) {
                        try {
                            List<?> list = entityManager.createQuery(
                                            "SELECT u FROM UserInfo u WHERE u.usuario.id = :uid", targetEntityClass)
                                    .setParameter("uid", entityId)
                                    .getResultList();
                            if (!list.isEmpty()) {
                                return serializarParaJsonMascarado(list.get(0));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Fallback: mapear os argumentos de entrada do método
            Map<String, Object> argsMap = mapearArgumentos(joinPoint);
            return serializarParaJsonMascarado(argsMap);
        } catch (Exception e) {
            log.debug("Erro ao capturar estado anterior para auditoria", e);
            return null;
        }
    }

    private Class<?> resolveEntityClass(ProceedingJoinPoint joinPoint, Auditable auditable) {
        if (auditable.entityClass() != Void.class) {
            return auditable.entityClass();
        }

        if (joinPoint.getSignature() instanceof MethodSignature signature) {
            Class<?> returnType = signature.getReturnType();
            if (returnType.isAnnotationPresent(jakarta.persistence.Entity.class)) {
                return returnType;
            }

            for (Class<?> paramType : signature.getParameterTypes()) {
                if (paramType.isAnnotationPresent(jakarta.persistence.Entity.class)) {
                    return paramType;
                }
            }
        }
        return null;
    }

    private Object converterId(String targetId, Class<?> entityClass) {
        try {
            Class<?> current = entityClass;
            while (current != null && current != Object.class) {
                try {
                    var idField = current.getDeclaredField("id");
                    Class<?> idType = idField.getType();
                    if (idType == Long.class || idType == long.class) {
                        return Long.parseLong(targetId);
                    } else if (idType == Integer.class || idType == int.class) {
                        return Integer.parseInt(targetId);
                    } else if (idType == java.util.UUID.class) {
                        return java.util.UUID.fromString(targetId);
                    }
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
        
        try {
            return Long.parseLong(targetId);
        } catch (Exception ignored) {}

        return targetId;
    }

    private String extrairTargetId(ProceedingJoinPoint joinPoint, Auditable auditable) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames == null || args == null) {
            return null;
        }

        // Se idParam foi especificado na anotação
        if (!auditable.idParam().isBlank()) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (auditable.idParam().equalsIgnoreCase(parameterNames[i]) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        // Convenção: parâmetro terminando em 'id' ou igual a 'id'
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i] != null
                    && (parameterNames[i].equalsIgnoreCase("id") || parameterNames[i].toLowerCase().endsWith("id"))
                    && args[i] != null) {
                return args[i].toString();
            }
        }

        if (args.length > 0 && args[0] != null && isSimpleType(args[0].getClass())) {
            return args[0].toString();
        }

        return null;
    }

    public static String serializarParaJsonMascarado(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (isSimpleType(obj.getClass())) {
                if (isSensitiveValue(obj.toString())) {
                    return jsonMapper.writeValueAsString("*****");
                }
                return jsonMapper.writeValueAsString(obj);
            }
            Object cleaned = sanitizarValor(obj, new HashSet<>());
            return jsonMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            log.debug("Erro ao serializar objeto mascarado para JSON", e);
            return null;
        }
    }

    public static Map<String, Object> mapearParaMapLimpo(Object obj) {
        Object cleaned = sanitizarValor(obj, new HashSet<>());
        if (cleaned instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) map;
            return resultMap;
        }
        return Collections.singletonMap("result", cleaned);
    }

    private static Object sanitizarValor(Object value, Set<Object> visited) {
        if (value == null) {
            return null;
        }

        if (value instanceof TemporalAccessor) {
            return value.toString();
        }

        if (isSimpleType(value.getClass())) {
            if (isSensitiveValue(value.toString())) {
                return "*****";
            }
            return value;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> cleanList = new ArrayList<>();
            for (Object item : collection) {
                if (item != null) {
                    cleanList.add(sanitizarValor(item, visited));
                }
            }
            return cleanList;
        }

        if (value instanceof Map<?, ?> originalMap) {
            Map<Object, Object> cleanMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                if (key != null && isSensitiveField(key.toString())) {
                    cleanMap.put(key, "*****");
                } else if (val != null) {
                    cleanMap.put(key, sanitizarValor(val, visited));
                }
            }
            return cleanMap;
        }

        String className = value.getClass().getName();
        if (className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.contains("sun.")
                || className.startsWith("org.hibernate")
                || className.startsWith("org.springframework")
                || className.startsWith("org.slf4j")
                || className.startsWith("ch.qos.logback")) {
            return className;
        }

        if (visited.contains(value)) {
            return Collections.singletonMap(
                    "$ref",
                    value.getClass().getSimpleName() + "@" + System.identityHashCode(value)
            );
        }

        visited.add(value);
        Map<String, Object> map = new HashMap<>();

        try {
            Class<?> clazz = value.getClass();

            while (clazz != null && clazz != Object.class) {
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    if (field.isSynthetic()) {
                        continue;
                    }

                    field.setAccessible(true);
                    String name = field.getName();

                    if (isSensitiveField(name)) {
                        map.put(name, "*****");
                        continue;
                    }

                    Object val;
                    try {
                        val = field.get(value);
                    } catch (Exception e) {
                        continue;
                    }

                    if (val != null) {
                        map.put(name, sanitizarValor(val, visited));
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            log.debug("Erro ao sanitizar valor de objeto", e);
        } finally {
            visited.remove(value);
        }

        return map;
    }

    private static boolean isSensitiveField(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.equals("password")
                || lower.equals("senha")
                || lower.equals("token")
                || lower.equals("accesstoken")
                || lower.equals("refreshtoken")
                || lower.equals("tokenredefinicao")
                || lower.equals("resettoken")
                || lower.equals("cpf")
                || lower.equals("creditcard")
                || lower.equals("cartao");
    }

    private static boolean isSensitiveValue(String val) {
        if (val == null) return false;
        // Identifica JWT token (ex: eyJhbGci...)
        return val.startsWith("eyJ") && val.length() > 30;
    }

    private Map<String, Object> mapearArgumentos(ProceedingJoinPoint joinPoint) {
        Map<String, Object> argumentos = new HashMap<>();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames == null || args == null) {
            return argumentos;
        }

        for (int i = 0; i < parameterNames.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            if (isSensitiveField(parameterNames[i])) {
                argumentos.put(parameterNames[i], "*****");
            } else {
                argumentos.put(parameterNames[i], sanitizarValor(arg, new HashSet<>()));
            }
        }

        return argumentos;
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || clazz == Boolean.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Double.class
                || clazz == Float.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Character.class
                || clazz == java.math.BigDecimal.class
                || clazz == java.math.BigInteger.class
                || clazz.isEnum();
    }
}