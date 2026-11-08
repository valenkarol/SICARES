package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.ElegibilidadResponse;
import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.PostulacionResponse;
import co.edu.uniquindio.poo.sicares.dto.PostulacionMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;
import co.edu.uniquindio.poo.sicares.models.entities.Postulacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import co.edu.uniquindio.poo.sicares.repository.PostulacionRepository;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaCompatibilidad;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaNivelEducativo;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaRangoEdad;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaRequisitos;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ResultadoEvaluacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Auditoría - Plan de mejoramiento #2 ("Rigidez en la recomendación de rutas").
 * Permite que el beneficiario se postule a cualquier convocatoria vigente del catálogo de "Ofertas",
 * aunque no coincida con los intereses o el nivel que marcó en su caracterización.
 * La postulación queda igual que una creada desde la ruta, pero sin recomendación asociada
 * (la relación Postulacion -> Recomendacion es 0..1 en el diagrama de clases).
 *
 * Elegibilidad: se quita la restricción por INTERESES, pero NO la de requisitos. Antes de crear la
 * postulación se aplican las mismas reglas eliminatorias del motor (patrón Strategy) que son requisitos reales:
 *  - ReglaNivelEducativo: nivel previo exigido y requisitos NIVEL_EDUCATIVO (ej. un usuario con grado 9° no puede
 *    postularse a Enfermería, que exige haber culminado la media / grado 11).
 *  - ReglaRangoEdad: requisitos EDAD.
 *  - ReglaRequisitos: requisitos CONDICION_VICTIMA (RUV) y SISBEN obligatorios.
 * No se aplican ReglaIntereses (es justo lo que pidió flexibilizar la auditoría) ni ReglaAccesibilidad
 * (ubicación/transporte/internet son decisiones del usuario, no requisitos de admisión).
 */
@Service
@RequiredArgsConstructor
public class PostulacionDirectaService {

    static final String ORIGEN_CATALOGO = "Oportunidad seleccionada desde el catálogo de ofertas";

    private final PostulacionRepository postulacionRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OportunidadEducativaRepository oportunidadRepository;
    private final ServicioNotificaciones servicioNotificaciones;
    private final ReglaNivelEducativo reglaNivelEducativo;
    private final ReglaRangoEdad reglaRangoEdad;
    private final ReglaRequisitos reglaRequisitos;

    @Transactional
    public PostulacionResponse postularDesdeCatalogo(Long beneficiarioId, Long oportunidadId) {
        Beneficiario b = beneficiarioRepository.findById(beneficiarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", beneficiarioId));
        OportunidadEducativa op = oportunidadRepository.findById(oportunidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Oportunidad", oportunidadId));

        if (!op.getPrograma().isActivo()) {
            throw new ReglaNegocioException("Este programa ya no se está ofreciendo");
        }
        if (!op.estaVigente()) {
            throw new ReglaNegocioException("La convocatoria ya cerró o no tiene cupos");
        }
        boolean yaExiste = postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(
                beneficiarioId, op.getId(),
                List.of(EstadoPostulacion.CERRADA, EstadoPostulacion.NO_ADMITIDA));
        if (yaExiste) {
            throw new ReglaNegocioException("Ya tienes una postulación activa para esta oportunidad");
        }
        // No cumple los requisitos del programa -> no se crea la postulación
        String motivo = motivoNoElegible(b.getPerfil(), op);
        if (motivo != null) {
            throw new ReglaNegocioException("No puedes postularte a este programa: " + motivo);
        }

        Postulacion p = new Postulacion();
        p.setBeneficiario(b);
        p.setOportunidad(op);
        p.setRecomendacion(null); // no viene de la ruta educativa
        p.iniciar(b, ORIGEN_CATALOGO);
        servicioNotificaciones.programarRecordatorios(p); // HU-26
        p = postulacionRepository.save(p);

        servicioNotificaciones.notificar(b, p, "Seleccionaste una oportunidad educativa",
                "Seleccionaste " + op.getPrograma().getNombre() + " (" + op.getPeriodoAcademico() + ") desde el "
                        + "catálogo de ofertas. Revisa los requisitos y carga tus documentos antes del "
                        + op.getFechaCierreInscripcion() + ".");
        return PostulacionMapper.toDto(p);
    }

    /**
     * Para la pantalla de Ofertas: indica, por cada convocatoria vigente, si el beneficiario puede postularse
     * y, si no, por qué. Así el botón "Postularme" se deshabilita con el motivo antes de intentarlo.
     */
    @Transactional(readOnly = true)
    public List<ElegibilidadResponse> elegibilidad(Long beneficiarioId) {
        Beneficiario b = beneficiarioRepository.findById(beneficiarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", beneficiarioId));
        return oportunidadRepository.findVigentes(LocalDate.now()).stream()
                .map(op -> {
                    String motivo = motivoNoElegible(b.getPerfil(), op);
                    return new ElegibilidadResponse(op.getId(), motivo == null, motivo);
                })
                .toList();
    }

    /** null si cumple todos los requisitos; si no, el motivo (texto para el usuario). */
    String motivoNoElegible(PerfilEducativo perfil, OportunidadEducativa op) {
        if (perfil == null || !perfil.isCompleto()) {
            return "completa tu caracterización (Mi perfil al 100%) para verificar los requisitos";
        }
        for (ReglaCompatibilidad regla : List.of(reglaNivelEducativo, reglaRangoEdad, reglaRequisitos)) {
            ResultadoEvaluacion r = regla.evaluar(perfil, op);
            if (!r.isCumple()) {
                return r.getObservaciones().isEmpty() ? "no cumples los requisitos del programa"
                        : String.join(". ", r.getObservaciones());
            }
        }
        return null;
    }
}

