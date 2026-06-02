-- =============================================================================
-- Arreglo UNIQUE de categorías (ejecutar UNA VEZ en la base PostgreSQL del proyecto)
-- =============================================================================
-- Problema: una restricción UNIQUE solo sobre "name" impide que dos vendedores
-- tengan categorías con el mismo nombre (ej. "Electronicos").
-- Solución: borrar esa restricción y dejar UNIQUE (owner_id, name).
--
-- Conexión típica (ajusta usuario/host si hace falta):
--   PGPASSWORD='tu_password' psql -h localhost -p 5432 -U andrew -d MercadoLibre -f fix_category_unique_constraint.sql
-- O pega este archivo en pgAdmin / DBeaver y ejecútalo contra la base MercadoLibre.
-- =============================================================================

-- Nombres habituales de la restricción "solo nombre" (Hibernate vs PostgreSQL por defecto)
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uk_category_name;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;

-- Por si Hibernate ya había creado la restricción compuesta y quieres recrearla limpia
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uk_category_owner_name;

-- Restricción correcta: mismo nombre permitido entre distintos dueños, no duplicado por el mismo dueño
ALTER TABLE categories
    ADD CONSTRAINT uk_category_owner_name UNIQUE (owner_id, name);
