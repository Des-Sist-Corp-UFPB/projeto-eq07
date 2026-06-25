package br.ufpb.dsc.corrida.audit.event;

import br.ufpb.dsc.corrida.audit.domain.AuditLog;
import br.ufpb.dsc.corrida.audit.repository.AuditLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditLogEventListener {

    private final AuditLogRepository auditLogRepository;

    public AuditLogEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Ouve eventos de auditoria após a transação principal (PostgreSQL) ser confirmada com sucesso (COMMIT).
     * Caso a transação principal sofra um Rollback, esse método não é executado,
     * evitando salvar logs de negócio inconsistentes.
     * O @Async garante que a comunicação com o MongoDB seja feita em outra Thread.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuditLogEventAfterCommit(AuditLogEvent event) {
        AuditLog log = event.getAuditLog();
        // Não precisamos salvar falhas aqui, pois se houver falha, a transação da rollback.
        if (!log.getAction().endsWith("_FAILED")) {
            auditLogRepository.save(log);
        }
    }

    /**
     * Se o log é um log de FALHA, a transação sofreu rollback.
     * Portanto, @TransactionalEventListener(AFTER_COMMIT) seria ignorado.
     * Usamos @EventListener comum (sem transação) para garantir que logs de falha sejam gravados.
     */
    @Async
    @EventListener
    public void handleAuditLogEventAlways(AuditLogEvent event) {
        AuditLog log = event.getAuditLog();
        if (log.getAction().endsWith("_FAILED")) {
            auditLogRepository.save(log);
        }
    }
}
