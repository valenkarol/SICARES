package co.edu.uniquindio.poo.sicares.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HU-13. Lectura para cualquier usuario autenticado; escritura solo ADMINISTRADOR (/api/admin/**). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InstitucionController {

    private final InstitucionService institucionService;

    @GetMapping("/instituciones")
    public List<InstitucionDto> listar() {
        return institucionService.listar();
    }

    @GetMapping("/instituciones/{id}")
    public InstitucionDto obtener(@PathVariable Long id) {
        return institucionService.obtener(id);
    }

    @PostMapping("/admin/instituciones")
    @ResponseStatus(HttpStatus.CREATED)
    public InstitucionDto crear(@Valid @RequestBody InstitucionDto dto) {
        return institucionService.crear(dto);
    }

    @PutMapping("/admin/instituciones/{id}")
    public InstitucionDto actualizar(@PathVariable Long id, @Valid @RequestBody InstitucionDto dto) {
        return institucionService.actualizar(id, dto);
    }

    @DeleteMapping("/admin/instituciones/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        institucionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
