package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import co.edu.uniquindio.poo.sicares.notifications.EnviadorCorreo;
import co.edu.uniquindio.poo.sicares.notifications.PostulacionEstadoCambiadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.NotificacionRepository;
import co.edu.uniquindio.poo.sicares.repository.RecordatorioRepository;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.ActualizadorTrayectoria;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaNivelEducativo;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ResultadoEvaluacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del seguimiento: recordatorios (HU-26) y retroalimentación de la trayectoria (HU-27).
 * Casos de TestRail: CP-23 (recordatorio de una fecha próxima) y CP-24 (la nueva ruta refleja la trayectoria).
 * Complementa ServicioNotificacionesTest y ActualizadorTrayectoriaTest sin modificarlos.
 * Dueño: Integrante 2 (Karol), dueña de ServicioNotificaciones y ActualizadorTrayectoria.
 */
@ExtendWith(MockitoExtension.class)
class SeguimientoTrayectoriaTest {

    @Mock private ServicioNotificaciones servicioNotificacionesMock;
    @Mock private RutaEducativaRepository rutaRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private EnviadorCorreo enviadorCorreo;

    private ActualizadorTrayectoria actualizador;
    private ServicioNotificaciones servicioNotificaciones;
    private Beneficiario beneficiario;

    @BeforeEach
    void setUp() {
        actualizador = new ActualizadorTrayectoria(servicioNotificacionesMock, rutaRepository);
        servicioNotificaciones = new ServicioNotificaciones(notificacionRepository, recordatorioRepository,
                enviadorCorreo, 3);

        TrayectoriaEducativa trayectoria = new TrayectoriaEducativa();
        trayectoria.setNivelActual(NivelEducativo.MEDIA);
        PerfilEducativo perfil = new PerfilEducativo();
        perfil.setTrayectoriaEducativa(trayectoria);

        beneficiario = new Beneficiario();
        beneficiario.setId(1L);
        beneficiario.setCorreo("ana@correo.com");
        beneficiario.setPerfil(perfil);
    }

    // ------------------------------------------------------------------ datos de prueba

    private static OportunidadEducativa oportunidad(String nombre, NivelEducativo nivel) {
        Institucion inst = new Institucion();
        inst.setNombre("SENA Regional Quindío");
        ProgramaEducativo programa = new ProgramaEducativo();
        programa.setNombre(nombre);
        programa.setNivel(nivel);
        programa.setModalidad(Modalidad.PRESENCIAL);
        programa.setInstitucion(inst);
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPrograma(programa);
        return o;
    }

    private Postulacion postulacion(OportunidadEducativa o, EstadoPostulacion estado) {
        Postulacion p = new Postulacion();
        p.setBeneficiario(beneficiario);
        p.setOportunidad(o);
        p.setEstado(estado);
        return p;
    }

    private static Recordatorio recordatorio(Postulacion p, String titulo, LocalDate evento, LocalDateTime programada) {
        Recordatorio r = new Recordatorio();
        r.setTitulo(titulo);
        r.setMensaje("Mensaje de prueba");
        r.setFechaEvento(evento);
        r.setFechaProgramada(programada);
        r.setEnviado(false);
        p.agregarRecordatorio(r);
        return r;
    }

    // ================================================================== CP-24 Trayectoria (HU-27)

    @Test
    @DisplayName("CP-24: tras matricularse en un técnico, la regla de nivel ya no recomienda otro técnico")
    void nuevaRutaNoRecomiendaLoSuperado() {
        OportunidadEducativa matriculada = oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO);
        actualizador.onEstadoCambiado(new PostulacionEstadoCambiadoEvent(
                postulacion(matriculada, EstadoPostulacion.MATRICULADA),
                EstadoPostulacion.ADMITIDA, EstadoPostulacion.MATRICULADA, null));

        ReglaNivelEducativo regla = new ReglaNivelEducativo();
        ResultadoEvaluacion otroTecnico = regla.evaluar(beneficiario.getPerfil(),
                oportunidad("Técnico en Producción Agropecuaria", NivelEducativo.TECNICO));
        ResultadoEvaluacion tecnologo = regla.evaluar(beneficiario.getPerfil(),
                oportunidad("Tecnólogo en Análisis y Desarrollo de Software", NivelEducativo.TECNOLOGICO));

