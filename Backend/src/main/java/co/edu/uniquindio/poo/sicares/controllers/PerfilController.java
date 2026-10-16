package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.dto.CaracterizacionDtos;
import co.edu.uniquindio.poo.sicares.security.UsuarioActual;
import co.edu.uniquindio.poo.sicares.services.CaracterizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API de caracterización (HU-04 a HU-11). Solo rol BENEFICIARIO (ver SecurityConfig).
 * Cada sección se guarda por separado para que el usuario pueda avanzar por pasos.
 */
@RestController
@RequestMapping("/api/beneficiario/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final CaracterizacionService caracterizacionService;

    /** HU-10 */
    @GetMapping
    public CaracterizacionDtos.PerfilResponse consultar() {
        return caracterizacionService.consultarPerfil(UsuarioActual.id());
    }

    @PostMapping("/autorizacion")
    public CaracterizacionDtos.PerfilResponse aceptarAutorizacion() {
        return caracterizacionService.aceptarAutorizacion(UsuarioActual.id());
    }

    @DeleteMapping("/autorizacion")
    public CaracterizacionDtos.PerfilResponse revocarAutorizacion() {
        return caracterizacionService.revocarAutorizacion(UsuarioActual.id());
    }

    /** HU-04 / HU-11 */
    @PutMapping("/informacion-personal")
    public CaracterizacionDtos.PerfilResponse informacionPersonal(@Valid @RequestBody CaracterizacionDtos.InformacionPersonalDto dto) {
        return caracterizacionService.guardarInformacionPersonal(UsuarioActual.id(), dto);
    }

    /** HU-05 */
    @PutMapping("/situacion-victima")
    public CaracterizacionDtos.PerfilResponse situacionVictima(@Valid @RequestBody CaracterizacionDtos.SituacionVictimaDto dto) {
        return caracterizacionService.guardarSituacionVictima(UsuarioActual.id(), dto);
    }

    /** HU-06 */
    @PutMapping("/situacion-socioeconomica")
    public CaracterizacionDtos.PerfilResponse situacionSocioeconomica(@Valid @RequestBody CaracterizacionDtos.SituacionSocioeconomicaDto dto) {
        return caracterizacionService.guardarSituacionSocioeconomica(UsuarioActual.id(), dto);
    }

    /** HU-07 */
    @PutMapping("/trayectoria")
    public CaracterizacionDtos.PerfilResponse trayectoria(@Valid @RequestBody CaracterizacionDtos.TrayectoriaEducativaDto dto) {
        return caracterizacionService.guardarTrayectoria(UsuarioActual.id(), dto);
    }

    /** HU-08 */
    @PutMapping("/intereses")
    public CaracterizacionDtos.PerfilResponse intereses(@Valid @RequestBody CaracterizacionDtos.InteresesRequest req) {
        return caracterizacionService.guardarIntereses(UsuarioActual.id(), req);
    }

    /** HU-09 */
    @PutMapping("/accesibilidad")
    public CaracterizacionDtos.PerfilResponse accesibilidad(@Valid @RequestBody CaracterizacionDtos.CondicionAccesibilidadDto dto) {
        return caracterizacionService.guardarCondicionAccesibilidad(UsuarioActual.id(), dto);
    }
}
