-- Se ejecuta automáticamente la primera vez que se crea el contenedor de
-- Postgres (docker-entrypoint-initdb.d). Crea las dos bases que usan los
-- microservicios: pedidosdb (ms-pedidos360-orders) y catalogodb
-- (ms-pedidos360-catalog). Las tablas las crea Hibernate solo
-- (ddl-auto: update) al levantar cada servicio.
CREATE DATABASE pedidosdb;
CREATE DATABASE catalogodb;
