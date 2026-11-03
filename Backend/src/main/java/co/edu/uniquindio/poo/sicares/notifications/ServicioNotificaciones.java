package co.edu.uniquindio.poo.sicares.notifications;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.CanalNotificacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.repository.NotificacionRepository;
import co.edu.uniquindio.poo.sicares.repository.RecordatorioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * <<service>> ServicioNotificaciones (HU-26, ADR-06: mecanismo centralizado de notificaciones).
 * Guarda cada notificación en BD (se ve en la app) y, si está habilitado, la envía por correo
 * (el envío real lo hace {@link EnviadorCorreo}, en segundo plano y después de confirmar la transacción).
 */
@Slf4j
@Service
public class ServicioNotificaciones {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacionRepository notificacionRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final EnviadorCorreo enviadorCorreo;
    private final int diasAnticipacion;

    public ServicioNotificaciones(NotificacionRepository notificacionRepository,
                                  RecordatorioRepository recordatorioRepository,
                                  EnviadorCorreo enviadorCorreo,
                                  @Value("${sicares.notificaciones.dias-anticipacion-recordatorio}") int diasAnticipacion) {
        this.notificacionRepository = notificacionRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.enviadorCorreo = enviadorCorreo;
        this.diasAnticipacion = diasAnticipacion;
    }

    /** enviar(n): persiste la notificación y la envía por correo si aplica. */
    @Transactional
    public Notificacion enviar(Notificacion n) {
        n.setFechaEnvio(LocalDateTime.now());
        Notificacion guardada = notificacionRepository.save(n);
        if (n.getCanal() == CanalNotificacion.CORREO) {
            enviadorCorreo.enviar(n.getUsuario().getCorreo(), n.getAsunto(), n.getMensaje());
        }
        return guardada;
    }

    /** Atajo para crear y enviar una notificación. */
    @Transactional
    public Notificacion notificar(Usuario destinatario, Postulacion postulacion, String asunto, String mensaje) {
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setPostulacion(postulacion);
        n.setAsunto(asunto);
        n.setMensaje(mensaje);
        n.setCanal(CanalNotificacion.CORREO);
        return enviar(n);
    }

    /**
     * programarRecordatorios(p): crea recordatorios N días antes de cada fecha importante
     * de la oportunidad (cierre de inscripción, admisión y matrícula), a la misma hora en que se seleccionó.
     * Si esa fecha ya pasó (el evento está a menos de N días), queda para enviarse de inmediato.
     */
    public void programarRecordatorios(Postulacion p) {
        OportunidadEducativa o = p.getOportunidad();
        String programa = o.getPrograma().getNombre();
        crearRecordatorio(p, o.getFechaCierreInscripcion(), "Cierre de inscripciones",
                "Las inscripciones para " + programa + " cierran el %s. Verifica que tu postulación y documentos estén completos.");
        crearRecordatorio(p, o.getFechaAdmision(), "Publicación de admitidos",
                "El %s se publican los resultados de admisión de " + programa + ".");
        crearRecordatorio(p, o.getFechaMatricula(), "Fecha de matrícula",
                "La matrícula para " + programa + " es el %s. Si fuiste admitido, no olvides matricularte.");
    }

    private void crearRecordatorio(Postulacion p, LocalDate fechaEvento, String titulo, String plantilla) {
        if (fechaEvento == null || fechaEvento.isBefore(LocalDate.now())) return;
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime programada = fechaEvento.minusDays(diasAnticipacion).atTime(LocalTime.from(ahora));
        if (programada.isBefore(ahora)) {
            programada = ahora;
        }
        Recordatorio r = new Recordatorio();
        r.setTitulo(titulo);
        r.setMensaje(String.format(plantilla, fechaEvento.format(FECHA)));
        r.setFechaEvento(fechaEvento);
        r.setFechaProgramada(programada);
        r.setEnviado(false);
        p.agregarRecordatorio(r);
    }

    /**
     * procesarRecordatoriosPendientes(): se ejecuta automáticamente cada minuto (configurable con
     * sicares.notificaciones.intervalo-recordatorios-ms), así cada recordatorio sale en cuanto llega su hora y no
     * solo a las 7:00 a.m. El admin también puede dispararlo manualmente.
     */
    @Scheduled(fixedDelayString = "${sicares.notificaciones.intervalo-recordatorios-ms:60000}",
            initialDelayString = "${sicares.notificaciones.intervalo-recordatorios-ms:60000}")
    @Transactional
    public void tareaProgramadaRecordatorios() {
        procesarRecordatoriosPendientes();
    }

    @Transactional
    public int procesarRecordatoriosPendientes() {
        LocalDate hoy = LocalDate.now();
        List<Recordatorio> pendientes = recordatorioRepository.findPendientes(LocalDateTime.now());
        int enviados = 0;
        for (Recordatorio r : pendientes) {
            Postulacion p = r.getPostulacion();
            boolean irrelevante = p.estaFinalizada()
                    || (r.getTitulo().startsWith("Cierre") && p.getEstado() != EstadoPostulacion.SELECCIONADA
                    && p.getEstado() != EstadoPostulacion.DOCUMENTOS_PENDIENTES);
            if (!irrelevante && r.debeEnviarse(hoy)) {
                notificar(p.getBeneficiario(), p, "Recordatorio: " + r.getTitulo(), r.getMensaje());
                enviados++;
            }
            r.setEnviado(true);
        }
        if (enviados > 0) log.info("Recordatorios enviados: {}", enviados);
        return enviados;
    }
}
