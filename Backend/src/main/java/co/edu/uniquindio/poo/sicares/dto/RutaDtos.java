package co.edu.uniquindio.poo.sicares.dto;


import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs del proceso 2: Planeación de rutas educativas (HU-17 a HU-19). */
public final class RutaDtos {

    private RutaDtos() {
    }

    public record RecomendacionResponse(
            Long id,
            int orden,
            double puntajeCompatibilidad,
            String justificacion,
            OfertaDtos.OportunidadResponse oportunidad,
            OfertaDtos.ProgramaResponse programa,
            List<OfertaDtos.BeneficioResumen> beneficiosAplicables
    ) {
    }

    public record RutaResponse(
            Long id,
            LocalDateTime fechaGeneracion,
            EstadoRuta estado,
            int version,
            List<RecomendacionResponse> recomendaciones
    ) {
    }
}
