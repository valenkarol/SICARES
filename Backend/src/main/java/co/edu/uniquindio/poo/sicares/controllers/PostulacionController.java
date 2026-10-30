package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.*;
import co.edu.uniquindio.poo.sicares.security.UsuarioActual;
import co.edu.uniquindio.poo.sicares.services.PostulacionService;
import co.edu.uniquindio.poo.sicares.services.PostulacionService.ArchivoDescarga;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Postulación y seguimiento del lado del beneficiario . */
@RestController
@RequestMapping("/api/beneficiario")
@RequiredArgsConstructor
public class PostulacionController {

    private final PostulacionService postulacionService;

    /** HU-20 Seleccionar oportunidad de la ruta. */
    @PostMapping("/postulaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PostulacionResponse seleccionar(@Valid @RequestBody SeleccionarOportunidadRequest req) {
        return postulacionService.seleccionarOportunidad(UsuarioActual.id(), req.recomendacionId());
    }

    @GetMapping("/postulaciones")
    public List<PostulacionResponse> listar() {
        return postulacionService.consultarPostulaciones(UsuarioActual.id());
    }

    /** HU-21 (requisitos y fechas) y HU-24 (estado e historial). */
    @GetMapping("/postulaciones/{id}")
    public PostulacionResponse obtener(@PathVariable Long id) {
        return postulacionService.obtenerDelBeneficiario(UsuarioActual.id(), id);
    }

    /** HU-22 Cargar documento (multipart/form-data: archivo + requisitoId). */
    @PostMapping(value = "/postulaciones/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostulacionResponse cargarDocumento(@PathVariable Long id,
                                               @RequestParam Long requisitoId,
                                               @RequestParam("archivo") MultipartFile archivo) {
        return postulacionService.cargarDocumento(UsuarioActual.id(), id, requisitoId, archivo);
    }

    @GetMapping("/postulaciones/{id}/documentos/{docId}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, @PathVariable Long docId) {
        return respuestaArchivo(postulacionService.descargarDocumentoDelBeneficiario(UsuarioActual.id(), id, docId));
    }

    /** HU-23 Registrar postulación. */
    @PostMapping("/postulaciones/{id}/registrar")
    public PostulacionResponse registrar(@PathVariable Long id) {
        return postulacionService.registrar(UsuarioActual.id(), id);
    }

    @PostMapping("/postulaciones/{id}/cancelar")
    public PostulacionResponse cancelar(@PathVariable Long id) {
        return postulacionService.cancelar(UsuarioActual.id(), id);
    }

    /** HU-26 Recordatorios. */
    @GetMapping("/recordatorios")
    public List<RecordatorioResponse> recordatorios() {
        return postulacionService.recordatorios(UsuarioActual.id());
    }

    /** Arma la respuesta HTTP con el archivo (la usa también AdminPostulacionController). */
    static ResponseEntity<Resource> respuestaArchivo(ArchivoDescarga a) {
        MediaType tipo = a.tipo() != null ? MediaType.parseMediaType(a.tipo()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(a.nombre(), StandardCharsets.UTF_8).build().toString())
                .body(a.recurso());
    }
}
