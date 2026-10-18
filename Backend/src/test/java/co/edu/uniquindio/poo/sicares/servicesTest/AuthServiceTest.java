package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.AuthDtos.AuthResponse;
import co.edu.uniquindio.poo.sicares.dto.AuthDtos.CambiarContrasenaRequest;
import co.edu.uniquindio.poo.sicares.dto.AuthDtos.LoginRequest;
import co.edu.uniquindio.poo.sicares.dto.AuthDtos.RegistroRequest;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Administrador;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.models.entities.Usuario;
import co.edu.uniquindio.poo.sicares.models.enums.Rol;
import co.edu.uniquindio.poo.sicares.repository.UsuarioRepository;
import co.edu.uniquindio.poo.sicares.security.JwtService;
import co.edu.uniquindio.poo.sicares.security.UsuarioPrincipal;
import co.edu.uniquindio.poo.sicares.services.AuthService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de AuthService — HU-01 Registro, HU-02 Inicio de sesión y HU-03 Roles.
 * Casos de TestRail relacionados: CP-01, CP-02, CP-03, CP-04 y CP-05.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CORREO = "usuario.prueba01@correo.com";
    private static final String CONTRASENA = "Prueba#2026";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        // @Value no se inyecta en una prueba unitaria: se asigna a mano
        ReflectionTestUtils.setField(authService, "versionPolitica", "1.0");
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ------------------------------------------------------------------ HU-01 Registro

    @Test
    @DisplayName("CP-01: registra un beneficiario con datos válidos, autorización aceptada y token")
    void registroConDatosValidos() {
        when(usuarioRepository.existsByCorreoIgnoreCase(CORREO)).thenReturn(false);
        when(passwordEncoder.encode(CONTRASENA)).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generarToken(any(UsuarioPrincipal.class))).thenReturn("token-jwt");

        // El correo llega con espacios y mayúsculas: se debe normalizar
        AuthResponse r = authService.registrar(new RegistroRequest("  Usuario.Prueba01@Correo.com ", CONTRASENA, true));

        assertEquals("token-jwt", r.token());
        assertEquals(1L, r.id());
        assertEquals(CORREO, r.correo());
        assertEquals(Rol.BENEFICIARIO, r.rol());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Beneficiario guardado = (Beneficiario) captor.getValue();
        assertEquals("hash-seguro", guardado.getContrasenaHash(), "La contraseña se guarda cifrada");
        assertTrue(guardado.getAutorizacion().isAceptada(), "Se guarda la autorización de datos (Ley 1581)");
        assertEquals("1.0", guardado.getAutorizacion().getVersionPolitica());
        assertNotNull(guardado.getAutorizacion().getFechaAceptacion());
    }

    @Test
    @DisplayName("CP-02: rechaza el registro con un correo ya registrado y no guarda nada")
    void registroCorreoDuplicado() {
        when(usuarioRepository.existsByCorreoIgnoreCase(CORREO)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> authService.registrar(new RegistroRequest(CORREO, CONTRASENA, true)));

        assertEquals("Ya existe una cuenta con ese correo", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-02: un correo sin formato válido no pasa la validación")
    void registroCorreoInvalido() {
        Set<ConstraintViolation<RegistroRequest>> errores =
                validator.validate(new RegistroRequest("usuario.com", CONTRASENA, true));

        assertTrue(errores.stream().anyMatch(v -> v.getPropertyPath().toString().equals("correo")));
    }

    @Test
    @DisplayName("HU-01: una contraseña débil no pasa la validación")
    void registroContrasenaDebil() {
        Set<ConstraintViolation<RegistroRequest>> errores =
                validator.validate(new RegistroRequest(CORREO, "abc12345", true));

        assertTrue(errores.stream().anyMatch(v -> v.getPropertyPath().toString().equals("contrasena")));
    }

    @Test
    @DisplayName("HU-01: sin aceptar el tratamiento de datos no se puede registrar")
    void registroSinAceptarPolitica() {
        Set<ConstraintViolation<RegistroRequest>> errores =
                validator.validate(new RegistroRequest(CORREO, CONTRASENA, false));

        assertTrue(errores.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("aceptaTratamientoDatos")));
    }

    @Test
    @DisplayName("CP-01: datos válidos no generan errores de validación")
    void registroSinErroresDeValidacion() {
        assertTrue(validator.validate(new RegistroRequest(CORREO, CONTRASENA, true)).isEmpty());
    }

    // ------------------------------------------------------------------ HU-02 Inicio de sesión

    @Test
    @DisplayName("CP-03: inicia sesión con credenciales válidas y devuelve el token")
    void loginValido() {
        UsuarioPrincipal principal = new UsuarioPrincipal(5L, CORREO, "hash", "BENEFICIARIO", true);
        Authentication autenticado =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(autenticado);

        Beneficiario b = new Beneficiario();
        b.setId(5L);
        b.setCorreo(CORREO);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(b));
        when(jwtService.generarToken(principal)).thenReturn("token-jwt");

        AuthResponse r = authService.iniciarSesion(new LoginRequest(" USUARIO.prueba01@correo.com", CONTRASENA));

        assertEquals("token-jwt", r.token());
        assertEquals(Rol.BENEFICIARIO, r.rol());
        assertNotNull(b.getUltimoAcceso(), "Se registra la fecha del último acceso");

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals(CORREO, captor.getValue().getName(), "El correo se normaliza antes de autenticar");
    }

    @Test
    @DisplayName("CP-04: con credenciales incorrectas no da acceso ni genera token")
    void loginCredencialesIncorrectas() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.iniciarSesion(new LoginRequest(CORREO, "Incorrecta#1")));
        verify(jwtService, never()).generarToken(any());
    }

    // ------------------------------------------------------------------ HU-03 Roles

    @Test
    @DisplayName("CP-05: un beneficiario solo tiene la autoridad ROLE_BENEFICIARIO")
    void rolBeneficiario() {
        Beneficiario b = new Beneficiario();
        b.setId(1L);
        b.setCorreo(CORREO);
        b.setContrasenaHash("hash");

        List<String> autoridades = UsuarioPrincipal.de(b).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertEquals(List.of("ROLE_BENEFICIARIO"), autoridades);
        assertFalse(autoridades.contains("ROLE_ADMINISTRADOR"));
    }

    @Test
    @DisplayName("HU-03: un administrador tiene la autoridad ROLE_ADMINISTRADOR")
    void rolAdministrador() {
        Administrador a = new Administrador();
        a.setId(2L);
        a.setCorreo("admin@sicares.co");
        a.setContrasenaHash("hash");

        assertEquals("ROLE_ADMINISTRADOR",
                UsuarioPrincipal.de(a).getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("HU-03: una cuenta inactiva queda deshabilitada para iniciar sesión")
    void cuentaInactiva() {
        Beneficiario b = new Beneficiario();
        b.setId(3L);
        b.setCorreo(CORREO);
        b.setContrasenaHash("hash");
        b.setActivo(false);

        assertFalse(UsuarioPrincipal.de(b).isEnabled());
    }

    // ------------------------------------------------------------------ Cambio de contraseña

    @Test
    @DisplayName("No cambia la contraseña si la actual es incorrecta")
    void cambiarContrasenaActualIncorrecta() {
        Beneficiario b = new Beneficiario();
        b.setContrasenaHash("hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(b));
        when(passwordEncoder.matches("Mala#2026", "hash")).thenReturn(false);

        assertThrows(ReglaNegocioException.class,
                () -> authService.cambiarContrasena(1L, new CambiarContrasenaRequest("Mala#2026", "Nueva#2026")));
        assertEquals("hash", b.getContrasenaHash());
    }

    @Test
    @DisplayName("No permite que la nueva contraseña sea igual a la actual")
    void cambiarContrasenaIgual() {
        Beneficiario b = new Beneficiario();
        b.setContrasenaHash("hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(b));
        when(passwordEncoder.matches(CONTRASENA, "hash")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> authService.cambiarContrasena(1L, new CambiarContrasenaRequest(CONTRASENA, CONTRASENA)));
    }
}
