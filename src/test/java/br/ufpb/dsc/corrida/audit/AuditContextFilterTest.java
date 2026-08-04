package br.ufpb.dsc.corrida.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditContextFilter — Unit Tests")
class AuditContextFilterTest {

    @Mock
    private FilterChain filterChain;

    private AuditContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuditContextFilter("127.0.0.1,::1");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal() — popula AuditContext durante a requisição e limpa ao final")
    void doFilterInternal_populatesContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/races");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "TestBrowser");
        request.addHeader("X-Request-ID", "custom-req-id");
        request.addHeader("X-Forwarded-For", "203.0.113.195");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        User user = new User("atleta@test.com", "pass", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(AuditContext.getClientIp()).isEqualTo("203.0.113.195");
            assertThat(AuditContext.getUserAgent()).isEqualTo("TestBrowser");
            assertThat(AuditContext.getRequestId()).isEqualTo("custom-req-id");
            assertThat(AuditContext.getHttpMethod()).isEqualTo("GET");
            assertThat(AuditContext.getResource()).isEqualTo("/api/races");
            assertThat(AuditContext.getUserId()).isEqualTo("atleta@test.com");
        });

        // Ao final da requisição, deve ser limpo
        assertThat(AuditContext.getUserId()).isNull();
    }
}
