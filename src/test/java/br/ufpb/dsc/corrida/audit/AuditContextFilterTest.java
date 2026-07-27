package br.ufpb.dsc.corrida.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("AuditContextFilter — Unit & Filter Chain Tests")
public class AuditContextFilterTest {

    private AuditContextFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new AuditContextFilter("127.0.0.1, 10.0.0.1");
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(response.getStatus()).thenReturn(200);
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should extract userId from SecurityContextHolder, capture request metadata and clear context in finally block")
    void shouldExtractContextAndClearInFinally() throws ServletException, IOException {
        User springUser = new User("authenticated_user", "password", Collections.emptyList());
        var auth = new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        AtomicReference<AuditContextSnapshot> capturedSnapshot = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedSnapshot.set(AuditContext.getSnapshot());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);

        AuditContextSnapshot snapshot = capturedSnapshot.get();
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.userId()).isEqualTo("authenticated_user");
        assertThat(snapshot.httpMethod()).isEqualTo("POST");
        assertThat(snapshot.resource()).isEqualTo("/api/test");
        assertThat(snapshot.clientIp()).isEqualTo("127.0.0.1");
        assertThat(snapshot.userAgent()).isEqualTo("Mozilla/5.0");

        // Asserte cleanup after filter execution
        assertThat(AuditContext.getUserId()).isNull();
        assertThat(AuditContext.getClientIp()).isNull();
    }

    @Test
    @DisplayName("Should extract first IP from X-Forwarded-For ONLY when coming from a trusted proxy")
    void shouldExtractXForwardedForForTrustedProxy() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1"); // trusted proxy
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

        AtomicReference<String> capturedIp = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedIp.set(AuditContext.getClientIp());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedIp.get()).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Should ignore X-Forwarded-For when request comes from an UNTRUSTED IP (prevent header spoofing)")
    void shouldIgnoreXForwardedForForUntrustedProxy() throws ServletException, IOException {
        when(request.getRemoteAddr()).thenReturn("198.51.100.42"); // untrusted external IP
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2");

        AtomicReference<String> capturedIp = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedIp.set(AuditContext.getClientIp());
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(capturedIp.get()).isEqualTo("198.51.100.42");
    }
}
