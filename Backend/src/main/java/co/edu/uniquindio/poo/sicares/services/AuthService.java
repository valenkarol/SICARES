package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.AuthDtos.*;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.AutorizacionDatos;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.models.entities.Usuario;
import co.edu.uniquindio.poo.sicares.repository.UsuarioRepository;
import co.edu.uniquindio.poo.sicares.security.JwtService;
import co.edu.uniquindio.poo.sicares.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro, Inicio de sesión.
 * Implementa iniciarSesion(), cerrarSesion() y cambiarContrasena() de la clase Usuario del UML.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${sicares.politica-datos.version}")
    private String versionPolitica;

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }
        Beneficiario b = new Beneficiario();
        b.setCorreo(correo);
        b.setContrasenaHash(passwordEncoder.encode(req.contrasena()));

        // "Solicitar autorización e informar del tratamiento de datos" (Ley 1581 de 2012)
        AutorizacionDatos autorizacion = new AutorizacionDatos();
        autorizacion.setVersionPolitica(versionPolitica);
        autorizacion.aceptar();
        b.setAutorizacion(autorizacion);

        b.registrarAcceso();
        Usuario guardado = usuarioRepository.save(b);
        UsuarioPrincipal principal = UsuarioPrincipal.de(guardado);
        return new AuthResponse(jwtService.generarToken(principal), guardado.getId(), guardado.getCorreo(),
                guardado.getRol());
    }

    /** iniciarSesion(correo, contrasena) */
    @Transactional
    public AuthResponse iniciarSesion(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.correo().trim().toLowerCase(), req.contrasena()));
        UsuarioPrincipal principal = (UsuarioPrincipal) auth.getPrincipal();

        Usuario usuario = usuarioRepository.findById(principal.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", principal.id()));
        usuario.registrarAcceso();

        return new AuthResponse(jwtService.generarToken(principal), usuario.getId(), usuario.getCorreo(),
                usuario.getRol());
    }

    /**
     * cerrarSesion(): con JWT sin estado el cierre real ocurre en el frontend al borrar el token.
     * Se deja el método para registrar el evento y mantener la trazabilidad con el UML.
     */
    public void cerrarSesion(Long usuarioId) {
        // Punto de extensión: lista negra de tokens, auditoría, etc.
    }

    @Transactional
    public void cambiarContrasena(Long usuarioId, CambiarContrasenaRequest req) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));
        if (!passwordEncoder.matches(req.actual(), usuario.getContrasenaHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (req.actual().equals(req.nueva())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser diferente a la actual");
        }
        usuario.setContrasenaHash(passwordEncoder.encode(req.nueva()));
    }
}
