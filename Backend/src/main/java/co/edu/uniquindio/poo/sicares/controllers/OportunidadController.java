package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.*;
import co.edu.uniquindio.poo.sicares.services.OportunidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Convocatorias de los programas fechas de inscripción, admisión, matrícula y cupos). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OportunidadController {

    private final OportunidadService oportunidadService;

    @GetMapping("/oportunidades")
    public List<OportunidadResponse> listar(@RequestParam(defaultValue = "true") boolean vigentes,
                                            @RequestParam(required = false) Long programaId) {
        return oportunidadService.listar(vigentes, programaId);
    }

    @PostMapping("/admin/oportunidades")
    @ResponseStatus(HttpStatus.CREATED)
    public OportunidadResponse crear(@Valid @RequestBody OportunidadRequest req) {
        return oportunidadService.crear(req);
    }

    @PutMapping("/admin/oportunidades/{id}")
    public OportunidadResponse actualizar(@PathVariable Long id, @Valid @RequestBody OportunidadRequest req) {
        return oportunidadService.actualizar(id, req);
    }

    @DeleteMapping("/admin/oportunidades/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        oportunidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
