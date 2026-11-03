package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.notifications.EnviadorCorreo;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.security.UsuarioActual;
import co.edu.uniquindio.poo.sicares.services.PostulacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** HU-25 Actualizar estado (Administrador.actualizarEstadoPostulacion) y validación de documentos. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPostulacionController {

    private final PostulacionService postulacionService;
    private final ServicioNotificaciones servicioNotificaciones;
    private final EnviadorCorreo enviadorCorreo;

    @GetMapping("/postulaciones")
    public List<PostulacionResponse> listar(@RequestParam(required = false) EstadoPostulacion estado) {
        return postulacionService.listarTodas(estado);
    }

    @GetMapping("/postulaciones/{id}")
    public PostulacionResponse obtener(@PathVariable Long id) {
        return postulacionService.obtener(id);
    }

    @PatchMapping("/postulaciones/{id}/estado")
    public PostulacionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest req) {
        return postulacionService.actualizarEstado(UsuarioActual.id(), id, req);
    }

    @PatchMapping("/documentos/{docId}/estado")
    public PostulacionResponse cambiarEstadoDocumento(@PathVariable Long docId,
                                                      @Valid @RequestBody CambioEstadoDocumentoRequest req) {
        return postulacionService.cambiarEstadoDocumento(docId, req.estado());
    }

    @GetMapping("/documentos/{docId}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long docId) {
        return PostulacionController.respuestaArchivo(postulacionService.descargarDocumento(docId));
    }

    /** Ejecuta manualmente el envío de recordatorios pendientes (útil para pruebas/demostración). */
    @PostMapping("/recordatorios/procesar")
    public Map<String, Integer> procesarRecordatorios() {
        return Map.of("enviados", servicioNotificaciones.procesarRecordatoriosPendientes());
    }

    /**
     * Diagnóstico del correo (HU-26): envía un correo de prueba y devuelve el error real si Gmail lo rechaza.
     * Si no se indica destino, se envía al correo del administrador que lo pide.
     */
    @PostMapping("/correo/prueba")
    public EnviadorCorreo.ResultadoPrueba probarCorreo(@RequestParam(required = false) String para) {
        String destino = (para == null || para.isBlank()) ? UsuarioActual.get().correo() : para.trim();
        return enviadorCorreo.probar(destino);
    }
}
