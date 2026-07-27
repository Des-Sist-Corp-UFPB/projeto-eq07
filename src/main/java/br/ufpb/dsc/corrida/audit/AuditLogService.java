package br.ufpb.dsc.corrida.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async("auditTaskExecutor")
    public void saveAuditLogAsync(AuditLog auditLog) {
        try {
            AuditLog salvo = auditLogRepository.save(auditLog);
            log.debug("[Audit] Log de auditoria persistido no PostgreSQL: id={}", salvo.getId());
        } catch (Exception e) {
            log.error("[Audit] Erro ao persistir registro de auditoria no PostgreSQL", e);
            throw e; // Lança para o AsyncUncaughtExceptionHandler capturar e registrar
        }
    }
}
