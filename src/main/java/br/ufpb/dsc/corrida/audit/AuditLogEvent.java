package br.ufpb.dsc.corrida.audit;

import org.springframework.context.ApplicationEvent;

public class AuditLogEvent extends ApplicationEvent {

    private final AuditLog auditLog;
    private final boolean transactional;
    private final boolean success;

    public AuditLogEvent(Object source, AuditLog auditLog, boolean transactional, boolean success) {
        super(source);
        this.auditLog = auditLog;
        this.transactional = transactional;
        this.success = success;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public boolean isSuccess() {
        return success;
    }
}
