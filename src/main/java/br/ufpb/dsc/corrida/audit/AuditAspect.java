package br.ufpb.dsc.corrida.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.*;

@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        HttpServletRequest request = AuditContextUtils.getCurrentRequest();

        String ip = AuditContextUtils.getClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";
        String httpMethod = request != null ? request.getMethod() : "N/A";

        String operator = "anonymous";
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                operator = userDetails.getUsername();
            } else if (principal != null) {
                operator = principal.toString();
            }
        }

        String resource = auditable.resource();
        String action = auditable.action();
        String targetId = extrairTargetId(joinPoint);

        Map<String, Object> stateBefore = mapearArgumentos(joinPoint);

        Object result;

        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            AuditLog auditLog = AuditLog.builder()
                    .action(action + "_FAILED")
                    .operator(operator)
                    .ip(ip)
                    .userAgent(userAgent)
                    .httpMethod(httpMethod)
                    .resource(resource)
                    .targetId(targetId)
                    .stateBefore(stateBefore)
                    .errorMessage(throwable.getMessage())
                    .timestamp(Instant.now())
                    .build();

            publishEvent(auditLog, false);
            throw throwable;
        }

        Map<String, Object> stateAfter = result != null
                ? mapearParaMapLimpo(result)
                : null;

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .operator(operator)
                .ip(ip)
                .userAgent(userAgent)
                .httpMethod(httpMethod)
                .resource(resource)
                .targetId(targetId)
                .stateBefore(stateBefore)
                .stateAfter(stateAfter)
                .timestamp(Instant.now())
                .build();

        publishEvent(auditLog, true);

        return result;
    }

    private String extrairTargetId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i] != null
                        && parameterNames[i].toLowerCase().endsWith("id")
                        && args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        if (args != null && args.length > 0 && args[0] != null && isSimpleType(args[0].getClass())) {
            return args[0].toString();
        }

        return null;
    }

    private void publishEvent(AuditLog auditLog, boolean success) {
        boolean isTransactional = TransactionSynchronizationManager.isActualTransactionActive();
        eventPublisher.publishEvent(new AuditLogEvent(this, auditLog, isTransactional, success));
    }

    public static Map<String, Object> mapearParaMapLimpo(Object obj) {
        return mapearParaMapLimpoHelper(obj, new HashSet<>());
    }

    private static Map<String, Object> mapearParaMapLimpoHelper(Object obj, Set<Object> visited) {
        if (obj == null) {
            return null;
        }

        if (visited.contains(obj)) {
            return Collections.singletonMap(
                    "$ref",
                    obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj)
            );
        }

        visited.add(obj);

        Map<String, Object> map = new HashMap<>();

        try {
            Class<?> clazz = obj.getClass();

            while (clazz != null && clazz != Object.class) {
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    if (field.isSynthetic()) {
                        continue;
                    }

                    field.setAccessible(true);

                    String name = field.getName();

                    if (isSensitiveField(name)) {
                        continue;
                    }

                    Object value = field.get(obj);

                    if (value != null) {
                        map.put(name, sanitizarValor(value, visited));
                    }
                }

                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            log.debug("Erro ao mapear objeto para auditoria", e);
        } finally {
            visited.remove(obj);
        }

        return map;
    }

    private static Object sanitizarValor(Object value, Set<Object> visited) {
        if (value == null) {
            return null;
        }

        if (value instanceof TemporalAccessor) {
            return value.toString();
        }

        if (isSimpleType(value.getClass())) {
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

                if (val != null) {
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
            return className; // Retorna apenas o nome da classe para não carregar toString() pesados
        }
        
        if (visited.size() > 50) {
            return "Max Depth Reached";
        }

        return mapearParaMapLimpoHelper(value, visited);
    }

    private static boolean isSensitiveField(String name) {
        String lower = name.toLowerCase();

        return lower.equals("password")
                || lower.equals("senha")
                || lower.equals("token")
                || lower.equals("accesstoken")
                || lower.equals("refreshtoken")
                || lower.equals("tokenredefinicao")
                || lower.equals("resettoken")
                || lower.equals("rotageojson")
                || lower.equals("mapa");
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

            argumentos.put(parameterNames[i], sanitizarValor(arg, new HashSet<>()));
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