-- =============================================================
-- V11: Novos campos para análise de risco via LLM
-- =============================================================

CREATE TYPE terreno_corrida AS ENUM ('ASFALTO', 'TRILHA', 'AREIA', 'MISTO');
CREATE TYPE clima_corrida AS ENUM ('AMENO', 'CALOR_INTENSO', 'FRIO_INTENSO', 'CHUVOSO', 'UMIDADE_ALTA');
CREATE TYPE dificuldade_corrida AS ENUM ('FACIL', 'MEDIO', 'DIFICIL', 'EXTREMO');

ALTER TABLE corrida ADD COLUMN terreno terreno_corrida;
ALTER TABLE corrida ADD COLUMN ganho_elevacao INT;
ALTER TABLE corrida ADD COLUMN clima_esperado clima_corrida;
ALTER TABLE corrida ADD COLUMN nivel_dificuldade dificuldade_corrida;

ALTER TABLE user_info ADD COLUMN consentimento_saude BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE inscricao ADD COLUMN alerta_risco_reconhecido BOOLEAN DEFAULT FALSE NOT NULL;
