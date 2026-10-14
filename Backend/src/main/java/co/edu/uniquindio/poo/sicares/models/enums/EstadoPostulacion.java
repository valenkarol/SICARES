package co.edu.uniquindio.poo.sicares.models.enums;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoPostulacion {
    SELECCIONADA,
    DOCUMENTOS_PENDIENTES,
    POSTULADA,
    ADMITIDA,
    NO_ADMITIDA,
    MATRICULADA,
    CERRADA;

    /**
     * Transiciones válidas de la máquina de estados de la postulación.
     */
    public Set<EstadoPostulacion> siguientesPermitidos() {
        return switch (this) {
            case SELECCIONADA -> EnumSet.of(DOCUMENTOS_PENDIENTES, POSTULADA, CERRADA);
            case DOCUMENTOS_PENDIENTES -> EnumSet.of(POSTULADA, CERRADA);
            case POSTULADA -> EnumSet.of(ADMITIDA, NO_ADMITIDA, CERRADA);
            case ADMITIDA -> EnumSet.of(MATRICULADA, CERRADA);
            case NO_ADMITIDA, MATRICULADA, CERRADA -> EnumSet.noneOf(EstadoPostulacion.class);
        };
    }

    public boolean puedeCambiarA(EstadoPostulacion nuevo) {
        return siguientesPermitidos().contains(nuevo);
    }

    public boolean esFinal() {
        return this == NO_ADMITIDA || this == MATRICULADA || this == CERRADA;
    }
}