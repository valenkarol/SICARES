package co.edu.uniquindio.poo.sicares.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Envío de correos de SICARES (HU-26, ADR-06).
 * <ul>
 *   <li>El correo se envía <b>después</b> de que la transacción se confirma y en otro hilo: si Gmail tarda o falla,
 *       la petición del usuario no espera ni se cae, y la notificación igual queda en la app.</li>
 *   <li>Al arrancar escribe en el log si el correo quedó habilitado y qué falta configurar.</li>
 *   <li>{@link #probar(String)} envía un correo de prueba y devuelve el error real (lo usa el panel del admin).</li>
 * </ul>
 */
@Slf4j
@Component
public class EnviadorCorreo {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final TaskExecutor executor;
    private final boolean habilitado;
    private final String remitente;
    private final String usuarioSmtp;
    private final String claveSmtp;

    public EnviadorCorreo(ObjectProvider<JavaMailSender> mailSender,
                          @Qualifier("applicationTaskExecutor") TaskExecutor executor,
                          @Value("${sicares.notificaciones.correo-habilitado}") boolean habilitado,
                          @Value("${sicares.notificaciones.remitente}") String remitente,
                          @Value("${spring.mail.username:}") String usuarioSmtp,
                          @Value("${spring.mail.password:}") String claveSmtp) {
        this.mailSender = mailSender;
        this.executor = executor;
        this.habilitado = habilitado;
        this.remitente = remitente;
        this.usuarioSmtp = usuarioSmtp;
        this.claveSmtp = claveSmtp;
    }

    /** Deja en el log el estado del correo para no tener que adivinar por qué no llegan. */
    @EventListener(ApplicationReadyEvent.class)
    public void informarConfiguracion() {
        if (!habilitado) {
            log.warn("Correo DESHABILITADO (MAIL_ENABLED=false): las notificaciones solo se verán dentro de la app. "
                    + "Para enviarlas a Gmail configure MAIL_ENABLED=true, MAIL_USER y MAIL_PASSWORD.");
        } else if (usuarioSmtp.isBlank() || claveSmtp.isBlank()) {
            log.error("Correo habilitado pero falta MAIL_USER o MAIL_PASSWORD: Gmail rechazará los envíos.");
        } else {
            log.info("Correo habilitado: se envía desde {} por {}", remitente, usuarioSmtp);
        }
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    /** Programa el envío para cuando termine la transacción actual (o de inmediato si no hay transacción). */
    public void enviar(String para, String asunto, String cuerpo) {
        if (!habilitado || para == null || para.isBlank()) {
            return;
        }
        Runnable tarea = () -> executor.execute(() -> {
            try {
                enviarAhora(para, asunto, cuerpo);
            } catch (Exception e) {
                // ADR-06: un fallo de correo no rompe el proceso; la notificación ya está guardada en la app
                log.error("No se pudo enviar el correo a {}: {}", para, explicar(e));
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tarea.run();
                }
            });
        } else {
            tarea.run();
        }
    }

    /** Envía un correo de prueba en este mismo hilo y devuelve qué pasó (para el botón del panel del admin). */
    public ResultadoPrueba probar(String para) {
        if (!habilitado) {
            return new ResultadoPrueba(false, "El correo está deshabilitado. En el servidor ponga MAIL_ENABLED=true "
                    + "en /etc/sicares/sicares.env (en local, en las variables de entorno de IntelliJ) y reinicie el backend.");
        }
        if (usuarioSmtp.isBlank() || claveSmtp.isBlank()) {
            return new ResultadoPrueba(false, "Falta MAIL_USER o MAIL_PASSWORD (la contraseña de aplicación de Gmail).");
        }
        if (para == null || para.isBlank()) {
            return new ResultadoPrueba(false, "Indique un correo de destino.");
        }
        try {
            enviarAhora(para, "Correo de prueba",
                    "Si estás leyendo esto, SICARES ya puede enviar notificaciones y recordatorios a Gmail.");
            return new ResultadoPrueba(true, "Correo enviado a " + para + ". Revise también la carpeta de spam.");
        } catch (Exception e) {
            log.error("Falló el correo de prueba a {}: {}", para, explicar(e));
            return new ResultadoPrueba(false, explicar(e));
        }
    }

    private void enviarAhora(String para, String asunto, String cuerpo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("No hay JavaMailSender configurado (revise spring.mail en application.yml)");
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("SICARES <" + remitente + ">");
        msg.setTo(para.trim());
        msg.setSubject("[SICARES] " + asunto);
        msg.setText(cuerpo + "\n\n— SICARES, Sistema de Caracterización y Seguimiento Educativo");
        sender.send(msg);
        log.info("Correo enviado a {}: {}", para, asunto);
    }

    /** Traduce los errores típicos de Gmail a algo que se pueda corregir. */
    static String explicar(Exception e) {
        if (e instanceof MailAuthenticationException) {
            return "Gmail rechazó el usuario o la contraseña. MAIL_PASSWORD debe ser una 'contraseña de aplicación' "
                    + "de 16 letras (requiere verificación en 2 pasos), no la contraseña normal de la cuenta.";
        }
        String detalle = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (detalle.contains("timed out") || detalle.contains("connect")) {
            return "No hubo conexión con smtp.gmail.com:587 (" + detalle + "). Revise la salida a internet del servidor.";
        }
        return detalle;
    }

    /** Resultado del correo de prueba. */
    public record ResultadoPrueba(boolean enviado, String detalle) {
    }
}
