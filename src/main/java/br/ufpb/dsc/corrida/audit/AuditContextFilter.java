package br.ufpb.dsc.corrida.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class AuditContextFilter extends OncePerRequestFilter {

    private final List<String> trustedProxies;

    public AuditContextFilter(@Value("${audit.trusted-proxies:127.0.0.1,::1}") String trustedProxiesConfig) {
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            this.trustedProxies = Arrays.stream(trustedProxiesConfig.split(","))
                    .map(String::trim)
                    .toList();
        } else {
            this.trustedProxies = Collections.emptyList();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String clientIp = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            String httpMethod = request.getMethod();
            String resource = request.getRequestURI();
            String userId = extractUserId();

            AuditContext.setClientIp(clientIp);
            AuditContext.setUserAgent(userAgent);
            AuditContext.setRequestId(requestId);
            AuditContext.setHttpMethod(httpMethod);
            AuditContext.setResource(resource);
            AuditContext.setUserId(userId);

            filterChain.doFilter(request, response);

            AuditContext.setStatusCode(response.getStatus());
        } finally {
            AuditContext.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] ips = xff.split(",");
                if (ips.length > 0) {
                    return ips[0].trim();
                }
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        for (String trusted : trustedProxies) {
            if (trusted.equals(remoteAddr) || trusted.equals("0:0:0:0:0:0:0:1") && remoteAddr.equals("127.0.0.1")) {
                return true;
            }
            // Basic subnet/wildcard support if needed
            if (trusted.endsWith("*") && remoteAddr.startsWith(trusted.substring(0, trusted.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private String extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            } else if (principal != null) {
                return principal.toString();
            }
        }
        return null;
    }
}
