package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.notifications.PerfilActualizadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Automatiza el proceso 1 → 2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegeneradorRutas {

    private final RutaEducativaService rutaService;
    private final BeneficiarioRepository beneficiarioRepository;
    private final ServicioNotificaciones notificaciones;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPerfilActualizado(PerfilActualizadoEvent evento) {
        if (!evento.perfilCompleto()) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            tx.executeWithoutResult(estado -> procesar(evento));
        } catch (RuntimeException e) {
            log.warn("No se pudo actualizar automáticamente la ruta del beneficiario {}: {}",
                    evento.beneficiarioId(), e.getMessage());
        }
    }

    /** Público para poder probarlo sin transacciones. */
    public void procesar(PerfilActualizadoEvent evento) {
        Beneficiario b = beneficiarioRepository.findById(evento.beneficiarioId()).orElse(null);
        if (b == null) {
            return;
        }
        if (evento.completadoAhora()) {
            notificaciones.notificar(b, null, "¡Completaste tu caracterización!",
                    "Tu perfil está al 100 %. Ya puedes explorar la oferta educativa en SICARES (menú Oferta) "
                            + "y revisar tu ruta educativa recomendada (menú Mi ruta).");
        }
        rutaService.regenerarAutomaticamente(b.getId()).ifPresentOrElse(r -> {
            if (r.cambiaronLasRecomendaciones()) {
                notificaciones.notificar(b, null, "Tu ruta educativa se actualizó",
                        "Con la información de tu perfil generamos la versión " + r.ruta().version()
                                + " de tu ruta, con " + r.ruta().recomendaciones().size()
                                + " oportunidades recomendadas. Revísala en el menú Mi ruta.");
            }
        }, () -> log.info("Beneficiario {}: perfil completo pero sin oferta compatible por ahora", b.getId()));
    }
}
