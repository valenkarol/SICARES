package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.PostulacionResponse;
import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.RecordatorioResponse;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de PostulacionService — casos del proceso 3 que no cubre PostulacionServiceTest.
 * Casos de TestRail: CP-17 (selección), CP-18 (requisitos y fechas), CP-19 (documentos),
 * CP-20 (registro) y CP-21 (consulta del estado).
 */
@ExtendWith(MockitoExtension.class)
class PostulacionSeguimientoTest {

    private static final Long BENEFICIARIO_ID = 1L;
    private static final Long POSTULACION_ID = 7L;
    private static final Long REQ_IDENTIDAD = 5L;
    private static final Long REQ_DIPLOMA = 6L;
    private static final Long REQ_NIVEL = 8L;

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
    private Requisito identidad;
    private Requisito diploma;
    private Requisito nivelMedia;

    @BeforeEach
    void setUp() {
        service = new PostulacionService(postulacionRepository, recomendacionRepository, beneficiarioRepository,
                usuarioRepository, requisitoRepository, documentoRepository, recordatorioRepository,
                almacenamiento, servicioNotificaciones, eventos);

        beneficiario = new Beneficiario();
        beneficiario.setId(BENEFICIARIO_ID);
        beneficiario.setCorreo("ana@correo.com");

        identidad = requisito(REQ_IDENTIDAD, "Documento de identidad", TipoRequisito.DOCUMENTO, null, true);
        diploma = requisito(REQ_DIPLOMA, "Diploma de bachiller", TipoRequisito.DOCUMENTO, null, true);
        nivelMedia = requisito(REQ_NIVEL, "Haber culminado la media", TipoRequisito.NIVEL_EDUCATIVO, "MEDIA", false);
    }

    // ------------------------------------------------------------------ datos de prueba

    private static Requisito requisito(Long id, String nombre, TipoRequisito tipo, String valor, boolean conDocumento) {
        Requisito r = new Requisito();
        r.setId(id);
        r.setNombre(nombre);
        r.setTipo(tipo);
        r.setValorEsperado(valor);
        r.setObligatorio(true);
        r.setRequiereDocumento(conDocumento);
        return r;
    }

