package br.ufpb.dsc.corrida.audit.repository;

import br.ufpb.dsc.corrida.audit.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
}