        assertFalse(otroTecnico.isCumple(), "Ya está cursando un programa de nivel TECNICO");
        assertTrue(otroTecnico.getObservaciones().get(0).contains("Ya estás cursando"));
        assertTrue(tecnologo.isCumple(), "Los niveles siguientes se siguen recomendando");
    }

    @Test
    @DisplayName("CP-24: la trayectoria queda actualizada con el estudio EN_CURSO y el año actual")
    void trayectoriaActualizada() {
        actualizador.actualizarTrayectoria(beneficiario,
                oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO).getPrograma());

        TrayectoriaEducativa t = beneficiario.getPerfil().getTrayectoriaEducativa();
        EstudioRealizado estudio = t.getEstudios().get(0);
        assertEquals(NivelEducativo.TECNICO, estudio.getNivel());
        assertEquals("SENA Regional Quindío", estudio.getInstitucion());
        assertEquals(EstadoEstudio.EN_CURSO, estudio.getEstado());
        assertEquals(LocalDate.now().getYear(), estudio.getAnioInicio());
        assertEquals(LocalDate.now().getYear(), t.getAnioUltimoEstudio());
    }

    @Test
    @DisplayName("CP-24: si el beneficiario no tiene trayectoria registrada, no se modifica nada")
    void sinTrayectoriaNoHaceNada() {
        beneficiario.setPerfil(null);

        actualizador.actualizarTrayectoria(beneficiario,
                oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO).getPrograma());

        verifyNoInteractions(rutaRepository, servicioNotificacionesMock);
    }

    // ================================================================== CP-23 Recordatorios (HU-26)

    @Test
    @DisplayName("CP-23: un recordatorio programado para hoy con el evento próximo debe enviarse")
    void recordatorioDeFechaProxima() {
        Postulacion p = postulacion(oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO), EstadoPostulacion.SELECCIONADA);
        Recordatorio r = recordatorio(p, "Cierre de inscripciones", LocalDate.now().plusDays(2), LocalDateTime.now());

        assertTrue(r.debeEnviarse(LocalDate.now()));
    }

    @Test
    @DisplayName("CP-23: una fecha lejana todavía no genera recordatorio")
    void fechaLejanaNoSeEnvia() {
        Postulacion p = postulacion(oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO), EstadoPostulacion.SELECCIONADA);
        Recordatorio r = recordatorio(p, "Fecha de matrícula", LocalDate.now().plusDays(60),
                LocalDateTime.now().plusDays(57));

        assertFalse(r.debeEnviarse(LocalDate.now()));
    }

    @Test
    @DisplayName("CP-23: un recordatorio ya enviado o de un evento pasado no se vuelve a enviar")
    void enviadoOPasadoNoSeEnvia() {
        Postulacion p = postulacion(oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO), EstadoPostulacion.SELECCIONADA);
        Recordatorio enviado = recordatorio(p, "Cierre de inscripciones", LocalDate.now().plusDays(1),
                LocalDateTime.now().minusHours(2));
        enviado.setEnviado(true);
        Recordatorio pasado = recordatorio(p, "Publicación de admitidos", LocalDate.now().minusDays(1),
                LocalDateTime.now().minusDays(4));

        assertFalse(enviado.debeEnviarse(LocalDate.now()));
        assertFalse(pasado.debeEnviarse(LocalDate.now()));
    }

    @Test
    @DisplayName("CP-23: el recordatorio llega con el título y la oportunidad correctos")
    void recordatorioConFechaYOportunidadCorrectas() {
        Postulacion p = postulacion(oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO), EstadoPostulacion.SELECCIONADA);
        Recordatorio r = recordatorio(p, "Cierre de inscripciones", LocalDate.now().plusDays(2), LocalDateTime.now().minusMinutes(5));
        when(recordatorioRepository.findPendientes(any())).thenReturn(List.of(r));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        int enviados = servicioNotificaciones.procesarRecordatoriosPendientes();

        assertEquals(1, enviados);
        assertTrue(r.isEnviado());
        verify(notificacionRepository).save(argThatNotificacion("Recordatorio: Cierre de inscripciones", p));
    }

    @Test
    @DisplayName("CP-23: si ya se postuló, el recordatorio de cierre de inscripciones no se envía")
    void cierreNoSeEnviaSiYaSePostulo() {
        Postulacion p = postulacion(oportunidad("Técnico en Sistemas", NivelEducativo.TECNICO), EstadoPostulacion.POSTULADA);
        Recordatorio r = recordatorio(p, "Cierre de inscripciones", LocalDate.now().plusDays(2), LocalDateTime.now().minusMinutes(5));
        when(recordatorioRepository.findPendientes(any())).thenReturn(List.of(r));

        int enviados = servicioNotificaciones.procesarRecordatoriosPendientes();

        assertEquals(0, enviados);
        assertTrue(r.isEnviado(), "Se marca como procesado para no evaluarlo de nuevo");
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-23: sin recordatorios pendientes no se genera ninguna notificación")
    void sinPendientesNoNotifica() {
        when(recordatorioRepository.findPendientes(any())).thenReturn(List.of());

        assertEquals(0, servicioNotificaciones.procesarRecordatoriosPendientes());
        verifyNoInteractions(notificacionRepository, enviadorCorreo);
    }

    /** La notificación guardada es la del recordatorio, para el beneficiario y la postulación correctos. */
    private Notificacion argThatNotificacion(String asunto, Postulacion p) {
        return org.mockito.ArgumentMatchers.argThat(n -> asunto.equals(n.getAsunto())
                && n.getPostulacion() == p
                && n.getUsuario() == beneficiario);
    }
}