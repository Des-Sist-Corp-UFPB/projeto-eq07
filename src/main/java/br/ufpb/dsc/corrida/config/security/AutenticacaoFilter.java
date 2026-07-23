package br.ufpb.dsc.corrida.config.security;

import br.ufpb.dsc.corrida.user.User;
import br.ufpb.dsc.corrida.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AutenticacaoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoFilter.class);

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getToken(request);

        if (token != null) {
            try {
                String subject = tokenService.getSubject(token);
                var usuario = repository.findByLogin(subject);
                if (usuario != null) {
                    var authorization = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authorization);
                    MDC.put("userId", usuario.getUsername());
                    log.debug("[Auth] Autenticação via JWT validada para login: {}", subject);
                } else {
                    log.warn("[Auth] Usuário associado ao token JWT não encontrado: {}", subject);
                }
            } catch (Exception e) {
                log.warn("[Auth] Falha na validação do token JWT: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    public String getToken(HttpServletRequest httpServletRequest) {
        var token = httpServletRequest.getHeader("Authorization");
        if (token != null) {
            return token.replace("Bearer ", "").trim();
        }
        return null;
    }
}
