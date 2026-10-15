package co.edu.uniquindio.poo.sicares.security;

import co.edu.uniquindio.poo.sicares.models.entities.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Adaptador de Usuario para Spring Security. Rol -> autoridad "ROLE_BENEFICIARIO" / "ROLE_ADMINISTRADOR". */
public record UsuarioPrincipal(Long id, String correo, String contrasenaHash, String rol, boolean activo)
        implements UserDetails {

    public static UsuarioPrincipal de(Usuario u) {
        return new UsuarioPrincipal(u.getId(), u.getCorreo(), u.getContrasenaHash(), u.getRol().name(), u.isActivo());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
