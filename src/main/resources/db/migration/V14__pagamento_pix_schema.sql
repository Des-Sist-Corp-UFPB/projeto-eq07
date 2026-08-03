-- =============================================================
-- V14: Integração Mercado Pago (Pix) — Schema DDL
-- =============================================================
-- Estratégia de enums no banco:
--   Os status são armazenados como VARCHAR(30) puro — sem tipos
--   ENUM nativos do PostgreSQL. A camada de aplicação (JPA +
--   @Enumerated(EnumType.STRING)) é responsável pela validação
--   dos valores permitidos. Isso evita DDL complexo com
--   ALTER TYPE ... ADD VALUE e suas restrições transacionais.
-- =============================================================

-- 1. Remover o índice parcial ANTES de alterar o tipo da coluna.
--    O Postgres não permite ALTER COLUMN enquanto existe um índice
--    que compara a coluna contra um tipo incompatível (status_inscricao).
DROP INDEX IF EXISTS inscricao_usuario_corrida_ativa_uk;

-- 2. Remover o DEFAULT da coluna status (o default referencia o
--    tipo ENUM — deve ser removido antes do ALTER COLUMN).
ALTER TABLE inscricao ALTER COLUMN status DROP DEFAULT;

-- 3. Converter a coluna status de ENUM para VARCHAR(30).
--    O cast status::text é seguro: converte 'ATIVA' → 'ATIVA', etc.
ALTER TABLE inscricao
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

-- 4. Restaurar um default literal (string pura).
ALTER TABLE inscricao
    ALTER COLUMN status SET DEFAULT 'ATIVA';

-- 5. Descartar o tipo ENUM antigo do PostgreSQL (já não é mais
--    referenciado pela coluna após o passo 3).
DROP TYPE IF EXISTS status_inscricao CASCADE;

-- 6. Recriar o índice único parcial cobrindo todos os status
--    "ativos" — compara contra strings literais, não contra ENUM.
CREATE UNIQUE INDEX inscricao_usuario_corrida_ativa_uk
    ON inscricao (usuario_id, corrida_id)
    WHERE status IN ('AGUARDANDO_PAGAMENTO', 'ATIVA', 'CONFIRMADA');

-- 7. Criar tabela de pagamentos Pix (1:1 com inscricao).
--    Colunas payment_method e status armazenadas como VARCHAR.
CREATE TABLE pagamento (
    id                  BIGSERIAL PRIMARY KEY,
    inscricao_id        BIGINT NOT NULL UNIQUE,
    mp_payment_id       BIGINT UNIQUE,
    payment_method      VARCHAR(20) NOT NULL DEFAULT 'PIX',
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    amount              NUMERIC(10, 2) NOT NULL,
    qr_code_pix         TEXT,
    qr_code_base64_pix  TEXT,
    idempotency_key     VARCHAR(36) UNIQUE,
    expiration_date     TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pagamento_inscricao
        FOREIGN KEY (inscricao_id) REFERENCES inscricao(id) ON DELETE CASCADE
);

-- 8. Índice para o job de expiração (busca status PENDENTE + data).
CREATE INDEX idx_pagamento_status_expiracao
    ON pagamento (status, expiration_date)
    WHERE status = 'PENDENTE';

-- 9. Índice para reconciliação por mp_payment_id (webhook).
CREATE INDEX idx_pagamento_mp_payment_id
    ON pagamento (mp_payment_id)
    WHERE mp_payment_id IS NOT NULL;

-- 10. Adicionar CPF ao perfil do atleta.
--     Nullable no banco — obrigatoriedade aplicada pela aplicação
--     apenas para corridas pagas (valorInscricao > 0).
ALTER TABLE user_info ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);

CREATE UNIQUE INDEX IF NOT EXISTS user_info_cpf_uk
    ON user_info (cpf)
    WHERE cpf IS NOT NULL;