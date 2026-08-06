-- =============================================================
-- V17: Rastreamento de envio de e-mail de comprovante
-- =============================================================
-- A entidade Inscricao.java foi alterada para incluir dois campos
-- usados pelo EmailService para rastrear o envio do comprovante PDF:
--
--   emailEnviado    → email_enviado   (BOOLEAN NOT NULL DEFAULT false)
--   emailEnviadoEm  → email_enviado_em (TIMESTAMP WITH TIME ZONE, nullable)
--
-- O DEFAULT false é obrigatório para que registros existentes não
-- fiquem com NULL em uma coluna mapeada para o primitivo `boolean`
-- em Java (evita NullPointerException no Hibernate).
-- =============================================================

ALTER TABLE inscricao
    ADD COLUMN email_enviado    BOOLEAN                  NOT NULL DEFAULT FALSE,
    ADD COLUMN email_enviado_em TIMESTAMP WITH TIME ZONE;
