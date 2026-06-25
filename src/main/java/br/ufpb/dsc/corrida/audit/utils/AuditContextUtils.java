package br.ufpb.dsc.corrida.audit.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuditContextUtils {

    /**
     * Recupera a requisição HTTP atual a partir do contexto do Spring.
     * Pode ser nulo se a chamada não originou-se de uma requisição web (ex: @Scheduled).
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Extrai o IP do cliente. 
     * Tenta primeiro o cabeçalho "X-Forwarded-For" (comum se estiver atrás de um Load Balancer/Proxy),
     * caso contrário usa o getRemoteAddr() padrão do Servlet.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }

        // X-Forwarded-For pode conter uma lista separada por vírgula de IPs.
        // O primeiro é o IP original do cliente.
        return xfHeader.split(",")[0].trim();
    }
}
