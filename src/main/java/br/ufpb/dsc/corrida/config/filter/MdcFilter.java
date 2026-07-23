package br.ufpb.dsc.corrida.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP para enriquecimento do MDC (Mapped Diagnostic Context) do Logback/SLF4J
 * e registro do fluxo completo de entrada e saída de requisições.
 *
 * <p>Popula variáveis contextuais (requestId, userId, clientIp) em cada requisição.
 * O agente OpenTelemetry captura automaticamente estes atributos do MDC e os anexa
 * aos registros de log OTLP enviados ao Loki.
 */
@Component
public class MdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CLIENT_IP = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            // 1. Unique Request ID
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (!StringUtils.hasText(requestId)) {
                requestId = UUID.randomUUID().toString().substring(0, 8);
            }
            MDC.put(MDC_REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // 2. Client IP
            String clientIp = request.getHeader("X-Forwarded-For");
            if (!StringUtils.hasText(clientIp)) {
                clientIp = request.getRemoteAddr();
            }
            MDC.put(MDC_CLIENT_IP, clientIp);

            // 3. User ID / Principal (caso já autenticado via sessão/cookie)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(MDC_USER_ID, auth.getName());
            }

            log.info("[HTTP] --> {} {} (clientIp={})", request.getMethod(), request.getRequestURI(), clientIp);

            filterChain.doFilter(request, response);

        } finally {
            // Atualiza userId no MDC caso a autenticação tenha sido processada no meio da cadeia
            Authentication postAuth = SecurityContextHolder.getContext().getAuthentication();
            if (postAuth != null && postAuth.isAuthenticated() && !"anonymousUser".equals(postAuth.getPrincipal())) {
                MDC.put(MDC_USER_ID, postAuth.getName());
            }

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("[HTTP] <-- {} {} - Status {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);

            // Garante que o MDC seja limpo para a thread atual
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CLIENT_IP);
        }
    }
}
