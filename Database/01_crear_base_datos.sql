-- SICARES - Creación de la base de datos y usuario de la aplicación
-- Ejecutar UNA sola vez en MySQL 8 MySQL Workbench
-- Las TABLAS las crea Hibernate automáticamente al arrancar el backend
-- (spring.jpa.hibernate.ddl-auto=update), a partir de las entidades JPA.

CREATE DATABASE IF NOT EXISTS sicares_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Usuario propio para la aplicación (recomendado en lugar de root)
CREATE USER IF NOT EXISTS 'sicares_app'@'localhost' IDENTIFIED BY 'Sicares2026*';
GRANT ALL PRIVILEGES ON sicares_db.* TO 'sicares_app'@'localhost';
FLUSH PRIVILEGES;