    /** Convocatoria del programa con los requisitos indicados; cierra en 'diasParaCierre' días. */
    private static OportunidadEducativa oportunidad(int diasParaCierre, Requisito... requisitos) {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        inst.setTipo(TipoInstitucion.PUBLICA);

        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(3L);
        p.setNombre("Técnico en Sistemas");
        p.setNivel(NivelEducativo.TECNICO);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(BigDecimal.ZERO);
        p.setInstitucion(inst);
        for (Requisito r : requisitos) {
            p.getRequisitos().add(r);
        }

        OportunidadEducativa o = new OportunidadEducativa();
        o.setId(4L);
        o.setPrograma(p);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(10));
        o.setFechaCierreInscripcion(LocalDate.now().plusDays(diasParaCierre));
        o.setFechaAdmision(LocalDate.now().plusDays(40));
        o.setFechaMatricula(LocalDate.now().plusDays(55));
        o.setCupos(30);
        return o;
    }

    private Recomendacion recomendacion(OportunidadEducativa o) {
        RutaEducativa ruta = new RutaEducativa();
        ruta.setBeneficiario(beneficiario);
        Recomendacion r = new Recomendacion();
        r.setId(10L);
        r.setOportunidad(o);
        ruta.agregarRecomendacion(r);
        return r;
    }

    private Postulacion postulacion(Long id, EstadoPostulacion estado, OportunidadEducativa o) {
        Postulacion p = new Postulacion();
        p.setId(id);
        p.setBeneficiario(beneficiario);
        p.setOportunidad(o);
        p.iniciar(beneficiario);
        p.setEstado(estado);
        return p;
    }

    private void existePostulacion(Postulacion p) {
        when(postulacionRepository.findById(p.getId())).thenReturn(Optional.of(p));
    }

    private static MockMultipartFile pdf(String nombre) {
        return new MockMultipartFile("archivo", nombre, "application/pdf",
                "%PDF-1.4 documento de prueba".getBytes(StandardCharsets.US_ASCII));
    }

    // ================================================================== CP-17 Selección (HU-20)

    @Test
    @DisplayName("CP-17: no deja seleccionar una oportunidad cuya convocatoria ya cerró")
    void noSeleccionaConvocatoriaCerrada() {
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(recomendacionRepository.findById(10L)).thenReturn(Optional.of(recomendacion(oportunidad(-1))));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.seleccionarOportunidad(BENEFICIARIO_ID, 10L));

        assertTrue(ex.getMessage().contains("cerró"));
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-17: la oportunidad seleccionada se guarda y sigue SELECCIONADA al volver a consultarla")
    void seleccionSeConserva() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20, identidad));
        existePostulacion(p);

        PostulacionResponse r = service.obtenerDelBeneficiario(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(EstadoPostulacion.SELECCIONADA, r.estado());
        assertNotNull(r.fechaSeleccion());
        assertFalse(r.finalizada());
        assertTrue(r.siguientesEstados().contains(EstadoPostulacion.POSTULADA));
    }

    @Test
    @DisplayName("CP-17: solo bloquea la selección repetida si la postulación anterior sigue activa")
    void permiteVolverASeleccionarSiLaAnteriorTermino() {
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(recomendacionRepository.findById(10L)).thenReturn(Optional.of(recomendacion(oportunidad(20))));
        when(postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(eq(BENEFICIARIO_ID), eq(4L), anyList()))
                .thenReturn(false);
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.seleccionarOportunidad(BENEFICIARIO_ID, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EstadoPostulacion>> estados = ArgumentCaptor.forClass(List.class);
        verify(postulacionRepository).existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(
                eq(BENEFICIARIO_ID), eq(4L), estados.capture());
        assertTrue(estados.getValue().containsAll(List.of(EstadoPostulacion.CERRADA, EstadoPostulacion.NO_ADMITIDA)),
                "Una postulación CERRADA o NO_ADMITIDA no impide volver a seleccionar");
    }

    // ================================================================== CP-18 Requisitos y fechas (HU-21)

    @Test
    @DisplayName("CP-18: el detalle muestra los requisitos, los documentos solicitados y las fechas del programa")
    void detalleConRequisitosDocumentosYFechas() {
        OportunidadEducativa o = oportunidad(20, identidad, nivelMedia);
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, o);
        existePostulacion(p);

        PostulacionResponse r = service.obtenerDelBeneficiario(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(2, r.requisitos().size(), "Todos los requisitos configurados por el administrador");
        assertEquals(List.of("Documento de identidad"),
                r.documentosPendientes().stream().map(d -> d.nombre()).toList(),
                "Solo los requisitos que exigen documento se piden como archivo");
        assertEquals("Técnico en Sistemas", r.oportunidad().programaNombre());
        assertEquals(o.getFechaCierreInscripcion(), r.oportunidad().fechaCierreInscripcion());
        assertEquals(o.getFechaAdmision(), r.oportunidad().fechaAdmision());
        assertEquals(o.getFechaMatricula(), r.oportunidad().fechaMatricula());
    }

    // ================================================================== CP-19 Documentos (HU-22)

    @Test
    @DisplayName("CP-19: al seleccionar, todos los documentos requeridos aparecen como pendientes")
    void todosLosDocumentosInicianPendientes() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20, identidad, diploma));
        existePostulacion(p);

        PostulacionResponse r = service.obtenerDelBeneficiario(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(2, r.documentosPendientes().size());
        assertTrue(r.documentos().isEmpty());
    }

    @Test
    @DisplayName("CP-19: al cargar un documento, ese queda CARGADO y los demás siguen pendientes")
    void cargarUnoDejaLosDemasPendientes() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20, identidad, diploma));
        existePostulacion(p);
        when(requisitoRepository.findById(REQ_IDENTIDAD)).thenReturn(Optional.of(identidad));
        when(almacenamiento.guardar(any(), anyString())).thenReturn("postulacion-7/cedula.pdf");

        PostulacionResponse r = service.cargarDocumento(BENEFICIARIO_ID, POSTULACION_ID, REQ_IDENTIDAD, pdf("cedula.pdf"));

        assertEquals(1, r.documentos().size());
        assertEquals(EstadoDocumento.CARGADO, r.documentos().get(0).estado());
        assertEquals("Documento de identidad", r.documentos().get(0).requisitoNombre());
        assertEquals(List.of("Diploma de bachiller"),
                r.documentosPendientes().stream().map(d -> d.nombre()).toList());
    }

    @Test
    @DisplayName("CP-19: no acepta un documento para un requisito que no es del programa")
    void requisitoAjenoAlPrograma() {
        Requisito ajeno = requisito(88L, "Certificado laboral", TipoRequisito.DOCUMENTO, null, true);
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20, identidad));
        existePostulacion(p);
        when(requisitoRepository.findById(88L)).thenReturn(Optional.of(ajeno));

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarDocumento(BENEFICIARIO_ID, POSTULACION_ID, 88L, pdf("laboral.pdf")));
        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    @Test
    @DisplayName("CP-19: rechaza un archivo de más de 5 MB")
    void rechazaArchivoGrande() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20, identidad));
        existePostulacion(p);
        when(requisitoRepository.findById(REQ_IDENTIDAD)).thenReturn(Optional.of(identidad));
        byte[] grande = new byte[(int) DocumentoPostulacion.TAMANO_MAXIMO_BYTES + 1];
        grande[0] = '%'; grande[1] = 'P'; grande[2] = 'D'; grande[3] = 'F';
        MockMultipartFile archivo = new MockMultipartFile("archivo", "grande.pdf", "application/pdf", grande);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.cargarDocumento(BENEFICIARIO_ID, POSTULACION_ID, REQ_IDENTIDAD, archivo));

        assertTrue(ex.getMessage().contains("5 MB"));
        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    @Test
    @DisplayName("CP-19: un archivo vacío no se registra")
    void rechazaArchivoVacio() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20, identidad));
        existePostulacion(p);
        MockMultipartFile vacio = new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThrows(ReglaNegocioException.class,
                () -> service.cargarDocumento(BENEFICIARIO_ID, POSTULACION_ID, REQ_IDENTIDAD, vacio));
    }

    // ================================================================== CP-20 Registro (HU-23)

    @Test
    @DisplayName("CP-20: registrar guarda la fecha de hoy, queda POSTULADA y publica el cambio de estado")
    void registrarConFechaActual() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20));
        existePostulacion(p);

        PostulacionResponse r = service.registrar(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(EstadoPostulacion.POSTULADA, r.estado());
        assertEquals(LocalDate.now(), r.fechaPostulacion().toLocalDate());
        verify(eventos).publishEvent(any(PostulacionEstadoCambiadoEvent.class));
    }

    @Test
    @DisplayName("CP-20: una postulación ya registrada no se puede registrar otra vez")
    void noRegistraDosVeces() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.POSTULADA, oportunidad(20));
        existePostulacion(p);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(BENEFICIARIO_ID, POSTULACION_ID));

        assertTrue(ex.getMessage().contains("ya fue registrada"));
        verifyNoInteractions(eventos);
    }

    @Test
    @DisplayName("CP-20: no se registra si la convocatoria ya cerró")
    void noRegistraConvocatoriaCerrada() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(-1));
        existePostulacion(p);

        assertThrows(ReglaNegocioException.class, () -> service.registrar(BENEFICIARIO_ID, POSTULACION_ID));
        assertEquals(EstadoPostulacion.SELECCIONADA, p.getEstado());
    }

    // ================================================================== CP-21 Consulta del estado (HU-24)

    @Test
    @DisplayName("CP-21: el listado trae solo las postulaciones del beneficiario que consulta")
    void listadoSoloDelBeneficiario() {
        Postulacion a = postulacion(7L, EstadoPostulacion.POSTULADA, oportunidad(20));
        Postulacion b = postulacion(8L, EstadoPostulacion.SELECCIONADA, oportunidad(20));
        when(postulacionRepository.findByBeneficiarioIdOrderByFechaSeleccionDesc(BENEFICIARIO_ID))
                .thenReturn(List.of(a, b));

        List<PostulacionResponse> r = service.consultarPostulaciones(BENEFICIARIO_ID);

        assertEquals(List.of(7L, 8L), r.stream().map(PostulacionResponse::id).toList());
        assertTrue(r.stream().allMatch(x -> x.beneficiarioId().equals(BENEFICIARIO_ID)));
        verify(postulacionRepository, never()).findAllByOrderByFechaSeleccionDesc();
    }

    @Test
    @DisplayName("CP-21: el detalle muestra el estado actual y el historial con sus fechas")
    void detalleConEstadoEHistorial() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20));
        p.cambiarEstado(EstadoPostulacion.POSTULADA, beneficiario, "Postulación registrada por el beneficiario");
        existePostulacion(p);

        PostulacionResponse r = service.obtenerDelBeneficiario(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(EstadoPostulacion.POSTULADA, r.estado());
        assertEquals(2, r.historial().size());
        assertTrue(r.historial().stream().allMatch(h -> h.fechaCambio() != null));
    }

    @Test
    @DisplayName("CP-21: el beneficiario ve sus recordatorios con la fecha del evento")
    void recordatoriosDelBeneficiario() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20));
        Recordatorio rec = new Recordatorio();
        rec.setId(3L);
        rec.setTitulo("Cierre de inscripciones");
        rec.setMensaje("Las inscripciones cierran pronto");
        rec.setFechaEvento(LocalDate.now().plusDays(2));
        rec.setFechaProgramada(LocalDateTime.now());
        p.agregarRecordatorio(rec);
        when(recordatorioRepository.findByBeneficiario(BENEFICIARIO_ID)).thenReturn(List.of(rec));

        List<RecordatorioResponse> r = service.recordatorios(BENEFICIARIO_ID);

        assertEquals(1, r.size());
        assertEquals("Cierre de inscripciones", r.get(0).titulo());
        assertEquals(LocalDate.now().plusDays(2), r.get(0).fechaEvento());
        assertEquals(POSTULACION_ID, r.get(0).postulacionId());
    }

    @Test
    @DisplayName("HU-24: el beneficiario puede cancelar su postulación y queda en el historial")
    void cancelarPostulacion() {
        Postulacion p = postulacion(POSTULACION_ID, EstadoPostulacion.SELECCIONADA, oportunidad(20));
        existePostulacion(p);

        PostulacionResponse r = service.cancelar(BENEFICIARIO_ID, POSTULACION_ID);

        assertEquals(EstadoPostulacion.CERRADA, r.estado());
        assertTrue(r.finalizada());
        assertEquals(EstadoPostulacion.SELECCIONADA, p.getHistorial().get(p.getHistorial().size() - 1).getEstadoAnterior());
        verify(eventos).publishEvent(any(PostulacionEstadoCambiadoEvent.class));
    }
}
