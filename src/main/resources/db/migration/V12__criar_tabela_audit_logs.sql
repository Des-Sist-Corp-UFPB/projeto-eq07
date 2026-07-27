-- =============================================================================
-- Migration V12: Criar tabela audit_logs para migração da auditoria do MongoDB -> PostgreSQL
-- =============================================================================
-- Estratégia de Particionamento e Retenção:
-- Como os logs de auditoria crescem indefinidamente, em ambiente de produção recomenda-se:
-- 1. Particionamento por data: PARTITION BY RANGE (created_at) com partições mensais.
-- 2. Rotina de expurgo/arquivamento: job agendado (ex: pg_cron ou cron externo) para
--    exportar e desanexar (DETACH PARTITION) partições com mais de X meses (ex: 90 dias).
-- =============================================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255),
    request_id VARCHAR(100),
    client_ip VARCHAR(45),
    user_agent TEXT,
    http_method VARCHAR(10),
    resource VARCHAR(255),
    action VARCHAR(255),
    status_code INTEGER,
    entity_before JSONB,
    entity_after JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    target_id VARCHAR(255),
    error_message TEXT
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_request_id ON audit_logs(request_id);
