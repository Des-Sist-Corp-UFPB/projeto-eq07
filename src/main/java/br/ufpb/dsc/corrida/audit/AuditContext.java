package br.ufpb.dsc.corrida.audit;

import org.slf4j.MDC;

public class AuditContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();
    private static final ThreadLocal<String> HTTP_METHOD = new ThreadLocal<>();
    private static final ThreadLocal<String> RESOURCE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> STATUS_CODE = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID.set(userId);
        if (userId != null) {
            MDC.put("userId", userId);
        } else {
            MDC.remove("userId");
        }
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
        if (requestId != null) {
            MDC.put("requestId", requestId);
        } else {
            MDC.remove("requestId");
        }
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void setClientIp(String clientIp) {
        CLIENT_IP.set(clientIp);
    }

    public static String getClientIp() {
        return CLIENT_IP.get();
    }

    public static void setUserAgent(String userAgent) {
        USER_AGENT.set(userAgent);
    }

    public static String getUserAgent() {
        return USER_AGENT.get();
    }

    public static void setHttpMethod(String httpMethod) {
        HTTP_METHOD.set(httpMethod);
    }

    public static String getHttpMethod() {
        return HTTP_METHOD.get();
    }

    public static void setResource(String resource) {
        RESOURCE.set(resource);
    }

    public static String getResource() {
        return RESOURCE.get();
    }

    public static void setStatusCode(Integer statusCode) {
        STATUS_CODE.set(statusCode);
    }

    public static Integer getStatusCode() {
        return STATUS_CODE.get();
    }

    public static AuditContextSnapshot getSnapshot() {
        return new AuditContextSnapshot(
                getUserId(),
                getRequestId(),
                getClientIp(),
                getUserAgent(),
                getHttpMethod(),
                getResource(),
                getStatusCode()
        );
    }

    public static void clear() {
        USER_ID.remove();
        REQUEST_ID.remove();
        CLIENT_IP.remove();
        USER_AGENT.remove();
        HTTP_METHOD.remove();
        RESOURCE.remove();
        STATUS_CODE.remove();

        MDC.remove("userId");
        MDC.remove("requestId");
    }
}
