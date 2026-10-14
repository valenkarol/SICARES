package co.edu.uniquindio.poo.sicares.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Bandeja de notificaciones del usuario autenticado (cualquier rol). */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<NotificacionResponse> listar() {
        return usuarioService.notificaciones(UsuarioActual.id());
    }

    @GetMapping("/no-leidas")
    public Map<String, Long> noLeidas() {
        return Map.of("total", usuarioService.notificacionesNoLeidas(UsuarioActual.id()));
    }

    @PatchMapping("/{id}/leida")
    public NotificacionResponse marcarLeida(@PathVariable Long id) {
        return usuarioService.marcarLeida(UsuarioActual.id(), id);
    }
}
