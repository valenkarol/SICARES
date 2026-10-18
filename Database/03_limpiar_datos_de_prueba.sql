-- =====================================================================================
-- SICARES - Borra de sicares_db los datos que creó la colección de Postman
-- (usuarios usuario.prueba...@correo.com, "Institución de Prueba 01", sus programas,
--  convocatorias, beneficios y los requisitos de prueba). No toca la oferta de ejemplo
-- ni los usuarios creados a mano con otros correos.
--
-- Ejecutar en MySQL Workbench con el backend DETENIDO (todo el script: rayo ⚡).
-- Si algo falla, nada se borra (ROLLBACK automático al cerrar la conexión sin COMMIT).
-- =====================================================================================
USE sicares_db;
SET SQL_SAFE_UPDATES = 0;
START TRANSACTION;

-- ---------- 1. Qué se va a borrar
DROP TEMPORARY TABLE IF EXISTS tmp_usuarios, tmp_instituciones, tmp_programas, tmp_oportunidades,
  tmp_beneficios, tmp_requisitos, tmp_rutas, tmp_recomendaciones, tmp_postulaciones, tmp_perfiles, tmp_autorizaciones;

CREATE TEMPORARY TABLE tmp_usuarios      AS SELECT id FROM usuarios WHERE correo LIKE 'usuario.prueba%@correo.com';
CREATE TEMPORARY TABLE tmp_instituciones AS SELECT id FROM instituciones WHERE nombre = 'Institución de Prueba 01';
CREATE TEMPORARY TABLE tmp_programas     AS SELECT id FROM programas_educativos WHERE institucion_id IN (SELECT id FROM tmp_instituciones);
CREATE TEMPORARY TABLE tmp_oportunidades AS SELECT id FROM oportunidades_educativas WHERE programa_id IN (SELECT id FROM tmp_programas);
CREATE TEMPORARY TABLE tmp_beneficios    AS SELECT id FROM beneficios WHERE institucion_id IN (SELECT id FROM tmp_instituciones);
CREATE TEMPORARY TABLE tmp_requisitos    AS SELECT id FROM requisitos
                                            WHERE nombre IN ('Nivel educativo mínimo: bachiller', 'Exige título de tecnólogo');
CREATE TEMPORARY TABLE tmp_rutas         AS SELECT id FROM rutas_educativas WHERE beneficiario_id IN (SELECT id FROM tmp_usuarios);
CREATE TEMPORARY TABLE tmp_recomendaciones AS SELECT id FROM recomendaciones
                                              WHERE ruta_id IN (SELECT id FROM tmp_rutas) OR oportunidad_id IN (SELECT id FROM tmp_oportunidades);
CREATE TEMPORARY TABLE tmp_postulaciones AS SELECT id FROM postulaciones
                                            WHERE beneficiario_id IN (SELECT id FROM tmp_usuarios)
                                               OR oportunidad_id IN (SELECT id FROM tmp_oportunidades)
                                               OR recomendacion_id IN (SELECT id FROM tmp_recomendaciones);
CREATE TEMPORARY TABLE tmp_perfiles AS
SELECT pe.id, pe.informacion_personal_id, pe.situacion_victima_id, pe.situacion_socioeconomica_id,
       pe.condicion_accesibilidad_id, pe.trayectoria_educativa_id
FROM perfiles_educativos pe JOIN beneficiarios b ON b.perfil_id = pe.id
WHERE b.usuario_id IN (SELECT id FROM tmp_usuarios);
CREATE TEMPORARY TABLE tmp_autorizaciones AS SELECT autorizacion_id AS id FROM beneficiarios
                                             WHERE usuario_id IN (SELECT id FROM tmp_usuarios) AND autorizacion_id IS NOT NULL;

SELECT (SELECT COUNT(*) FROM tmp_usuarios) AS usuarios, (SELECT COUNT(*) FROM tmp_instituciones) AS instituciones,
       (SELECT COUNT(*) FROM tmp_programas) AS programas, (SELECT COUNT(*) FROM tmp_oportunidades) AS convocatorias,
       (SELECT COUNT(*) FROM tmp_beneficios) AS beneficios, (SELECT COUNT(*) FROM tmp_requisitos) AS requisitos,
       (SELECT COUNT(*) FROM tmp_rutas) AS rutas;

