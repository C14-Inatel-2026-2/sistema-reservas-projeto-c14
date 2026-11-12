-- Execute no SQL Editor do TiDB antes de iniciar a API pela primeira vez.
-- O Flyway continua responsavel pela criacao das tabelas.
SET GLOBAL tidb_enable_check_constraint = ON;

CREATE DATABASE IF NOT EXISTS sistema_reservas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

SELECT @@GLOBAL.tidb_enable_check_constraint AS check_constraints_ativas;
SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'sistema_reservas';
