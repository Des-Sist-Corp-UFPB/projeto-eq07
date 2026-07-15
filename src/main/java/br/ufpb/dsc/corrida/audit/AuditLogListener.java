package br.ufpb.dsc.corrida.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLogListener {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @EventListener
    public void handleAuditLog(AuditLogEvent event) {
        System.out.println("RECEBI EVENTO DE AUDITORIA: " + event.getAuditLog().getAction());

        AuditLog salvo = auditLogRepository.save(event.getAuditLog());

        System.out.println("AUDIT SALVO NO MONGO COM ID: " + salvo.getId());
    }
}
