-- =============================================================
-- Script de recuperação: rodar SOMENTE se o Flyway falhou ao
-- tentar executar V14 ou V16 e deixou entradas com success=false
-- no flyway_schema_history.
--
-- Passos:
--   1. Conecte ao banco de desenvolvimento.
--   2. Rode este script para remover as entradas com falha.
--   3. Execute ./mvnw spring-boot:run novamente.
-- =============================================================

-- Ver o estado atual
SELECT version, description, success, installed_on
FROM flyway_schema_history
WHERE version IN ('14', '16')
ORDER BY version;

-- Remover entradas com falha (success = false)
DELETE FROM flyway_schema_history
WHERE version IN ('14', '16')
  AND success = false;

-- Se V14 ou V16 já tiver success=true com checksum antigo,
-- também remova para que o Flyway reexecute com o arquivo corrigido.
-- ATENÇÃO: use somente em banco de desenvolvimento!
-- DELETE FROM flyway_schema_history WHERE version IN ('14', '16');
