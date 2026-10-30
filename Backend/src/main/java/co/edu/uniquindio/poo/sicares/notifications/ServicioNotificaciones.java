package co.edu.uniquindio.poo.sicares.notifications;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.CanalNotificacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.repository.NotificacionRepository;
import co.edu.uniquindio.poo.sicares.repository.RecordatorioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <<service>> ServicioNotificaciones (HU-26, ADR-06: mecanismo centralizado de notificaciones).
 * Guarda cada notificación en BD (se ve en la app) y, si está habilitado, la envía por correo.
 */
@Slf4j
@Service
public class ServicioNotificaciones {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacionRepository notificacionRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean correoHabilitado;
    private final String remitente;
    private final int diasAnticipacion;

    public ServicioNotificaciones(NotificacionRepository notificacionRepository,
                                  RecordatorioRepository recordatorioRepository,
                                  ObjectProvider<JavaMailSender> mailSender,
                                  @Value("${sicares.notificaciones.correo-habilitado}") boolean correoHabilitado,
                                  @Value("${sicares.notificaciones.remitente}") String remitente,
                                  @Value("${sicares.notificaciones.dias-anticipacion-recordatorio}") int diasAnticipacion) {
        this.notificacionRepository = notificacionRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.mailSender = mailSender;
        this.correoHabilitado = correoHabilitado;
        this.remitente = remitente;
        this.diasAnticipacion = diasAnticipacion;
    }

    /** enviar(n): persiste la notificación y la envía por correo si aplica. */
    @Transactional
    public Notificacion enviar(Notificacion n) {
        n.setFechaEnvio(LocalDateTime.now());
        Notificacion guardada = notificacionRepository.save(n);
        if (n.getCanal() == CanalNotificacion.CORREO && correoHabilitado) {
            enviarCorreo(n.getUsuario().getCorreo(), n.getAsunto(), n.getMensaje());
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
     * de la oportunidad (cierre de inscripción, admisión y matrícula).
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
        LocalDateTime programada = fechaEvento.minusDays(diasAnticipacion).atTime(LocalTime.of(7, 0));
        if (programada.isBefore(LocalDateTime.now())) {
            programada = LocalDateTime.now();
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
     * procesarRecordatoriosPendientes(): se ejecuta automáticamente según el cron configurado
     * (por defecto todos los días a las 7:00 a.m.). El admin también puede dispararlo manualmente.
     */
    @Scheduled(cron = "${sicares.notificaciones.cron-recordatorios}")
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

    private void enviarCorreo(String para, String asunto, String cuerpo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Correo habilitado pero no hay JavaMailSender configurado");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remitente);
            msg.setTo(para);
            msg.setSubject("[SICARES] " + asunto);
            msg.setText(cuerpo + "\n\n— SICARES, Sistema de Caracterización y Seguimiento Educativo");
            sender.send(msg);
        } catch (Exception e) {
            // ADR-06: un fallo de correo no debe romper el proceso; la notificación queda en la app
            log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }
}
