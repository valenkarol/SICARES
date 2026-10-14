package co.edu.uniquindio.poo.sicares.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HU-16 Gestionar beneficios. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BeneficioController {

    private final BeneficioService beneficioService;

    @GetMapping("/beneficios")
    public List<BeneficioResponse> listar() {
        return beneficioService.listar();
    }

    @PostMapping("/admin/beneficios")
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficioResponse crear(@Valid @RequestBody BeneficioRequest req) {
        return beneficioService.crear(req);
    }

    @PutMapping("/admin/beneficios/{id}")
    public BeneficioResponse actualizar(@PathVariable Long id, @Valid @RequestBody BeneficioRequest req) {
        return beneficioService.actualizar(id, req);
    }

    @DeleteMapping("/admin/beneficios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        beneficioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
