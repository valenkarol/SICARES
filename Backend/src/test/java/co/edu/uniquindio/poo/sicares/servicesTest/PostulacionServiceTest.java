package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.CambioEstadoRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de PostulacionService.
 *
 * Repositorios, almacenamiento, notificaciones y publicador de eventos se simulan (mock):
 * solo se prueba la lógica del servicio y la máquina de estados de la postulación.
 */
@ExtendWith(MockitoExtension.class)
class PostulacionServiceTest {

    private static final Long BENEFICIARIO_ID = 1L;
    private static final Long ADMIN_ID = 99L;
    private static final Long POSTULACION_ID = 7L;
    private static final Long REQUISITO_DOC_ID = 5L;

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
    private Requisito requisitoDocumento;

    @BeforeEach
    void setUp() {
        service = new PostulacionService(postulacionRepository, recomendacionRepository, beneficiarioRepository,
                usuarioRepository, requisitoRepository, documentoRepository, recordatorioRepository,
                almacenamiento, servicioNotificaciones, eventos);
        beneficiario = new Beneficiario();
        beneficiario.setId(BENEFICIARIO_ID);
        beneficiario.setCorreo("ana@correo.com");

        requisitoDocumento = new Requisito();
        requisitoDocumento.setId(REQUISITO_DOC_ID);
        requisitoDocumento.setNombre("Diploma de bachiller");
        requisitoDocumento.setTipo(TipoRequisito.DOCUMENTO);
        requisitoDocumento.setObligatorio(true);
        requisitoDocumento.setRequiereDocumento(true);
    }

    // ------------------------------------------------------------------ datos de prueba

