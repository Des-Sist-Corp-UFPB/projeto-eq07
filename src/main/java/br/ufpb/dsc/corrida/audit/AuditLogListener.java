package br.ufpb.dsc.corrida.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLogListener {

    private static final Logger log = LoggerFactory.getLogger(AuditLogListener.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @EventListener
    public void handleAuditLog(AuditLogEvent event) {
        log.info("Evento de auditoria recebido: action={}", event.getAuditLog().getAction());

        AuditLog salvo = auditLogRepository.save(event.getAuditLog());

        log.debug("Registro de auditoria salvo no MongoDB: id={}", salvo.getId());
    }
}
