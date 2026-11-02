-- =====================================================================================
-- SICARES · Consultas de verificación de la revisión de RNF (correr en MySQL Workbench o
-- en el servidor con:  sudo mysql sicares_db < 04_verificar_rnf.sql)
-- =====================================================================================

-- RNF-13
-- si esta consulta devuelve filas, hay personas duplicadas y la restricción NO se podrá crear.
-- Borren o corrijan esos perfiles de prueba y vuelvan a correrla hasta que salga vacía.
SELECT numero_documento, COUNT(*) AS veces
FROM informacion_personal
GROUP BY numero_documento
HAVING COUNT(*) > 1;

-- RNF-13(evidencia de "0 duplicados")
SELECT CONSTRAINT_NAME, TABLE_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'uk_info_numero_documento';

-- RNF-14 · Recomendaciones generadas después del cambio (deben tener huella de 64 caracteres)
SELECT COUNT(*) AS recomendaciones_con_huella FROM recomendaciones WHERE huella_oferta IS NOT NULL;

-- RNF-14 · Prueba de alteración controlada (hacerla en local o en la base de pruebas, NO en producción):
--   1) Generar la ruta de un beneficiario de prueba.
--   2) Alterar un dato de la oferta, por ejemplo:
--        UPDATE programas_educativos SET municipio = 'Otro municipio' WHERE id = <id del programa>;
--   3) Abrir "Mi ruta": debe aparecer DESACTUALIZADA y en el log del backend "RNF-14: alteración detectada".
--   4) Devolver el dato a su valor original.

-- KPIs · Tiempos guardados por los cambios nuevos
SELECT COUNT(*) AS perfiles_con_fecha_completado FROM perfiles_educativos WHERE fecha_completado IS NOT NULL;
SELECT ROUND(AVG(duracion_generacion_ms), 1) AS ms_promedio_generacion_ruta
FROM rutas_educativas WHERE duracion_generacion_ms IS NOT NULL;