-- =============================================================
-- V6: Módulo Corrida
-- =============================================================

CREATE TYPE status_corrida AS ENUM ('RASCUNHO', 'PUBLICADA', 'CANCELADA', 'ENCERRADA');
CREATE TYPE categoria_corrida AS ENUM ('C5K', 'C10K', 'C21K', 'C42K', 'OUTRO');
CREATE TYPE beneficio_corrida AS ENUM (
    'AGUA', 'CAMISA', 'MEDALHA', 'PRONTO_ATENDIMENTO',
    'CHIP_CRONOMETRAGEM', 'KIT_LARGADA', 'FRUTAS', 'OUTRO'
);

CREATE TABLE corrida (
    id                   BIGSERIAL PRIMARY KEY,
    slug                 VARCHAR(255) UNIQUE NOT NULL,
    nome                 VARCHAR(255) NOT NULL,
    descricao            TEXT NOT NULL,
    banner_url           VARCHAR(512),
    valor_inscricao      NUMERIC(10,2),
    max_inscricoes       INT,
    data_inicio          TIMESTAMP WITH TIME ZONE NOT NULL,
    status               status_corrida NOT NULL DEFAULT 'RASCUNHO',
    categoria            categoria_corrida NOT NULL,

    largada_lat          DOUBLE PRECISION NOT NULL,
    largada_lng          DOUBLE PRECISION NOT NULL,
    largada_endereco     VARCHAR(512) NOT NULL,

    chegada_lat          DOUBLE PRECISION NOT NULL,
    chegada_lng          DOUBLE PRECISION NOT NULL,
    chegada_endereco     VARCHAR(512) NOT NULL,

    distancia_km         NUMERIC(6,2),
    duracao_estimada_min INT,
    rota_geojson         TEXT,

    organization_id      BIGINT NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_corrida_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id)
);

CREATE TABLE corrida_beneficio (
    corrida_id BIGINT          NOT NULL,
    beneficio  beneficio_corrida NOT NULL,
    PRIMARY KEY (corrida_id, beneficio),
    CONSTRAINT fk_beneficio_corrida
        FOREIGN KEY (corrida_id) REFERENCES corrida(id) ON DELETE CASCADE
);
