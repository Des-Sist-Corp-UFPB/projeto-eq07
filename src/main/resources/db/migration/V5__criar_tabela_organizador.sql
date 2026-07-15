-- Migração V5: Criação das tabelas organizer e organization
-- Gerencia perfis de organizadores e suas organizações de corrida.

CREATE TABLE organizer (
    id            BIGSERIAL PRIMARY KEY,
    usuario_id    BIGINT NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    cref          VARCHAR(30) NOT NULL UNIQUE,
    cpf           VARCHAR(20) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL,
    whatsapp      VARCHAR(30) NOT NULL,
    uf_conselho   VARCHAR(2) NOT NULL,
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE organization (
    id            BIGSERIAL PRIMARY KEY,
    organizer_id  BIGINT NOT NULL UNIQUE REFERENCES organizer(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    founded_at    DATE NOT NULL,
    description   TEXT,
    logo_url      TEXT,
    city          VARCHAR(255),
    state         VARCHAR(255),
    social_link   VARCHAR(255),
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizer_usuario ON organizer (usuario_id);
CREATE INDEX idx_organization_organizer ON organization (organizer_id);

COMMENT ON TABLE organizer IS 'Perfil complementar de Organizador de Corridas';
COMMENT ON TABLE organization IS 'Informações da Organização de Corridas associada ao Organizador';
