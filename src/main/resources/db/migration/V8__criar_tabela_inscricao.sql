-- =============================================================
-- V8: Módulo de Inscrição em Corridas
-- =============================================================

CREATE TYPE status_inscricao AS ENUM ('ATIVA', 'CANCELADA');

CREATE TABLE inscricao (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT NOT NULL,
    corrida_id     BIGINT NOT NULL,
    data_inscricao TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    compareceu     BOOLEAN NOT NULL DEFAULT FALSE,
    status         status_inscricao NOT NULL DEFAULT 'ATIVA',
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inscricao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
        
    CONSTRAINT fk_inscricao_corrida
        FOREIGN KEY (corrida_id) REFERENCES corrida(id) ON DELETE CASCADE,
        
    CONSTRAINT inscricao_usuario_corrida_uk
        UNIQUE (usuario_id, corrida_id)
);
