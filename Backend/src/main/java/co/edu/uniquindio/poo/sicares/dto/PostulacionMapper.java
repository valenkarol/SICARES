package co.edu.uniquindio.poo.sicares.dto;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.*;
import co.edu.uniquindio.poo.sicares.models.entities.*;

import java.util.List;

/**
 * Postulación y seguimiento .
 * Conversión Entidad - DTO. Debe llamarse dentro de una transacción
 */
public final class PostulacionMapper {

    private PostulacionMapper() {
    }

    public static DocumentoResponse toDto(DocumentoPostulacion d) {
        Requisito r = d.getRequisito();
        return new DocumentoResponse(d.getId(), d.getNombreArchivo(), d.getTipoArchivo(), d.getTamanoBytes(),
                d.getFechaCarga(), d.getEstado(), r != null ? r.getId() : null, r != null ? r.getNombre() : null);
    }

    public static HistorialResponse toDto(HistorialEstado h) {
        return new HistorialResponse(h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFechaCambio(),
                h.getObservacion(), h.getResponsable() != null ? h.getResponsable().getCorreo() : "Sistema");
    }

    public static RecordatorioResponse toDto(Recordatorio r) {
        return new RecordatorioResponse(r.getId(), r.getTitulo(), r.getMensaje(), r.getFechaEvento(),
                r.getFechaProgramada(), r.isEnviado(), r.getPostulacion().getId());
    }

    public static PostulacionResponse toDto(Postulacion p) {
        ProgramaEducativo programa = p.getOportunidad().getPrograma();
        return new PostulacionResponse(p.getId(), p.getFechaSeleccion(), p.getFechaPostulacion(), p.getEstado(),
                p.estaFinalizada(), p.getBeneficiario().getId(), p.getBeneficiario().getCorreo(),
                OfertaMapper.toDto(p.getOportunidad()),
                programa.getRequisitos().stream().map(OfertaMapper::toDto).toList(),
                p.documentosPendientes().stream().map(OfertaMapper::toDto).toList(),
                p.getDocumentos().stream().map(PostulacionMapper::toDto).toList(),
                p.getHistorial().stream().map(PostulacionMapper::toDto).toList(),
                p.getRecordatorios().stream().map(PostulacionMapper::toDto).toList(),
                List.copyOf(p.getEstado().siguientesPermitidos()));
    }

    public static NotificacionResponse toDto(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getAsunto(), n.getMensaje(), n.getCanal(), n.getFechaEnvio(),
                n.isLeida(), n.getPostulacion() != null ? n.getPostulacion().getId() : null);
    }
}
