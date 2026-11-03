package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.CanalNotificacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.notifications.EnviadorCorreo;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.NotificacionRepository;
import co.edu.uniquindio.poo.sicares.repository.RecordatorioRepository;
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
 * Pruebas unitarias de ServicioNotificaciones — HU-26 Recibir recordatorios.
 * Dueño: Integrante 2.
 */
@ExtendWith(MockitoExtension.class)
class ServicioNotificacionesTest {

    private static final int DIAS_ANTICIPACION = 3;

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private EnviadorCorreo enviadorCorreo;

    private ServicioNotificaciones servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioNotificaciones(notificacionRepository, recordatorioRepository, enviadorCorreo,
                DIAS_ANTICIPACION);
    }

    private static Postulacion postulacion(LocalDate cierre, LocalDate admision, LocalDate matricula,
                                           EstadoPostulacion estado) {
        ProgramaEducativo programa = new ProgramaEducativo();
        programa.setNombre("Técnico en Sistemas");
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPrograma(programa);
        o.setFechaCierreInscripcion(cierre);
        o.setFechaAdmision(admision);
        o.setFechaMatricula(matricula);

        Beneficiario b = new Beneficiario();
        b.setId(1L);
        b.setCorreo("ana@correo.com");

        Postulacion p = new Postulacion();
        p.setBeneficiario(b);
        p.setOportunidad(o);
        p.setEstado(estado);
        return p;
    }

    private static Recordatorio recordatorio(Postulacion p, String titulo) {
        Recordatorio r = new Recordatorio();
        r.setTitulo(titulo);
        r.setMensaje("Mensaje de prueba");
        r.setFechaEvento(LocalDate.now().plusDays(2));
        r.setFechaProgramada(LocalDateTime.now().minusHours(1));
        r.setEnviado(false);
        p.agregarRecordatorio(r);
        return r;
    }

    // ------------------------------------------------------------------ programarRecordatorios

    @Test
    @DisplayName("HU-26: programa 3 recordatorios (cierre, admisión y matrícula) N días antes, sin esperar a las 7:00")
    void programaTresRecordatorios() {
        LocalDate cierre = LocalDate.now().plusDays(20);
        Postulacion p = postulacion(cierre, cierre.plusDays(15), cierre.plusDays(30), EstadoPostulacion.SELECCIONADA);

        servicio.programarRecordatorios(p);

        assertEquals(3, p.getRecordatorios().size());
        Recordatorio primero = p.getRecordatorios().get(0);
        assertEquals("Cierre de inscripciones", primero.getTitulo());
        assertEquals(cierre.minusDays(DIAS_ANTICIPACION), primero.getFechaProgramada().toLocalDate());
        assertFalse(primero.isEnviado());
    }

    @Test
    @DisplayName("HU-26: si el evento está a menos de N días, el recordatorio queda para enviarse de inmediato")
    void eventoCercanoSeProgramaYa() {
        LocalDate cierre = LocalDate.now().plusDays(1);
        Postulacion p = postulacion(cierre, null, null, EstadoPostulacion.SELECCIONADA);

        servicio.programarRecordatorios(p);

        LocalDateTime programada = p.getRecordatorios().get(0).getFechaProgramada();
        assertFalse(programada.isAfter(LocalDateTime.now()), "No debe esperar a otro día ni a las 7:00");
    }

    @Test
    @DisplayName("HU-26: no crea recordatorios para fechas que ya pasaron o que no existen")
    void ignoraFechasPasadas() {
        Postulacion p = postulacion(LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), null,
                EstadoPostulacion.SELECCIONADA);

        servicio.programarRecordatorios(p);

        assertEquals(1, p.getRecordatorios().size());
        assertEquals("Publicación de admitidos", p.getRecordatorios().get(0).getTitulo());
    }

    // ------------------------------------------------------------------ procesarRecordatoriosPendientes

    @Test
    @DisplayName("HU-26: envía los recordatorios vigentes y omite los de postulaciones finalizadas")
    void procesaPendientes() {
        LocalDate f = LocalDate.now().plusDays(20);
        Recordatorio vigente = recordatorio(postulacion(f, f, f, EstadoPostulacion.SELECCIONADA), "Cierre de inscripciones");
        Recordatorio finalizada = recordatorio(postulacion(f, f, f, EstadoPostulacion.MATRICULADA), "Fecha de matrícula");
        when(recordatorioRepository.findPendientes(any(LocalDateTime.class))).thenReturn(List.of(vigente, finalizada));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        int enviados = servicio.procesarRecordatoriosPendientes();

        assertEquals(1, enviados);
        assertTrue(vigente.isEnviado());
        assertTrue(finalizada.isEnviado(), "El de la postulación finalizada se marca para no volver a revisarlo");
        verify(notificacionRepository, times(1)).save(any(Notificacion.class));
    }

    // ------------------------------------------------------------------ enviar

    @Test
    @DisplayName("HU-26: cada notificación se guarda en la app y se entrega al enviador de correo")
    void notificarGuardaYEnviaCorreo() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        Beneficiario b = new Beneficiario();
        b.setCorreo("ana@correo.com");

        Notificacion n = servicio.notificar(b, null, "Asunto", "Mensaje");

        assertNotNull(n.getFechaEnvio());
        assertEquals(CanalNotificacion.CORREO, n.getCanal());
        verify(notificacionRepository).save(n);
        verify(enviadorCorreo).enviar("ana@correo.com", "Asunto", "Mensaje");
    }

}