    private OportunidadEducativa oportunidadVigente(boolean exigeDocumento) {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        inst.setTipo(TipoInstitucion.PUBLICA);

        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(3L);
        p.setNombre("Tecnólogo en Desarrollo de Software");
        p.setNivel(NivelEducativo.TECNOLOGICO);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(BigDecimal.ZERO);
        p.setInstitucion(inst);
        if (exigeDocumento) {
            p.getRequisitos().add(requisitoDocumento);
        }

        OportunidadEducativa o = new OportunidadEducativa();
        o.setId(4L);
        o.setPrograma(p);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(5));
        o.setFechaCierreInscripcion(LocalDate.now().plusDays(20));
        o.setFechaAdmision(LocalDate.now().plusDays(35));
        o.setFechaMatricula(LocalDate.now().plusDays(50));
        o.setCupos(30);
        return o;
    }

    private Recomendacion recomendacionDe(Beneficiario dueno, OportunidadEducativa o) {
        RutaEducativa ruta = new RutaEducativa();
        ruta.setBeneficiario(dueno);
        Recomendacion r = new Recomendacion();
        r.setId(10L);
        r.setOportunidad(o);
        ruta.agregarRecomendacion(r);
        return r;
    }

    private Postulacion postulacion(EstadoPostulacion estado, boolean exigeDocumento) {
        Postulacion p = new Postulacion();
        p.setId(POSTULACION_ID);
        p.setBeneficiario(beneficiario);
        p.setOportunidad(oportunidadVigente(exigeDocumento));
        p.iniciar(beneficiario);
        p.setEstado(estado);
        return p;
    }

    // ------------------------------------------------------------------ HU-20 Seleccionar oportunidad

    @Test
    @DisplayName("HU-20: seleccionar una oportunidad de la ruta crea la postulación SELECCIONADA con recordatorios")
    void seleccionarOportunidad() {
        OportunidadEducativa o = oportunidadVigente(true);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(recomendacionRepository.findById(10L)).thenReturn(Optional.of(recomendacionDe(beneficiario, o)));
        when(postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(eq(BENEFICIARIO_ID), eq(4L), anyList()))
                .thenReturn(false);
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        PostulacionResponse r = service.seleccionarOportunidad(BENEFICIARIO_ID, 10L);

        assertEquals(EstadoPostulacion.SELECCIONADA, r.estado());
        assertEquals(1, r.historial().size());
        assertEquals(1, r.documentosPendientes().size(), "El diploma queda como documento pendiente");
        verify(servicioNotificaciones).programarRecordatorios(any(Postulacion.class));
        verify(servicioNotificaciones).notificar(eq(beneficiario), any(Postulacion.class), anyString(), anyString());
    }

    @Test
    @DisplayName("HU-20: no se puede seleccionar una recomendación de la ruta de otro beneficiario")
    void seleccionarRecomendacionAjena() {
        Beneficiario otro = new Beneficiario();
        otro.setId(2L);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(recomendacionRepository.findById(10L)).thenReturn(Optional.of(recomendacionDe(otro, oportunidadVigente(false))));

        assertThrows(ReglaNegocioException.class, () -> service.seleccionarOportunidad(BENEFICIARIO_ID, 10L));
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-20: no permite dos postulaciones activas a la misma oportunidad")
    void seleccionarDuplicada() {
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(recomendacionRepository.findById(10L))
                .thenReturn(Optional.of(recomendacionDe(beneficiario, oportunidadVigente(false))));
        when(postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(eq(BENEFICIARIO_ID), eq(4L), anyList()))
                .thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.seleccionarOportunidad(BENEFICIARIO_ID, 10L));
        verify(postulacionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ HU-21 / HU-24 Consultar

    @Test
    @DisplayName("HU-24: un beneficiario no puede ver la postulación de otro")
    void consultarPostulacionAjena() {
        Postulacion p = postulacion(EstadoPostulacion.SELECCIONADA, false);
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerDelBeneficiario(2L, POSTULACION_ID));
    }

    // ------------------------------------------------------------------ HU-22 Documentos

    @Test
    @DisplayName("HU-22: carga un PDF para el requisito y deja de estar pendiente")
    void cargarDocumentoPdf() {
        Postulacion p = postulacion(EstadoPostulacion.SELECCIONADA, true);
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));
        when(requisitoRepository.findById(REQUISITO_DOC_ID)).thenReturn(Optional.of(requisitoDocumento));
        when(almacenamiento.guardar(any(), eq("postulacion-" + POSTULACION_ID))).thenReturn("postulacion-7/a.pdf");
        MockMultipartFile pdf = new MockMultipartFile("archivo", "diploma.pdf", "application/pdf", new byte[1024]);

        PostulacionResponse r = service.cargarDocumento(BENEFICIARIO_ID, POSTULACION_ID, REQUISITO_DOC_ID, pdf);

        assertEquals(1, r.documentos().size());
        assertEquals(EstadoDocumento.CARGADO, r.documentos().get(0).estado());
        assertTrue(r.documentosPendientes().isEmpty());
    }

    @Test
    @DisplayName("HU-22: rechaza archivos que no son PDF, JPG o PNG")
    void cargarDocumentoFormatoInvalido() {
        Postulacion p = postulacion(EstadoPostulacion.SELECCIONADA, true);
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));
        when(requisitoRepository.findById(REQUISITO_DOC_ID)).thenReturn(Optional.of(requisitoDocumento));
        MockMultipartFile exe = new MockMultipartFile("archivo", "virus.exe", "application/x-msdownload", new byte[10]);

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarDocumento(BENEFICIARIO_ID, POSTULACION_ID, REQUISITO_DOC_ID, exe));
        verifyNoInteractions(almacenamiento);
    }

    // ------------------------------------------------------------------ HU-23 Registrar

    @Test
    @DisplayName("HU-23: registrar con documentos faltantes deja la postulación en DOCUMENTOS_PENDIENTES")
    void registrarConPendientes() {
        Postulacion p = postulacion(EstadoPostulacion.SELECCIONADA, true);
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));

        PostulacionResponse r = service.registrar(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(EstadoPostulacion.DOCUMENTOS_PENDIENTES, r.estado());
        assertNull(r.fechaPostulacion());
        verify(eventos).publishEvent(any(PostulacionEstadoCambiadoEvent.class));
    }

    @Test
    @DisplayName("HU-23: registrar sin documentos pendientes pasa a POSTULADA con fecha")
    void registrarCompleta() {
        Postulacion p = postulacion(EstadoPostulacion.SELECCIONADA, false);
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));

        PostulacionResponse r = service.registrar(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(EstadoPostulacion.POSTULADA, r.estado());
        assertNotNull(r.fechaPostulacion());
        verify(eventos).publishEvent(any(PostulacionEstadoCambiadoEvent.class));
    }

    // ------------------------------------------------------------------ HU-25 Actualizar estado (admin)

    @Test
    @DisplayName("HU-25: el administrador pasa una postulación de POSTULADA a ADMITIDA y queda en el historial")
    void adminAdmite() {
        Administrador admin = new Administrador();
        admin.setId(ADMIN_ID);
        admin.setCorreo("admin@sicares.co");
        Postulacion p = postulacion(EstadoPostulacion.POSTULADA, false);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));

        PostulacionResponse r = service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.ADMITIDA, "Cumple requisitos"));

        assertEquals(EstadoPostulacion.ADMITIDA, r.estado());
        assertEquals("admin@sicares.co", r.historial().get(r.historial().size() - 1).responsable());
        verify(eventos).publishEvent(any(PostulacionEstadoCambiadoEvent.class));
    }

    @Test
    @DisplayName("HU-25: no se permite saltar de SELECCIONADA a MATRICULADA")
    void adminSaltoInvalido() {
        Administrador admin = new Administrador();
        admin.setId(ADMIN_ID);
        Postulacion p = postulacion(EstadoPostulacion.SELECCIONADA, false);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(postulacionRepository.findById(POSTULACION_ID)).thenReturn(Optional.of(p));

        assertThrows(ReglaNegocioException.class, () -> service.actualizarEstado(ADMIN_ID, POSTULACION_ID,
                new CambioEstadoRequest(EstadoPostulacion.MATRICULADA, null)));
        verifyNoInteractions(eventos);
    }
}
