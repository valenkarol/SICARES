package co.edu.uniquindio.poo.sicares.exceptions;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato estándar de error que recibe el frontend. */
public record ApiError(
        LocalDateTime fecha,
        int estado,
        String error,
        String mensaje,
        Map<String, String> campos
) {
    public static ApiError of(int estado, String error, String mensaje) {
        return new ApiError(LocalDateTime.now(), estado, error, mensaje, Map.of());
    }
}
