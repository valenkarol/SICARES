package co.edu.uniquindio.poo.sicares.notifications;

/**
 * Evento que se publica cada vez que el beneficiario guarda una sección de su caracterización (HU-04 a HU-11).
 * Lo escucha RegeneradorRutas para actualizar la ruta recomendada automáticamente (sin pulsar "Generar").
 *
 * @param beneficiarioId  dueño del perfil
 * @param perfilCompleto  el perfil quedó al 100 % después del cambio
 * @param completadoAhora con este cambio el perfil pasó de incompleto a completo (aviso "caracterización completa")
 */
public record PerfilActualizadoEvent(Long beneficiarioId, boolean perfilCompleto, boolean completadoAhora) {
}
