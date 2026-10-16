package co.edu.uniquindio.poo.sicares.dto;


import co.edu.uniquindio.poo.sicares.models.entities.Recomendacion;
import co.edu.uniquindio.poo.sicares.models.entities.RutaEducativa;

/**
 * Proceso 2 – Rutas educativas (HU-18 y HU-19). Reutiliza OfertaMapper para programa y convocatoria.
 * Conversión Entidad -> DTO. Debe llamarse dentro de una transacción (@Transactional en el service)
 * porque accede a relaciones LAZY. Los services nunca devuelven entidades al frontend.
 */
public final class RutaMapper {

    private RutaMapper() {
    }

    public static RutaDtos.RecomendacionResponse toDto(Recomendacion r) {
        return new RutaDtos.RecomendacionResponse(r.getId(), r.getOrden(), r.getPuntajeCompatibilidad(), r.getJustificacion(),
                OfertaMapper.toDto(r.getOportunidad()), OfertaMapper.toDto(r.getOportunidad().getPrograma()),
                r.getBeneficiosAplicables().stream().map(OfertaMapper::toResumen).toList());
    }

    public static RutaDtos.RutaResponse toDto(RutaEducativa r) {
        return new RutaDtos.RutaResponse(r.getId(), r.getFechaGeneracion(), r.getEstado(), r.getVersion(),
                r.getRecomendaciones().stream().map(RutaMapper::toDto).toList());
    }
}
