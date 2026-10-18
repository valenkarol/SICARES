package co.edu.uniquindio.poo.sicares.dto;


import co.edu.uniquindio.poo.sicares.models.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs del proceso 1: Caracterización del usuario (HU-04 a HU-11). */
public final class CaracterizacionDtos {

    private CaracterizacionDtos() {
    }

    /** HU-04. "edad" solo se llena en las respuestas (se ignora al guardar). */
    public record InformacionPersonalDto(
            @NotBlank(message = "Los nombres son obligatorios") @Size(max = 80) String nombres,
            @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 80) String apellidos,
            @NotNull(message = "El tipo de documento es obligatorio") TipoDocumento tipoDocumento,
            @NotBlank @Pattern(regexp = "\\d{6,12}", message = "El documento debe tener entre 6 y 12 dígitos")
            String numeroDocumento,
            @NotNull(message = "La fecha de nacimiento es obligatoria") @Past LocalDate fechaNacimiento,
            @Size(max = 30) String genero,
            @Size(max = 50) String etnia,
            @Pattern(regexp = "^$|\\d{7,10}", message = "Teléfono inválido") String telefono,
            @NotBlank(message = "El departamento es obligatorio") String departamento,
            @NotBlank(message = "El municipio es obligatorio") String municipio,
            @NotNull(message = "La zona es obligatoria") Zona zona,
            Integer edad,
            @Valid ResponsableMenorDto responsable
    ) {
    }

    /** Adulto responsable, obligatorio cuando el beneficiario es menor de 18 años. */
    public record ResponsableMenorDto(
            @NotBlank(message = "Los nombres del responsable son obligatorios") @Size(max = 80) String nombres,
            @NotBlank(message = "Los apellidos del responsable son obligatorios") @Size(max = 80) String apellidos,
            @NotNull(message = "El tipo de documento del responsable es obligatorio") TipoDocumento tipoDocumento,
            @NotBlank @Pattern(regexp = "\\d{6,12}", message = "El documento del responsable debe tener entre 6 y 12 dígitos")
            String numeroDocumento,
            @NotNull(message = "La fecha de nacimiento del responsable es obligatoria") @Past LocalDate fechaNacimiento,
            @NotBlank(message = "El parentesco es obligatorio") @Size(max = 40) String parentesco,
            @NotBlank(message = "El teléfono del responsable es obligatorio")
            @Pattern(regexp = "\\d{7,10}", message = "Teléfono del responsable inválido") String telefono,
            @Email(message = "Correo del responsable inválido") @Size(max = 120) String correo
    ) {
    }

    /** HU-05 */
    public record SituacionVictimaDto(
            boolean esVictima,
            boolean inscritoRUV,
            @Size(max = 100) String hechoVictimizante,
            @PastOrPresent LocalDate fechaHecho,
            @Size(max = 60) String municipioExpulsor
    ) {
    }

    /** HU-06 */
    public record SituacionSocioeconomicaDto(
            @Size(max = 10) String grupoSisben,
            @Min(value = 1, message = "Estrato entre 1 y 6") @Max(value = 6, message = "Estrato entre 1 y 6") int estrato,
            @PositiveOrZero BigDecimal ingresoMensualHogar,
            @Size(max = 50) String situacionLaboral,
            @Min(value = 1, message = "El hogar debe tener al menos 1 integrante") int integrantesHogar,
            @Min(0) int personasACargo
    ) {
    }

    /** HU-09 */
    public record CondicionAccesibilidadDto(
            boolean tieneInternet,
            boolean tieneDispositivos,
            @Size(max = 30) String medioTransporte,
            @PositiveOrZero double distanciaMaximaKm,
            Modalidad modalidadPreferida
    ) {
    }

    public record EstudioRealizadoDto(
            Long id,
            @NotNull NivelEducativo nivel,
            @Size(max = 150) String institucion,
            @Size(max = 150) String titulo,
            @NotNull EstadoEstudio estado,
            Integer anioInicio,
            Integer anioFin
    ) {
    }

    /** HU-07 */
    public record TrayectoriaEducativaDto(
            @NotNull(message = "El nivel actual es obligatorio") NivelEducativo nivelActual,
            @Min(0) @Max(11) int ultimoGradoAprobado,
            @Min(1950) int anioUltimoEstudio,
            @Valid List<EstudioRealizadoDto> estudios
    ) {
    }

    public record InteresProfesionalDto(
            @NotBlank(message = "El área es obligatoria") String areaConocimiento,
            @Size(max = 255) String descripcion
    ) {
    }

    /** HU-08 */
    public record InteresesRequest(
            @NotEmpty(message = "Selecciona al menos un interés") @Valid List<InteresProfesionalDto> intereses
    ) {
    }

    /** HU-10: perfil completo del beneficiario. */
    public record PerfilResponse(
            Long id,
            String correo,
            boolean completo,
            int porcentajeCompletitud,
            LocalDateTime fechaActualizacion,
            boolean autorizacionAceptada,
            InformacionPersonalDto informacionPersonal,
            SituacionVictimaDto situacionVictima,
            SituacionSocioeconomicaDto situacionSocioeconomica,
            CondicionAccesibilidadDto condicionAccesibilidad,
            TrayectoriaEducativaDto trayectoriaEducativa,
            List<InteresProfesionalDto> intereses
    ) {
    }
}
