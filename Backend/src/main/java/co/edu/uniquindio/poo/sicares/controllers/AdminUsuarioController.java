package co.edu.uniquindio.poo.sicares.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** HU-03 Gestión de roles / Administrador.gestionarUsuarios() y KPIs. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /** PATCH /api/admin/usuarios/5/activo?valor=false */
    @PatchMapping("/usuarios/{id}/activo")
    public UsuarioResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor) {
        return usuarioService.cambiarActivo(UsuarioActual.id(), id, valor);
    }

    @PostMapping("/administradores")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crearAdministrador(@Valid @RequestBody CrearAdministradorRequest req) {
        return usuarioService.crearAdministrador(req);
    }

    @GetMapping("/estadisticas")
    public EstadisticasResponse estadisticas() {
        return usuarioService.estadisticas();
    }
}

