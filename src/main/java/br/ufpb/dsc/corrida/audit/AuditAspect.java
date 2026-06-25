package br.ufpb.dsc.corrida.audit;

import br.ufpb.dsc.corrida.race.Race;
import br.ufpb.dsc.corrida.race.RaceRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.*;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RaceRepository raceRepository;

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
            if (principal instanceof UserDetails) {
                operator = ((UserDetails) principal).getUsername();
            } else if (principal != null) {
                operator = principal.toString();
            }
        }

        String resource = auditable.resource();
        String action = auditable.action();

        // 1. Capture Target ID from method arguments
        String targetId = null;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if ("id".equalsIgnoreCase(parameterNames[i]) || "raceId".equalsIgnoreCase(parameterNames[i])) {
                    if (args[i] != null) {
                        targetId = args[i].toString();
                    }
                    break;
                }
            }
        }
        if (targetId == null && args != null && args.length > 0
            && (args[0] instanceof Long || args[0] instanceof Integer || args[0] instanceof String)) {
                targetId = args[0].toString();
        }
        // 2. Capture stateBefore (for updates: PUT/PATCH or action containing UPDATE/CANCEL)
        Map<String, Object> stateBefore = null;
        boolean isUpdateOrDelete = "PUT".equalsIgnoreCase(httpMethod) || 
                                   "PATCH".equalsIgnoreCase(httpMethod) || 
                                   "DELETE".equalsIgnoreCase(httpMethod) ||
                                   action.contains("UPDATE") || 
                                   action.contains("CANCEL");

        if (isUpdateOrDelete && "Corrida".equalsIgnoreCase(resource) && targetId != null) {
            try {
                Long id = Long.parseLong(targetId);
                Optional<Race> existing = raceRepository.findById(id);
                if (existing.isPresent()) {
                    stateBefore = mapearParaMapLimpo(existing.get());
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            // Failure logging
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

        // 3. Resolve targetId from return value if not captured before (e.g. for creation)
        if (targetId == null && result instanceof Race) {
            targetId = String.valueOf(((Race) result).getId());
        }

        // 4. Capture stateAfter
        Map<String, Object> stateAfter = null;
        if (result != null) {
            stateAfter = mapearParaMapLimpo(result);
        } else if ("Corrida".equalsIgnoreCase(resource) && targetId != null) {
            // For void methods (like cancelarCorrida), fetch updated entity state from db
            try {
                Long id = Long.parseLong(targetId);
                Optional<Race> updated = raceRepository.findById(id);
                if (updated.isPresent()) {
                    stateAfter = mapearParaMapLimpo(updated.get());
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }

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

    private void publishEvent(AuditLog auditLog, boolean success) {
        boolean isTransactional = TransactionSynchronizationManager.isActualTransactionActive();
        eventPublisher.publishEvent(new AuditLogEvent(this, auditLog, isTransactional, success));
    }

    // =========================================================================
    // Sanitization & Reflection Mapping
    // =========================================================================

    public static Map<String, Object> mapearParaMapLimpo(Object obj) {
        return mapearParaMapLimpoHelper(obj, new HashSet<>());
    }

    private static Map<String, Object> mapearParaMapLimpoHelper(Object obj, Set<Object> visited) {
        if (obj == null) return null;
        if (visited.contains(obj)) {
            return Collections.singletonMap("$ref", obj.getClass().getSimpleName() + "@" + System.identityHashCode(obj));
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

                    boolean hasLombokExclude = Arrays.stream(field.getAnnotations())
                    .anyMatch(annotation -> annotation.annotationType().getName()
                            .equals("lombok.ToString$Exclude"));

                    if (hasLombokExclude) {
                        continue;
                    }

                    Object value = field.get(obj);
                    if (value != null) {
                        if (isSimpleType(value.getClass())) {
                            map.put(name, value);
                        } else if (value instanceof Collection<?>) {
                            List<Object> cleanList = new ArrayList<>();
                            for (Object item : (Collection<?>) value) {
                                if (item != null) {
                                    if (isSimpleType(item.getClass())) {
                                        cleanList.add(item);
                                    } else {
                                        cleanList.add(mapearParaMapLimpoHelper(item, visited));
                                    }
                                }
                            }
                            map.put(name, cleanList);
                        } else if (value instanceof Map<?, ?>) {
                            Map<Object, Object> cleanMap = new HashMap<>();
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                                Object key = entry.getKey();
                                Object val = entry.getValue();
                                if (val != null) {
                                    if (isSimpleType(val.getClass())) {
                                        cleanMap.put(key, val);
                                    } else {
                                        cleanMap.put(key, mapearParaMapLimpoHelper(val, visited));
                                    }
                                }
                            }
                            map.put(name, cleanMap);
                        } else {
                            if (value.getClass().getName().startsWith("java.") ||
                                value.getClass().getName().startsWith("javax.") ||
                                value.getClass().getName().startsWith("jakarta.") ||
                                value.getClass().getName().contains("sun.")) {
                                map.put(name, value.toString());
                            } else {
                                map.put(name, mapearParaMapLimpoHelper(value, visited));
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            visited.remove(obj);
        }
        return map;
    }

    private static boolean isSensitiveField(String name) {
        String lower = name.toLowerCase();
        return lower.equals("password") ||
               lower.equals("senha") ||
               lower.equals("token") ||
               lower.equals("accesstoken") ||
               lower.equals("refreshtoken") ||
               lower.equals("tokenredefinicao") ||
               lower.equals("resettoken");
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Boolean.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               clazz == Character.class ||
               clazz == java.math.BigDecimal.class ||
               clazz == java.math.BigInteger.class ||
               clazz == Instant.class ||
               clazz == java.time.LocalDate.class ||
               clazz == java.time.LocalDateTime.class ||
               clazz == java.time.OffsetDateTime.class ||
               clazz.isEnum();
    }
}
