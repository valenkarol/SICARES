package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.CambioEstadoRequest;
import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.HistorialResponse;
import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.PostulacionResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import co.edu.uniquindio.poo.sicares.notifications.PostulacionEstadoCambiadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.*;
import co.edu.uniquindio.poo.sicares.services.AlmacenamientoService;
import co.edu.uniquindio.poo.sicares.services.PostulacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la gestión de postulaciones por el administrador — HU-25 Actualizar estado.
 * Caso de TestRail: CP-22 (el administrador actualiza el estado y queda historial).
 * Prueba la lógica que usa AdminPostulacionController (PostulacionService).
 */
@ExtendWith(MockitoExtension.class)
class PostulacionAdminTest {

    private static final Long ADMIN_ID = 99L;
    private static final Long POSTULACION_ID = 7L;

    @Mock private PostulacionRepository postulacionRepository;
    @Mock private RecomendacionRepository recomendacionRepository;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RequisitoRepository requisitoRepository;
    @Mock private DocumentoPostulacionRepository documentoRepository;
    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private AlmacenamientoService almacenamiento;
    @Mock private ServicioNotificaciones servicioNotificaciones;
    @Mock private ApplicationEventPublisher eventos;

    private PostulacionService service;
    private Beneficiario beneficiario;
    private Administrador admin;
    private Requisito identidad;

    @BeforeEach
    void setUp() {
        service = new PostulacionService(postulacionRepository, recomendacionRepository, beneficiarioRepository,
                usuarioRepository, requisitoRepository, documentoRepository, recordatorioRepository,
                almacenamiento, servicioNotificaciones, eventos);

        beneficiario = new Beneficiario();
        beneficiario.setId(1L);
        beneficiario.setCorreo("ana@correo.com");

        admin = new Administrador();
        admin.setId(ADMIN_ID);
        admin.setCorreo("admin@sicares.co");

        identidad = new Requisito();
        identidad.setId(5L);
        identidad.setNombre("Documento de identidad");
        identidad.setTipo(TipoRequisito.DOCUMENTO);
        identidad.setObligatorio(true);
        identidad.setRequiereDocumento(true);
    }

    // ------------------------------------------------------------------ datos de prueba

    private Postulacion postulacion(EstadoPostulacion estado, EstadoDocumento estadoDocumento) {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        ProgramaEducativo programa = new ProgramaEducativo();
        programa.setId(3L);
        programa.setNombre("Técnico en Sistemas");
        programa.setNivel(NivelEducativo.TECNICO);
        programa.setModalidad(Modalidad.PRESENCIAL);
        programa.setMunicipio("Armenia");
        programa.setCostoPeriodo(BigDecimal.ZERO);
        programa.setInstitucion(inst);
        programa.getRequisitos().add(identidad);

        OportunidadEducativa o = new OportunidadEducativa();
        o.setId(4L);
        o.setPrograma(programa);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(10));
        o.setFechaCierreInscripcion(LocalDate.now().plusDays(20));
        o.setCupos(30);

        Postulacion p = new Postulacion();
        p.setId(POSTULACION_ID);
        p.setBeneficiario(beneficiario);
        p.setOportunidad(o);
        p.iniciar(beneficiario);
        p.setEstado(estado);

