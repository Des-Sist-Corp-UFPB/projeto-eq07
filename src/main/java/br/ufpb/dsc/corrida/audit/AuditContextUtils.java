package br.ufpb.dsc.corrida.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuditContextUtils {

    public static HttpServletRequest getCurrentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Pick first IP if multiple are proxy-forwarded
            String[] ips = xff.split(",");
            if (ips.length > 0) {
                return ips[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
