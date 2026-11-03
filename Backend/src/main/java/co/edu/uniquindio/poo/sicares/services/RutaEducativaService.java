package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.RutaDtos;
import co.edu.uniquindio.poo.sicares.dto.RutaMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.models.entities.RutaEducativa;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.motorReglas.HuellaIntegridad;
import co.edu.uniquindio.poo.sicares.services.motorReglas.MotorReglas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Proceso 2: HU-18 Generar ruta recomendada y HU-19 Consultar rutas (Beneficiario.solicitarRuta).
 * La ruta también se actualiza sola cuando cambia el perfil (regenerarAutomaticamente) o al consultarla
 * si quedó desactualizada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RutaEducativaService {

    private final RutaEducativaRepository rutaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MotorReglas motorReglas;

    static final String SIN_OFERTA = "Por ahora no hay oportunidades vigentes compatibles con tu perfil. "
            + "Revisa tu caracterización o vuelve a intentarlo cuando se publique nueva oferta.";

    /** HU-18: el beneficiario pide su ruta (botón "Generar mi ruta"). */
    @Transactional
    public RutaDtos.RutaResponse solicitarRuta(Long beneficiarioId) {
        Beneficiario b = beneficiarioRepository.findById(beneficiarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", beneficiarioId));
        return generarYGuardar(b)
                .map(RutaMapper::toDto)
                .orElseThrow(() -> new ReglaNegocioException(SIN_OFERTA));
    }

    /**
     * Ruta automática: se llama cuando el beneficiario cambia su perfil (RegeneradorRutas). No lanza error si el
     * perfil está incompleto o no hay oferta compatible: en esos casos devuelve vacío y la ruta anterior queda
     * DESACTUALIZADA. Indica si las oportunidades recomendadas cambiaron, para notificar solo cuando hay novedades.
     */
    @Transactional
    public Optional<Regeneracion> regenerarAutomaticamente(Long beneficiarioId) {
        Beneficiario b = beneficiarioRepository.findById(beneficiarioId).orElse(null);
        if (b == null || b.getPerfil() == null || !b.getPerfil().isCompleto()) {
            return Optional.empty();
        }
        Set<Long> anteriores = rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(beneficiarioId)
                .map(RutaEducativaService::idsOportunidades)
                .orElse(Set.of());
        return generarYGuardar(b)
                .map(nueva -> new Regeneracion(RutaMapper.toDto(nueva), !idsOportunidades(nueva).equals(anteriores)));
    }

    /** Resultado de una regeneración automática. */
    public record Regeneracion(RutaDtos.RutaResponse ruta, boolean cambiaronLasRecomendaciones) {
    }

    /** Corre el motor de reglas y, si hay recomendaciones, guarda la nueva versión VIGENTE. */
    private Optional<RutaEducativa> generarYGuardar(Beneficiario b) {
        Long beneficiarioId = b.getId();
        long inicio = System.nanoTime();
        RutaEducativa nueva = motorReglas.generarRuta(b.getPerfil());
        // KPI "tiempo promedio de generación de una ruta" (métrica 3.4.2 del Plan de Calidad)
        nueva.setDuracionGeneracionMs((System.nanoTime() - inicio) / 1_000_000);
        // RNF-14: huella de la oferta con la que se calculó cada recomendación
        nueva.getRecomendaciones()
                .forEach(r -> r.setHuellaOferta(HuellaIntegridad.deOportunidad(r.getOportunidad())));
        if (nueva.getRecomendaciones().isEmpty()) {
            return Optional.empty();
        }

        // Las rutas anteriores dejan de estar vigentes
        rutaRepository.findByBeneficiarioIdAndEstadoNot(beneficiarioId, EstadoRuta.DESACTUALIZADA)
                .forEach(RutaEducativa::marcarDesactualizada);

        int version = rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(beneficiarioId)
                .map(r -> r.getVersion() + 1).orElse(1);
        nueva.setBeneficiario(b);
        nueva.setVersion(version);
        nueva.setFechaGeneracion(LocalDateTime.now());
        nueva.setEstado(EstadoRuta.VIGENTE);
        return Optional.of(rutaRepository.save(nueva));
    }

    private static Set<Long> idsOportunidades(RutaEducativa ruta) {
        return ruta.getRecomendaciones().stream()
                .map(r -> r.getOportunidad().getId())
                .collect(Collectors.toSet());
    }

    /**
     * Ruta más reciente. Antes de devolverla verifica que la oferta no se haya alterado (RNF-14), por eso no es
     * readOnly. Si quedó DESACTUALIZADA (cambió el perfil, la oferta o se matriculó, HU-27) y el perfil está
     * completo, genera la versión nueva automáticamente; si no hay oferta compatible devuelve la anterior.
     */
    @Transactional
    public Optional<RutaDtos.RutaResponse> consultarActual(Long beneficiarioId) {
        Optional<RutaEducativa> actual = rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(beneficiarioId);
        actual.ifPresent(this::verificarIntegridad);
        if (actual.isPresent() && actual.get().getEstado() == EstadoRuta.DESACTUALIZADA) {
            Optional<RutaEducativa> nueva = beneficiarioRepository.findById(beneficiarioId)
                    .filter(b -> b.getPerfil() != null && b.getPerfil().isCompleto())
                    .flatMap(this::generarYGuardar);
            if (nueva.isPresent()) {
                log.info("Ruta del beneficiario {} regenerada automáticamente (versión {})",
                        beneficiarioId, nueva.get().getVersion());
                return nueva.map(RutaMapper::toDto);
            }
        }
        return actual.map(RutaMapper::toDto);
    }

    /**
     * RNF-14: compara la huella guardada de cada recomendación con la oferta actual.
     * Si alguna cambió (por el administrador o directamente en la base de datos), registra la alteración
     * detectada y marca la ruta como DESACTUALIZADA. Devuelve true si la ruta sigue íntegra.
     */
    public boolean verificarIntegridad(RutaEducativa ruta) {
        if (ruta.getEstado() == EstadoRuta.DESACTUALIZADA) {
            return true;
        }
        boolean alterada = ruta.getRecomendaciones().stream()
                .anyMatch(r -> HuellaIntegridad.fueAlterada(r.getHuellaOferta(), r.getOportunidad()));
        if (alterada) {
            log.warn("RNF-14: alteración detectada en la oferta de la ruta {} (beneficiario {}); se marca DESACTUALIZADA",
                    ruta.getId(), ruta.getBeneficiario() != null ? ruta.getBeneficiario().getId() : null);
            ruta.marcarDesactualizada();
        }
        return !alterada;
    }

    @Transactional(readOnly = true)
    public List<RutaDtos.RutaResponse> historial(Long beneficiarioId) {
        return rutaRepository.findByBeneficiarioIdOrderByVersionDesc(beneficiarioId).stream()
                .map(RutaMapper::toDto).toList();
    }
}
