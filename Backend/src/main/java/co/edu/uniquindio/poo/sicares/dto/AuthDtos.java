package co.edu.uniquindio.poo.sicares.dto;

import co.edu.uniquindio.poo.sicares.models.enums.Rol;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** DTOs de autenticación (HU-01, HU-02). */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Mínimo 8 caracteres, una mayúscula, una minúscula y un número. */
    public static final String REGEX_CONTRASENA = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$";
    public static final String MSG_CONTRASENA =
            "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número";

    public record RegistroRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Correo inválido")
            String correo,

            @NotBlank(message = "La contraseña es obligatoria")
            @Pattern(regexp = REGEX_CONTRASENA, message = MSG_CONTRASENA)
            String contrasena,

            @AssertTrue(message = "Debes aceptar la política de tratamiento de datos")
            boolean aceptaTratamientoDatos
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String correo,
            @NotBlank String contrasena
    ) {
    }

    public record CambiarContrasenaRequest(
            @NotBlank String actual,
            @NotBlank @Pattern(regexp = REGEX_CONTRASENA, message = MSG_CONTRASENA) String nueva
    ) {
    }

    public record AuthResponse(
            String token,
            Long id,
            String correo,
            Rol rol
    ) {
    }
}
