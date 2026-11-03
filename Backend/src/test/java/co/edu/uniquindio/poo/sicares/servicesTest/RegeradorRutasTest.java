package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.RutaDtos.RutaResponse;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.notifications.PerfilActualizadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.services.RegeneradorRutas;
import co.edu.uniquindio.poo.sicares.services.RutaEducativaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de RegeneradorRutas — ruta automática al cambiar el perfil y aviso de caracterización completa.
 */
@ExtendWith(MockitoExtension.class)
class RegeneradorRutasTest {

    private static final Long ID = 1L;

    @Mock private RutaEducativaService rutaService;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private ServicioNotificaciones notificaciones;
    @Mock private PlatformTransactionManager transactionManager;

    private RegeneradorRutas regenerador;
    private Beneficiario beneficiario;

    @BeforeEach
    void setUp() {
        regenerador = new RegeneradorRutas(rutaService, beneficiarioRepository, notificaciones, transactionManager);
        beneficiario = new Beneficiario();
        beneficiario.setId(ID);
    }

    /** Ruta de respuesta con N recomendaciones */
    private static RutaResponse ruta(int version, int recomendaciones) {
        return new RutaResponse(10L, LocalDateTime.now(), EstadoRuta.VIGENTE, version,
                Collections.nCopies(recomendaciones, null));
    }

    @Test
    @DisplayName("Perfil incompleto: no hace nada (ni abre transacción)")
    void perfilIncompleto() {
        regenerador.onPerfilActualizado(new PerfilActualizadoEvent(ID, false, false));

        verifyNoInteractions(rutaService, notificaciones, transactionManager);
    }

    @Test
    @DisplayName("Primera vez al 100 %: avisa que completó la caracterización y que su ruta está lista")
    void completoPorPrimeraVez() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        RutaResponse nueva = ruta(1, 3);
        when(rutaService.regenerarAutomaticamente(ID))
                .thenReturn(Optional.of(new RutaEducativaService.Regeneracion(nueva, true)));

        regenerador.procesar(new PerfilActualizadoEvent(ID, true, true));

        verify(notificaciones).notificar(eq(beneficiario), isNull(), eq("¡Completaste tu caracterización!"), anyString());
        verify(notificaciones).notificar(eq(beneficiario), isNull(), eq("Tu ruta educativa se actualizó"),
                contains("3 oportunidades"));
    }

    @Test
    @DisplayName("Si las oportunidades no cambiaron, actualiza la ruta sin enviar otra notificación")
    void sinCambiosNoNotifica() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(rutaService.regenerarAutomaticamente(ID))
                .thenReturn(Optional.of(new RutaEducativaService.Regeneracion(ruta(2, 3), false)));

        regenerador.procesar(new PerfilActualizadoEvent(ID, true, false));

        verifyNoInteractions(notificaciones);
    }

    @Test
    @DisplayName("Sin oferta compatible: no notifica cambio de ruta ni lanza error")
    void sinOferta() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(rutaService.regenerarAutomaticamente(ID)).thenReturn(Optional.empty());

        regenerador.procesar(new PerfilActualizadoEvent(ID, true, false));

        verify(notificaciones, never()).notificar(any(), any(), eq("Tu ruta educativa se actualizó"), anyString());
    }

    @Test
    @DisplayName("Un error al regenerar no se propaga (el perfil ya quedó guardado)")
    void errorNoSePropaga() {
        when(transactionManager.getTransaction(any())).thenThrow(new IllegalStateException("BD caída"));

        assertDoesNotThrow(
                () -> regenerador.onPerfilActualizado(new PerfilActualizadoEvent(ID, true, true)));
        verify(rutaService, never()).regenerarAutomaticamente(any());
    }
}
