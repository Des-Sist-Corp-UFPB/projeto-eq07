package br.ufpb.dsc.corrida.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança da aplicação usando Spring Security 6.
 *
 * <p><strong>Como o Spring Security funciona?</strong><br>
 * O Spring Security é baseado em uma cadeia de filtros (Filter Chain) que intercepta
 * todas as requisições HTTP antes de chegarem ao Controller. Cada filtro tem uma
 * responsabilidade específica (autenticação, autorização, CSRF, etc.).
 *
 * <p><strong>Principais conceitos:</strong>
 * <ul>
 *   <li><strong>Authentication</strong>: Verifica quem é o usuário (login/senha).</li>
 *   <li><strong>Authorization</strong>: Verifica o que o usuário pode fazer (roles/permissões).</li>
 *   <li><strong>CSRF</strong>: Proteção contra Cross-Site Request Forgery.</li>
 *   <li><strong>PasswordEncoder</strong>: Nunca armazene senhas em texto puro! BCrypt aplica um
 *       hash com salt aleatório a cada chamada.</li>
 * </ul>
 *
 * <p><strong>{@code @Configuration} + {@code @EnableWebSecurity}:</strong><br>
 * {@code @Configuration} marca a classe como fonte de definição de beans.
 * {@code @EnableWebSecurity} ativa a integração do Spring Security com o contexto do Spring MVC.
 *
 * @author DSC - UFPB Campus IV
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Autowired
    private AutenticacaoFilter autenticacaoFilter;

    /**
     * Define o algoritmo de codificação de senhas.
     *
     * <p><strong>Por que BCrypt?</strong><br>
     * BCrypt é um algoritmo de hash adaptativo — você pode aumentar o "cost factor"
     * conforme os computadores ficam mais rápidos, sem precisar re-hashear as senhas.
     * Ele também adiciona um salt aleatório automaticamente, impedindo ataques de
     * rainbow table (tabelas pré-computadas de hashes).
     *
     * <p>Nunca use MD5, SHA-1 ou SHA-256 simples para senhas!
     *
     * @return instância do BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura a cadeia de filtros de segurança HTTP.
     *
     * <p>Este é o método central da configuração do Spring Security.
     * A API fluente do {@code HttpSecurity} permite configurar:
     * <ul>
     *   <li>Quais URLs são públicas e quais exigem autenticação</li>
     *   <li>Como o login é feito (formulário, OAuth2, JWT, etc.)</li>
     *   <li>Como o logout funciona</li>
     *   <li>Configurações de CSRF, headers de segurança, etc.</li>
     * </ul>
     *
     * @param http construtor de configuração de segurança HTTP
     * @return cadeia de filtros configurada
     * @throws Exception se ocorrer erro na configuração
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/user/login", "/user/registrar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user/*", "/user/*/profile").permitAll()
                        .requestMatchers("/ping", "/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/corridas", "/corridas/**", "/api/geo/search").permitAll()
                        .requestMatchers("/organizacao/*/corridas/**").hasRole("ORGANIZADOR")
                        .requestMatchers(HttpMethod.POST, "/api/races/*/eligibility-check").authenticated()
                        .requestMatchers("/login", "/registrar", "/registrar/organizador", "/organizacao/*", "/css/**", "/js/**", "/images/**", 
                                                    "/webjars/**", "/manifest.json", "/sw.js", "/corrida.ico"
                                                ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                // === CSRF (Cross-Site Request Forgery) ===
                // CSRF é um ataque onde um site malicioso faz requisições em nome do usuário autenticado.
                // O Spring Security protege adicionando um token único em formulários.
                // Para HTMX funcionar com PUT/DELETE, precisamos de uma configuração especial.
                // Em produção real, considere usar o mecanismo de CSRF com SameSite cookies.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .addFilterBefore(autenticacaoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Expõe o {@code AuthenticationManager} como bean do Spring.
     *
     * <p>Necessário quando você precisa injetar o {@code AuthenticationManager} em outras classes,
     * como em um controller de API REST que faz autenticação programática.
     * Para este projeto educacional, serve como exemplo de como expor o bean.
     *
     * @param config configuração de autenticação gerenciada pelo Spring Security
     * @return instância do AuthenticationManager
     * @throws Exception se ocorrer erro ao obter o manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
