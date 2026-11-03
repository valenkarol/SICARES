package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.notifications.EnviadorCorreo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de EnviadorCorreo — HU-26 (correo a Gmail) y ADR-06 (un fallo de correo no rompe nada).
 * El executor corre la tarea en el mismo hilo para poder verificar el envío.
 * Dueño: Integrante 1.
 */
@ExtendWith(MockitoExtension.class)
class EnviadorCorreoTest {

    private static final TaskExecutor MISMO_HILO = Runnable::run;

    @Mock private ObjectProvider<JavaMailSender> proveedor;
    @Mock private JavaMailSender mailSender;

    private EnviadorCorreo enviador(boolean habilitado, String usuario, String clave) {
        return new EnviadorCorreo(proveedor, MISMO_HILO, habilitado, "sicares@gmail.com", usuario, clave);
    }

    @Test
    @DisplayName("Con MAIL_ENABLED=false no intenta enviar (por eso antes no llegaba nada a Gmail)")
    void deshabilitadoNoEnvia() {
        enviador(false, "sicares@gmail.com", "clave").enviar("ana@correo.com", "Asunto", "Mensaje");

        verifyNoInteractions(proveedor);
    }

    @Test
    @DisplayName("Habilitado: envía el correo con remitente, destino y asunto de SICARES")
    void habilitadoEnvia() {
        when(proveedor.getIfAvailable()).thenReturn(mailSender);

        enviador(true, "sicares@gmail.com", "clave").enviar("ana@correo.com", "Asunto", "Mensaje");

        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        assertArrayEquals(new String[]{"ana@correo.com"}, msg.getValue().getTo());
        assertEquals("[SICARES] Asunto", msg.getValue().getSubject());
        assertTrue(msg.getValue().getFrom().contains("sicares@gmail.com"));
    }

    @Test
    @DisplayName("ADR-06: si Gmail falla, enviar() no lanza excepción")
    void falloNoRompe() {
        when(proveedor.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailAuthenticationException("535 Bad credentials")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> enviador(true, "sicares@gmail.com", "clave").enviar("ana@correo.com", "A", "M"));
    }

    @Test
    @DisplayName("Correo de prueba: explica qué falta cuando está deshabilitado o sin contraseña")
    void pruebaExplicaConfiguracion() {
        EnviadorCorreo.ResultadoPrueba apagado = enviador(false, "", "").probar("ana@correo.com");
        EnviadorCorreo.ResultadoPrueba sinClave = enviador(true, "sicares@gmail.com", "").probar("ana@correo.com");

        assertFalse(apagado.enviado());
        assertTrue(apagado.detalle().contains("MAIL_ENABLED"));
        assertFalse(sinClave.enviado());
        assertTrue(sinClave.detalle().contains("MAIL_PASSWORD"));
    }

    @Test
    @DisplayName("Correo de prueba: si Gmail rechaza la clave, sugiere la contraseña de aplicación")
    void pruebaClaveIncorrecta() {
        when(proveedor.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailAuthenticationException("535 Bad credentials")).when(mailSender).send(any(SimpleMailMessage.class));

        EnviadorCorreo.ResultadoPrueba r = enviador(true, "sicares@gmail.com", "clave").probar("ana@correo.com");

        assertFalse(r.enviado());
        assertTrue(r.detalle().contains("contraseña de aplicación"));
    }
}
