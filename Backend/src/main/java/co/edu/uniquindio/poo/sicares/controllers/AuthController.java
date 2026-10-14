package co.edu.uniquindio.poo.sicares.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API de autenticación (HU-01 Registro, HU-02 Inicio de sesión). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.iniciarSesion(req);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.cerrarSesion(UsuarioActual.id());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest req) {
        authService.cambiarContrasena(UsuarioActual.id(), req);
        return ResponseEntity.noContent().build();
    }
}
