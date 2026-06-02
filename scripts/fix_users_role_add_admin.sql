-- Ampliar rol permitido en users tras anadir Role.ADMIN en la aplicacion.
-- Hibernate ddl-auto=update no suele modificar CHECK existentes en PostgreSQL.
--
-- Ejecutar una vez (ajusta usuario/host si hace falta), por ejemplo:
--   psql -h localhost -U TU_USUARIO -d MercadoLibre -f scripts/fix_users_role_add_admin.sql

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('SELLER', 'ADMIN'));
