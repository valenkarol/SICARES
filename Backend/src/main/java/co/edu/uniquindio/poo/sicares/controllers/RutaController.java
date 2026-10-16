package co.edu.uniquindio.poo.sicares.controllers;


import co.edu.uniquindio.poo.sicares.dto.RutaDtos;
import co.edu.uniquindio.poo.sicares.security.UsuarioActual;
import co.edu.uniquindio.poo.sicares.services.RutaEducativaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-18 Generar ruta recomendada y HU-19 Consultar rutas. */
@RestController
@RequestMapping("/api/beneficiario/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaEducativaService rutaService;

    @PostMapping("/generar")
    public RutaDtos.RutaResponse generar() {
        return rutaService.solicitarRuta(UsuarioActual.id());
    }

    /** 204 si aún no tiene rutas. */
    @GetMapping("/actual")
    public ResponseEntity<RutaDtos.RutaResponse> actual() {
        return rutaService.consultarActual(UsuarioActual.id())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<RutaDtos.RutaResponse> historial() {
        return rutaService.historial(UsuarioActual.id());
    }
}
