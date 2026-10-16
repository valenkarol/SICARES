package co.edu.uniquindio.poo.sicares.dto;

import co.edu.uniquindio.poo.sicares.models.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**DTOs de la oferta educativa**/
public final class OfertaDtos {

    private OfertaDtos() {
    }

    public record InstitucionDto(
            Long id,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
            @NotBlank @Pattern(regexp = "[0-9-]{6,15}", message = "NIT inválido") String nit,
            @NotNull TipoInstitucion tipo,
            @NotBlank String departamento,
            @NotBlank String municipio,
            @Email String correoContacto,
            Integer totalProgramas
    ) {
    }

    public record RequisitoDto(
            Long id,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull TipoRequisito tipo,
            boolean obligatorio,
            @Size(max = 100) String valorEsperado,
            boolean requiereDocumento
    ) {
    }

    public record BeneficioResumen(
            Long id,
            String nombre,
            TipoBeneficio tipo,
            double porcentajeCobertura,
            boolean exclusivoVictimas,
            LocalDate fechaLimite
    ) {
    }

    public record ProgramaRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull NivelEducativo nivel,
            @NotNull Modalidad modalidad,
            @NotBlank String areaConocimiento,
            @Min(1) int duracionPeriodos,
            @NotNull @PositiveOrZero BigDecimal costoPeriodo,
            @NotBlank String municipio,
            boolean activo,
            @NotNull(message = "La institución es obligatoria") Long institucionId,
            Set<Long> requisitoIds
    ) {
    }

    public record ProgramaResponse(
            Long id,
            String nombre,
            NivelEducativo nivel,
            Modalidad modalidad,
            String areaConocimiento,
            int duracionPeriodos,
            BigDecimal costoPeriodo,
            String municipio,
            boolean activo,
            boolean gratuito,
            Long institucionId,
            String institucionNombre,
            TipoInstitucion institucionTipo,
            List<RequisitoDto> requisitos,
            List<BeneficioResumen> beneficios
    ) {
    }

    public record BeneficioRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull TipoBeneficio tipo,
            @Size(max = 500) String descripcion,
            @DecimalMin("0") @DecimalMax("100") double porcentajeCobertura,
            boolean exclusivoVictimas,
            LocalDate fechaLimite,
            @NotNull(message = "La institución es obligatoria") Long institucionId,
            Set<Long> programaIds,
            Set<Long> requisitoIds
    ) {
    }

    public record BeneficioResponse(
            Long id,
            String nombre,
            TipoBeneficio tipo,
            String descripcion,
            double porcentajeCobertura,
            boolean exclusivoVictimas,
            LocalDate fechaLimite,
            boolean vigente,
            Long institucionId,
            String institucionNombre,
            List<Long> programaIds,
            List<RequisitoDto> requisitos
    ) {
    }

    public record OportunidadRequest(
            @NotBlank(message = "El periodo es obligatorio (ej. 2027-1)") String periodoAcademico,
            @NotNull LocalDate fechaAperturaInscripcion,
            @NotNull LocalDate fechaCierreInscripcion,
            LocalDate fechaAdmision,
            LocalDate fechaMatricula,
            @Min(0) int cupos,
            @NotNull(message = "El programa es obligatorio") Long programaId
    ) {
    }

    public record OportunidadResponse(
            Long id,
            String periodoAcademico,
            LocalDate fechaAperturaInscripcion,
            LocalDate fechaCierreInscripcion,
            LocalDate fechaAdmision,
            LocalDate fechaMatricula,
            int cupos,
            boolean vigente,
            int diasParaCierre,
            Long programaId,
            String programaNombre,
            String institucionNombre,
            NivelEducativo nivel,
            Modalidad modalidad,
            String municipio
    ) {
    }
}
