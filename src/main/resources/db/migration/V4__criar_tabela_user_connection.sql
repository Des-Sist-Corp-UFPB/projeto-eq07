-- Migração V4: Criação da tabela user_connection
-- Armazena os pedidos de conexão/amizade entre corredores.

CREATE TABLE user_connection (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    receiver_id   BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    status        BOOLEAN, -- NULL: Pendente, true: Aceito, false: Recusado/Declinado
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_requester_receiver UNIQUE (requester_id, receiver_id)
);

CREATE INDEX idx_user_connection_requester ON user_connection (requester_id);
CREATE INDEX idx_user_connection_receiver ON user_connection (receiver_id);

COMMENT ON TABLE user_connection IS 'Tabela que gerencia conexões e solicitações de amizade entre usuários';
COMMENT ON COLUMN user_connection.status IS 'Status da conexão. NULL = Pendente, true = Aceito, false = Recusado';
