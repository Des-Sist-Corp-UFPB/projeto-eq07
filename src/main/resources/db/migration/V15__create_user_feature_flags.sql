CREATE TABLE usuario_feature_flag (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    feature_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_usuario_feature_flag_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT uk_usuario_feature UNIQUE (usuario_id, feature_name)
);