        if (estadoDocumento != null) {
            DocumentoPostulacion d = new DocumentoPostulacion();
            d.setId(20L);
            d.setNombreArchivo("cedula.pdf");
            d.setTipoArchivo("application/pdf");
            d.setTamanoBytes(100);
            d.setRutaAlmacenamiento("postulacion-7/cedula.pdf");
            d.setRequisito(identidad);
            d.setEstado(estadoDocumento);
            p.agregarDocumento(d);
        }
        return p;
    }

    private void existenAdminYPostulacion(Postulacion p) {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));
    }

    private static HistorialResponse ultimo(PostulacionResponse r) {
        return r.historial().get(r.historial().size() - 1);
    }

    // ------------------------------------------------------------------ CP-22

    @Test
    @DisplayName("CP-22: el historial registra el estado anterior, el nuevo, la fecha, la observación y quién lo cambió")
    void historialCompleto() {
        Postulacion p = postulacion(EstadoPostulacion.POSTULADA, EstadoDocumento.VALIDADO);
        existenAdminYPostulacion(p);

        PostulacionResponse r = service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.ADMITIDA, "Cumple todos los requisitos"));

        HistorialResponse h = ultimo(r);
        assertEquals(EstadoPostulacion.ADMITIDA, r.estado());
        assertEquals(EstadoPostulacion.POSTULADA, h.estadoAnterior());
        assertEquals(EstadoPostulacion.ADMITIDA, h.estadoNuevo());
        assertNotNull(h.fechaCambio());
        assertEquals("Cumple todos los requisitos", h.observacion());
        assertEquals("admin@sicares.co", h.responsable());
    }

    @Test
    @DisplayName("CP-22: el cambio se publica para que el beneficiario lo vea y reciba la notificación")
    void publicaElCambioParaElBeneficiario() {
        Postulacion p = postulacion(EstadoPostulacion.POSTULADA, EstadoDocumento.VALIDADO);
        existenAdminYPostulacion(p);

        service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.ADMITIDA, "Cumple"));

        ArgumentCaptor<PostulacionEstadoCambiadoEvent> evento = ArgumentCaptor.forClass(PostulacionEstadoCambiadoEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertEquals(EstadoPostulacion.POSTULADA, evento.getValue().estadoAnterior());
        assertEquals(EstadoPostulacion.ADMITIDA, evento.getValue().estadoNuevo());
        assertSame(beneficiario, evento.getValue().postulacion().getBeneficiario());
    }

    @Test
    @DisplayName("CP-22: NO_ADMITIDA es un estado final: la postulación queda finalizada y sin estados siguientes")
    void noAdmitidaEsFinal() {
        Postulacion p = postulacion(EstadoPostulacion.POSTULADA, EstadoDocumento.CARGADO);
        existenAdminYPostulacion(p);

        PostulacionResponse r = service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.NO_ADMITIDA, "No cumple el puntaje"));

        assertTrue(r.finalizada());
        assertTrue(r.siguientesEstados().isEmpty());
    }

    @Test
    @DisplayName("CP-22: una postulación finalizada no cambia de estado y el historial no se altera")
    void finalizadaNoCambia() {
        Postulacion p = postulacion(EstadoPostulacion.MATRICULADA, EstadoDocumento.VALIDADO);
        existenAdminYPostulacion(p);
        int cambiosAntes = p.getHistorial().size();

        assertThrows(ReglaNegocioException.class, () -> service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.CERRADA, null)));

        assertEquals(EstadoPostulacion.MATRICULADA, p.getEstado());
        assertEquals(cambiosAntes, p.getHistorial().size());
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("CP-22: no se marca POSTULADA si aún faltan documentos")
    void noPostulaConDocumentosPendientes() {
        Postulacion p = postulacion(EstadoPostulacion.DOCUMENTOS_PENDIENTES, null);
        existenAdminYPostulacion(p);

        assertThrows(ReglaNegocioException.class, () -> service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.POSTULADA, null)));
    }

    @Test
    @DisplayName("CP-22: si el administrador no existe, no se cambia nada")
    void adminInexistente() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.ADMITIDA, null)));
        verify(postulacionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-22: el listado del administrador filtra por estado o muestra todas")
    void listadoFiltraPorEstado() {
        Postulacion p = postulacion(EstadoPostulacion.POSTULADA, null);
        when(postulacionRepository.findByEstadoOrderByFechaSeleccionDesc(EstadoPostulacion.POSTULADA))
                .thenReturn(List.of(p));
        when(postulacionRepository.findAllByOrderByFechaSeleccionDesc()).thenReturn(List.of(p));

        assertEquals(1, service.listarTodas(EstadoPostulacion.POSTULADA).size());
        assertEquals(1, service.listarTodas(null).size());
        verify(postulacionRepository).findByEstadoOrderByFechaSeleccionDesc(EstadoPostulacion.POSTULADA);
        verify(postulacionRepository).findAllByOrderByFechaSeleccionDesc();
    }

    @Test
    @DisplayName("CP-22: al rechazar un documento se notifica al beneficiario para que lo cargue de nuevo")
    void rechazarDocumentoNotifica() {
        Postulacion p = postulacion(EstadoPostulacion.POSTULADA, EstadoDocumento.CARGADO);
        DocumentoPostulacion d = p.getDocumentos().get(0);
        when(documentoRepository.findById(20L)).thenReturn(Optional.of(d));

        PostulacionResponse r = service.cambiarEstadoDocumento(20L, EstadoDocumento.RECHAZADO);

        assertEquals(EstadoDocumento.RECHAZADO, r.documentos().get(0).estado());
        verify(servicioNotificaciones).notificar(eq(beneficiario), eq(p), eq("Documento rechazado"), anyString());
    }

    @Test
    @DisplayName("CP-22: no se cambian documentos de una postulación finalizada")
    void documentoDePostulacionFinalizada() {
        Postulacion p = postulacion(EstadoPostulacion.MATRICULADA, EstadoDocumento.VALIDADO);
        DocumentoPostulacion d = p.getDocumentos().get(0);
        when(documentoRepository.findById(20L)).thenReturn(Optional.of(d));

        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstadoDocumento(20L, EstadoDocumento.RECHAZADO));
        assertEquals(EstadoDocumento.VALIDADO, d.getEstado());
    }
}
