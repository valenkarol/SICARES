package co.edu.uniquindio.poo.sicares.services.motorReglas;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** Resultado de aplicar una regla a un perfil frente a una oportunidad. */
@Getter
public class ResultadoEvaluacion {

    private final boolean cumple;
    /** Entre 0.0 y 1.0 para una regla; entre 0 y 100 para el resultado del motor. */
    private final double puntaje;
    private final List<String> observaciones;

    public ResultadoEvaluacion(boolean cumple, double puntaje, List<String> observaciones) {
        this.cumple = cumple;
        this.puntaje = puntaje;
        this.observaciones = new ArrayList<>(observaciones);
    }

    public static ResultadoEvaluacion cumple(double puntaje, String... observaciones) {
        return new ResultadoEvaluacion(true, puntaje, List.of(observaciones));
    }

    public static ResultadoEvaluacion noCumple(String... observaciones) {
        return new ResultadoEvaluacion(false, 0, List.of(observaciones));
    }
}