-- ---------- 2. Postulaciones y lo que depende de ellas (proceso 3)
DELETE FROM documentos_postulacion WHERE postulacion_id IN (SELECT id FROM tmp_postulaciones);
DELETE FROM documentos_postulacion WHERE requisito_id IN (SELECT id FROM tmp_requisitos);
DELETE FROM historial_estados      WHERE postulacion_id IN (SELECT id FROM tmp_postulaciones);
DELETE FROM recordatorios          WHERE postulacion_id IN (SELECT id FROM tmp_postulaciones);
DELETE FROM notificaciones         WHERE postulacion_id IN (SELECT id FROM tmp_postulaciones);
DELETE FROM notificaciones         WHERE usuario_id IN (SELECT id FROM tmp_usuarios);
DELETE FROM postulaciones          WHERE id IN (SELECT id FROM tmp_postulaciones);

-- ---------- 3. Rutas educativas
DELETE FROM recomendaciones  WHERE id IN (SELECT id FROM tmp_recomendaciones);
DELETE FROM rutas_educativas WHERE id IN (SELECT id FROM tmp_rutas);

-- ---------- 4. Oferta educativa de prueba
DELETE FROM programa_requisito  WHERE programa_id  IN (SELECT id FROM tmp_programas);
DELETE FROM programa_requisito  WHERE requisito_id IN (SELECT id FROM tmp_requisitos);
DELETE FROM beneficio_programa  WHERE beneficio_id IN (SELECT id FROM tmp_beneficios);
DELETE FROM beneficio_programa  WHERE programa_id  IN (SELECT id FROM tmp_programas);
DELETE FROM beneficio_requisito WHERE beneficio_id IN (SELECT id FROM tmp_beneficios);
DELETE FROM beneficio_requisito WHERE requisito_id IN (SELECT id FROM tmp_requisitos);
DELETE FROM oportunidades_educativas WHERE id IN (SELECT id FROM tmp_oportunidades);
DELETE FROM beneficios               WHERE id IN (SELECT id FROM tmp_beneficios);
DELETE FROM programas_educativos     WHERE id IN (SELECT id FROM tmp_programas);
DELETE FROM requisitos               WHERE id IN (SELECT id FROM tmp_requisitos);
DELETE FROM instituciones            WHERE id IN (SELECT id FROM tmp_instituciones);

-- ---------- 5. Usuarios de prueba y su caracterización
DELETE FROM beneficiarios           WHERE usuario_id IN (SELECT id FROM tmp_usuarios);
DELETE FROM intereses_profesionales WHERE perfil_id IN (SELECT id FROM tmp_perfiles);
DELETE FROM perfiles_educativos     WHERE id IN (SELECT id FROM tmp_perfiles);
DELETE FROM estudios_realizados     WHERE trayectoria_id IN (SELECT trayectoria_educativa_id FROM tmp_perfiles);
DELETE FROM trayectorias_educativas WHERE id IN (SELECT trayectoria_educativa_id FROM tmp_perfiles);
DELETE FROM informacion_personal    WHERE id IN (SELECT informacion_personal_id FROM tmp_perfiles);
DELETE FROM situacion_victima       WHERE id IN (SELECT situacion_victima_id FROM tmp_perfiles);
DELETE FROM situacion_socioeconomica WHERE id IN (SELECT situacion_socioeconomica_id FROM tmp_perfiles);
DELETE FROM condicion_accesibilidad WHERE id IN (SELECT condicion_accesibilidad_id FROM tmp_perfiles);
DELETE FROM autorizaciones_datos    WHERE id IN (SELECT id FROM tmp_autorizaciones);
DELETE FROM usuarios                WHERE id IN (SELECT id FROM tmp_usuarios);

COMMIT;
SET SQL_SAFE_UPDATES = 1;
SELECT 'Datos de prueba de Postman eliminados' AS resultado;
