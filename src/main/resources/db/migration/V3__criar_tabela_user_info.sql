-- Migração V3: Criação da tabela user_info
-- Armazena dados físicos e médicos de cada corredor.
-- Possui relação 1:1 com a tabela usuario (usuario_id UNIQUE).

CREATE TABLE user_info (
    id                    BIGSERIAL PRIMARY KEY,
    usuario_id            BIGINT NOT NULL UNIQUE REFERENCES usuario(id),
    peso                  FLOAT NOT NULL,
    altura                FLOAT NOT NULL,
    genero                VARCHAR(30) NOT NULL,
    total_km_run          FLOAT NOT NULL DEFAULT 0,
    data_nasc             DATE NOT NULL,
    foto_perfil           TEXT,
    nivel_condicionamento VARCHAR(20),
    notas_medicas         TEXT,
    criado_em             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_info_usuario_id ON user_info (usuario_id);

COMMENT ON TABLE user_info IS 'Dados físicos e médicos do corredor (1:1 com usuario)';
COMMENT ON COLUMN user_info.peso IS 'Peso em quilogramas (> 0)';
COMMENT ON COLUMN user_info.altura IS 'Altura em centímetros (> 0)';
COMMENT ON COLUMN user_info.genero IS 'Gênero: MALE | FEMALE | OTHER | PREFER_NOT_TO_SAY';
COMMENT ON COLUMN user_info.total_km_run IS 'Total de quilômetros percorridos, padrão 0';
COMMENT ON COLUMN user_info.nivel_condicionamento IS 'Nível: BEGINNER | INTERMEDIATE | ADVANCED';
COMMENT ON COLUMN user_info.notas_medicas IS 'Observações médicas, lesões ou restrições';
