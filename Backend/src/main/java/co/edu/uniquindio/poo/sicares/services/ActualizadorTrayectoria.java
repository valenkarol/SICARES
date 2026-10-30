package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoEstudio;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.notifications.PostulacionEstadoCambiadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * <<service>> ActualizadorTrayectoria (HU-27 Retroalimentación).
 * Escucha los cambios de estado de las postulaciones: notifica al beneficiario y, cuando se
 * matricula, actualiza su trayectoria educativa y marca sus rutas como desactualizadas para
 * que pueda generar una nueva ruta acorde a sus nuevas condiciones.
 */
@Service
@RequiredArgsConstructor
public class ActualizadorTrayectoria {

    private final ServicioNotificaciones servicioNotificaciones;
    private final RutaEducativaRepository rutaRepository;

    /** Se ejecuta dentro de la misma transacción de quien publicó el evento. */
    @EventListener
    public void onEstadoCambiado(PostulacionEstadoCambiadoEvent evento) {
        Postulacion p = evento.postulacion();
        String programa = p.getOportunidad().getPrograma().getNombre();

        String mensaje = "Tu postulación a " + programa + " cambió de " + evento.estadoAnterior()
                + " a " + evento.estadoNuevo() + "."
                + (evento.observacion() != null && !evento.observacion().isBlank()
                ? " Observación: " + evento.observacion() : "");
        servicioNotificaciones.notificar(p.getBeneficiario(), p, "Actualización de tu postulación", mensaje);

        if (evento.estadoNuevo() == EstadoPostulacion.MATRICULADA) {
            actualizarTrayectoria(p.getBeneficiario(), p.getOportunidad().getPrograma());
        }
    }

    /** actualizarTrayectoria(b, p): registra el programa como estudio EN_CURSO. */
    public void actualizarTrayectoria(Beneficiario b, ProgramaEducativo programa) {
        PerfilEducativo perfil = b.getPerfil();
        if (perfil == null || perfil.getTrayectoriaEducativa() == null) return;

        EstudioRealizado estudio = new EstudioRealizado();
        estudio.setNivel(programa.getNivel());
        estudio.setInstitucion(programa.getInstitucion().getNombre());
        estudio.setTitulo(programa.getNombre());
        estudio.setEstado(EstadoEstudio.EN_CURSO);
        estudio.setAnioInicio(LocalDate.now().getYear());
        perfil.getTrayectoriaEducativa().agregarEstudio(estudio);
        perfil.getTrayectoriaEducativa().setAnioUltimoEstudio(LocalDate.now().getYear());
        perfil.actualizar();

        rutaRepository.findByBeneficiarioIdAndEstadoNot(b.getId(), EstadoRuta.DESACTUALIZADA)
                .forEach(RutaEducativa::marcarDesactualizada);

        servicioNotificaciones.notificar(b, null, "Tu trayectoria educativa fue actualizada",
                "Registramos que te matriculaste en " + programa.getNombre()
                        + ". Puedes generar una nueva ruta educativa con tu información actualizada.");
    }
}
