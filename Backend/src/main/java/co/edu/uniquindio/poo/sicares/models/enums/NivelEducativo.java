package co.edu.uniquindio.poo.sicares.models.enums;

public enum NivelEducativo {
    NINGUNO,
    BASICA_PRIMARIA,
    BASICA_SECUNDARIA,
    MEDIA,
    TECNICO,
    TECNOLOGICO,
    UNIVERSITARIO;

    public boolean esMayorOIgualQue(NivelEducativo otro) {
        return this.ordinal() >= otro.ordinal();
    }

    /**
     * Nivel mínimo que debe tener aprobado una persona para ingresar a este nivel.
     * Técnico (SENA) admite personas con básica secundaria (grado 9°).
     */
    public NivelEducativo nivelPrevioRequerido() {
        return switch (this) {
            case NINGUNO, BASICA_PRIMARIA -> NINGUNO;
            case BASICA_SECUNDARIA -> BASICA_PRIMARIA;
            case MEDIA, TECNICO -> BASICA_SECUNDARIA;
            case TECNOLOGICO, UNIVERSITARIO -> MEDIA;
        };
    }
}
