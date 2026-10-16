package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.*;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.Rol;
import co.edu.uniquindio.poo.sicares.security.UsuarioActual;
import co.edu.uniquindio.poo.sicares.services.ProgramaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consultar programas (con filtros) Gestionar programas. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProgramaController {

    private final ProgramaService programaService;

    /** Ej.: GET /api/programas?nivel=TECNICO&modalidad=VIRTUAL&municipio=Armenia&area=salud&texto=enfer */
    @GetMapping("/programas")
    public List<ProgramaResponse> buscar(@RequestParam(required = false) NivelEducativo nivel,
                                         @RequestParam(required = false) Modalidad modalidad,
                                         @RequestParam(required = false) String municipio,
                                         @RequestParam(required = false) String area,
                                         @RequestParam(required = false) String texto) {
        boolean esAdmin = Rol.ADMINISTRADOR.name().equals(UsuarioActual.get().rol());
        return programaService.buscar(!esAdmin, nivel, modalidad, municipio, area, texto);
    }

    @GetMapping("/programas/{id}")
    public ProgramaResponse obtener(@PathVariable Long id) {
        return programaService.obtener(id);
    }

    @PostMapping("/admin/programas")
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramaResponse crear(@Valid @RequestBody ProgramaRequest req) {
        return programaService.crear(req);
    }

    @PutMapping("/admin/programas/{id}")
    public ProgramaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProgramaRequest req) {
        return programaService.actualizar(id, req);
    }

    @DeleteMapping("/admin/programas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        programaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
