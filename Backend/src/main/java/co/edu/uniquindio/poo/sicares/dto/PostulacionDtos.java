package co.edu.uniquindio.poo.sicares.dto;

import co.edu.uniquindio.poo.sicares.models.enums.CanalNotificacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoDocumento;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de Postulación y seguimiento. */
public final class PostulacionDtos {

    private PostulacionDtos() {
    }

    /** HU-20 */
    public record SeleccionarOportunidadRequest(
            @NotNull(message = "Debes indicar la recomendación") Long recomendacionId
    ) {
    }

    /** HU-25 */
    public record CambioEstadoRequest(
            @NotNull(message = "El nuevo estado es obligatorio") EstadoPostulacion estado,
            @Size(max = 500) String observacion
    ) {
    }

    public record CambioEstadoDocumentoRequest(
            @NotNull EstadoDocumento estado
    ) {
    }

    public record DocumentoResponse(
            Long id,
            String nombreArchivo,
            String tipoArchivo,
            long tamanoBytes,
            LocalDateTime fechaCarga,
            EstadoDocumento estado,
            Long requisitoId,
            String requisitoNombre
    ) {
    }

    public record HistorialResponse(
            Long id,
            EstadoPostulacion estadoAnterior,
            EstadoPostulacion estadoNuevo,
            LocalDateTime fechaCambio,
            String observacion,
            String responsable
    ) {
    }

    public record RecordatorioResponse(
            Long id,
            String titulo,
            String mensaje,
            LocalDate fechaEvento,
            LocalDateTime fechaProgramada,
            boolean enviado,
            Long postulacionId
    ) {
    }

    public record PostulacionResponse(
            Long id,
            LocalDateTime fechaSeleccion,
            LocalDateTime fechaPostulacion,
            EstadoPostulacion estado,
            boolean finalizada,
            Long beneficiarioId,
            String beneficiarioCorreo,
            OfertaDtos.OportunidadResponse oportunidad,
            List<OfertaDtos.RequisitoDto> requisitos,
            List<OfertaDtos.RequisitoDto> documentosPendientes,
            List<DocumentoResponse> documentos,
            List<HistorialResponse> historial,
            List<RecordatorioResponse> recordatorios,
            List<EstadoPostulacion> siguientesEstados
    ) {
    }

    public record NotificacionResponse(
            Long id,
            String asunto,
            String mensaje,
            CanalNotificacion canal,
            LocalDateTime fechaEnvio,
            boolean leida,
            Long postulacionId
    ) {
    }
}
