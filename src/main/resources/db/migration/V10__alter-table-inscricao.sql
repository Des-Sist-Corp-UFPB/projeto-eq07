ALTER TABLE inscricao DROP CONSTRAINT inscricao_usuario_corrida_uk;

CREATE UNIQUE INDEX inscricao_usuario_corrida_ativa_uk
    ON inscricao (usuario_id, corrida_id)
    WHERE status = 'ATIVA';