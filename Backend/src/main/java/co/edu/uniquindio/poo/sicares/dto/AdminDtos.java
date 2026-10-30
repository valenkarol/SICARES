package co.edu.uniquindio.poo.sicares.dto;

import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.models.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.Map;

/** DTOs del módulo administrativo (HU-03 gestión de roles, gestionarUsuarios, KPIs). */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record UsuarioResponse(
            Long id,
            String correo,
            Rol rol,
            boolean activo,
            LocalDateTime fechaRegistro,
            LocalDateTime ultimoAcceso,
            String nombreCompleto,
            Integer porcentajePerfil
    ) {
    }

    public record CrearAdministradorRequest(
            @NotBlank @Email String correo,
            @NotBlank @Pattern(regexp = AuthDtos.REGEX_CONTRASENA, message = AuthDtos.MSG_CONTRASENA) String contrasena
    ) {
    }

    /** Indicadores (KPIs) definidos en el documento de definición del proyecto. */
    public record EstadisticasResponse(
            long totalBeneficiarios,
            long perfilesCompletos,
            double porcentajePerfilesCompletos,
            long beneficiariosConRuta,
            long totalPostulaciones,
            Map<EstadoPostulacion, Long> postulacionesPorEstado,
            long totalProgramas,
            long oportunidadesVigentes
    ) {
    }
}
