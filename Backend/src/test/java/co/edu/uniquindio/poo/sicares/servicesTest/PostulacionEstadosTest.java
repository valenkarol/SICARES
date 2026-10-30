package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoDocumento;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la máquina de estados de la postulación (HU-23 a HU-25). */
class PostulacionEstadosTest {

    private Postulacion postulacion;
    private Requisito docIdentidad;
    private final Usuario admin = new Administrador();

    @BeforeEach
    void setUp() {
        docIdentidad = new Requisito();
        docIdentidad.setId(1L);
        docIdentidad.setNombre("Documento de identidad");
        docIdentidad.setTipo(TipoRequisito.DOCUMENTO);
        docIdentidad.setObligatorio(true);
        docIdentidad.setRequiereDocumento(true);

        ProgramaEducativo programa = new ProgramaEducativo();
        programa.getRequisitos().add(docIdentidad);
        OportunidadEducativa op = new OportunidadEducativa();
        op.setPrograma(programa);

        postulacion = new Postulacion();
        postulacion.setOportunidad(op);
    }

    @Test
    void sinDocumentosQuedaEnPendientes() {
        assertFalse(postulacion.registrar(admin));
        assertEquals(EstadoPostulacion.DOCUMENTOS_PENDIENTES, postulacion.getEstado());
        assertEquals(1, postulacion.documentosPendientes().size());
    }

    @Test
    void conDocumentosQuedaPostulada() {
        DocumentoPostulacion d = new DocumentoPostulacion();
        d.setRequisito(docIdentidad);
        d.setEstado(EstadoDocumento.CARGADO);
        postulacion.agregarDocumento(d);

        assertTrue(postulacion.registrar(admin));
        assertEquals(EstadoPostulacion.POSTULADA, postulacion.getEstado());
        assertNotNull(postulacion.getFechaPostulacion());
        assertEquals(1, postulacion.getHistorial().size());
    }

    @Test
    void transicionInvalidaLanzaExcepcion() {
        assertThrows(ReglaNegocioException.class,
                () -> postulacion.cambiarEstado(EstadoPostulacion.MATRICULADA, admin, "salto inválido"));
    }

    @Test
    void matriculadaEsFinal() {
        postulacion.cambiarEstado(EstadoPostulacion.POSTULADA, admin, null);
        postulacion.cambiarEstado(EstadoPostulacion.ADMITIDA, admin, null);
        postulacion.cambiarEstado(EstadoPostulacion.MATRICULADA, admin, null);
        assertTrue(postulacion.estaFinalizada());
    }

    @Test
    void validaFormatoDeDocumento() {
        DocumentoPostulacion pdf = new DocumentoPostulacion();
        pdf.setTipoArchivo("application/pdf");
        pdf.setTamanoBytes(1024);
        assertTrue(pdf.validarFormato());

        DocumentoPostulacion exe = new DocumentoPostulacion();
        exe.setTipoArchivo("application/x-msdownload");
        exe.setTamanoBytes(1024);
        assertFalse(exe.validarFormato());
    }
}
