package br.ufpb.dsc.corrida.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditLogListener {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void handleSuccessfulCommit(AuditLogEvent event) {
        if (event.isSuccess() && event.isTransactional()) {
            auditLogRepository.save(event.getAuditLog());
        }
    }

    @Async
    @EventListener
    public void handleFailedOrNonTransactional(AuditLogEvent event) {
        if (!event.isSuccess() || !event.isTransactional()) {
            auditLogRepository.save(event.getAuditLog());
        }
    }
}
