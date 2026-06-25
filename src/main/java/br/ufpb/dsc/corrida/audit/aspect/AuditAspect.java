package br.ufpb.dsc.corrida.audit.aspect;

import br.ufpb.dsc.corrida.audit.annotation.Auditable;
import br.ufpb.dsc.corrida.audit.domain.AuditLog;
import br.ufpb.dsc.corrida.audit.event.AuditLogEvent;
import br.ufpb.dsc.corrida.audit.utils.AuditContextUtils;
import br.ufpb.dsc.corrida.audit.utils.AuditSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditAspect {

    private final ApplicationEventPublisher eventPublisher;

    public AuditAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        HttpServletRequest request = AuditContextUtils.getRequest();

        AuditLog log = new AuditLog();
        log.setAction(auditable.action());
        log.setResource(auditable.resource());
        log.setTimestamp(Instant.now());

        String httpMethod = request != null ? request.getMethod() : "N/A";
        log.setHttpMethod(httpMethod);
        log.setIp(AuditContextUtils.getClientIp(request));
        log.setUserAgent(request != null ? request.getHeader("User-Agent") : "N/A");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.setOperator(auth != null && auth.isAuthenticated() ? auth.getName() : "ANONYMOUS");

        // Captura inteligente do estado/parâmetros:
        // Como o aspecto intercepta métodos genéricos (podendo receber DTOs, IDs, etc),
        // registramos os argumentos de entrada como stateBefore (a "intenção" da ação).
        // Se quiséssemos buscar no banco o estado exato anterior, precisaríamos do EntityManager
        // e da classe da Entidade (que poderia ser passada na anotação @Auditable).
        if ("PUT".equalsIgnoreCase(httpMethod) || "PATCH".equalsIgnoreCase(httpMethod) || "POST".equalsIgnoreCase(httpMethod)) {
            log.setStateBefore(capturarArgumentos(joinPoint));
        }

        try {
            // Executa o método de negócio (ex: salva no PostgreSQL)
            Object result = joinPoint.proceed();

            if (result != null) {
                // Captura o objeto resultante retornado pelo método (estado atualizado do banco)
                log.setStateAfter(AuditSanitizer.sanitize(result));
            }

            // Publica o evento para ser processado de forma assíncrona
            // preferencialmente APÓS o commit da transação do BD principal.
            eventPublisher.publishEvent(new AuditLogEvent(this, log));

            return result;

        } catch (Throwable e) {
            // Em caso de erro, marca a ação como falha e salva a mensagem
            log.setAction(auditable.action() + "_FAILED");
            log.setErrorMessage(e.getMessage());
            eventPublisher.publishEvent(new AuditLogEvent(this, log));
            
            throw e; // Relança a exceção para que o fluxo normal de erro (ControllerAdvice) funcione
        }
    }

    /**
     * Mapeia os argumentos do método interceptado para um Map seguro.
     */
    private Map<String, Object> capturarArgumentos(ProceedingJoinPoint joinPoint) {
        Map<String, Object> argsMap = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                // Ignora classes do Spring MVC (ex: HttpServletRequest) que não são serializáveis
                if (!(args[i] instanceof HttpServletRequest)) {
                    argsMap.put("arg" + i, AuditSanitizer.sanitize(args[i]));
                }
            }
        }
        return argsMap;
    }
}
