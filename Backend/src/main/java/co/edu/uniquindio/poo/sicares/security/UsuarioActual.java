package co.edu.uniquindio.poo.sicares.security;

import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utilidad para obtener el usuario autenticado en la petición actual. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static UsuarioPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new ReglaNegocioException("No hay un usuario autenticado");
        }
        return principal;
    }

    public static Long id() {
        return get().id();
    }
}
