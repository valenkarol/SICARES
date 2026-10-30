package co.edu.uniquindio.poo.sicares.notifications;

import co.edu.uniquindio.poo.sicares.models.entities.Postulacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;

/** Evento que se publica cada vez que una postulación cambia de estado (HU-27: eventos de actualización). */
public record PostulacionEstadoCambiadoEvent(
        Postulacion postulacion,
        EstadoPostulacion estadoAnterior,
        EstadoPostulacion estadoNuevo,
        String observacion
) {
}