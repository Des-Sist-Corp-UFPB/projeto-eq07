INSERT INTO usuario (username, nome, login, senha, papel, deletado)
VALUES ('admin', 'Administrador', 'admin', '$2a$10$XURPShQNCsLjp1ESc2laoO4BRLusCGLn2cLyJuHtYp.RcgZ9617TG', 'ADMINISTRADOR', false)
ON CONFLICT (login) DO NOTHING;
