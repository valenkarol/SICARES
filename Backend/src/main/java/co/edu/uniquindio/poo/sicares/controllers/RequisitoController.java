package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.RequisitoDto;
import co.edu.uniquindio.poo.sicares.services.RequisitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-15 Gestionar requisitos. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequisitoController {

    private final RequisitoService requisitoService;

    @GetMapping("/requisitos")
    public List<RequisitoDto> listar() {
        return requisitoService.listar();
    }

    @PostMapping("/admin/requisitos")
    @ResponseStatus(HttpStatus.CREATED)
    public RequisitoDto crear(@Valid @RequestBody RequisitoDto dto) {
        return requisitoService.crear(dto);
    }

    @PutMapping("/admin/requisitos/{id}")
    public RequisitoDto actualizar(@PathVariable Long id, @Valid @RequestBody RequisitoDto dto) {
        return requisitoService.actualizar(id, dto);
    }

    @DeleteMapping("/admin/requisitos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        requisitoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
