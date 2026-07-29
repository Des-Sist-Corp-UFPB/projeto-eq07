INSERT INTO feature_flags (key_name, enabled, description, updated_at)
VALUES
    ('SEARCH_RACES', true, 'Habilita o menu de busca de corridas na sidebar', CURRENT_TIMESTAMP),
    ('CREATE_RACE',  true, 'Habilita o menu de criação/gestão de corridas para organizadores', CURRENT_TIMESTAMP)
ON CONFLICT (key_name) DO NOTHING;
