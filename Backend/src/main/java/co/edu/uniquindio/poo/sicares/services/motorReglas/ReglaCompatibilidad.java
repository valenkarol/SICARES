package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;

/**
 * ReglaCompatibilidad — patrón Strategy.
 * Cada implementación es un @Component; Spring las inyecta todas en MotorReglas.
 * Para agregar una regla nueva basta con crear otra clase que implemente esta interfaz.
 */
public interface ReglaCompatibilidad {

    ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op);

    /** Peso relativo de la regla en el puntaje final. */
    double getPeso();

    /** Si es eliminatoria y no se cumple, la oportunidad se descarta. */
    boolean esEliminatoria();
}