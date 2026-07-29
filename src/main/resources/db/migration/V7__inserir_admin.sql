INSERT INTO usuario (username, nome, login, senha, papel, deletado)
VALUES ('admin', 'Administrador', 'admin', '$2a$10$XURPShQNCsLjp1ESc2laoO46CE8.XlY8.2q.QzZtQ1FwO5eN2r5C2', 'ADMINISTRADOR', false)
ON CONFLICT (login) DO NOTHING;
