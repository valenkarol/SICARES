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

/** Proceso 2: HU-18 Generar ruta recomendada y HU-19 Consultar rutas (Beneficiario.solicitarRuta). */
@Slf4j
@Service
@RequiredArgsConstructor
public class RutaEducativaService {

    private final RutaEducativaRepository rutaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MotorReglas motorReglas;

    @Transactional
    public RutaDtos.RutaResponse solicitarRuta(Long beneficiarioId) {
        Beneficiario b = beneficiarioRepository.findById(beneficiarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", beneficiarioId));

        long inicio = System.nanoTime();
        RutaEducativa nueva = motorReglas.generarRuta(b.getPerfil());
        // KPI "tiempo promedio de generación de una ruta" (métrica 3.4.2 del Plan de Calidad)
        nueva.setDuracionGeneracionMs((System.nanoTime() - inicio) / 1_000_000);
        // RNF-14: huella de la oferta con la que se calculó cada recomendación
        nueva.getRecomendaciones()
                .forEach(r -> r.setHuellaOferta(HuellaIntegridad.deOportunidad(r.getOportunidad())));
        if (nueva.getRecomendaciones().isEmpty()) {
            throw new ReglaNegocioException("Por ahora no hay oportunidades vigentes compatibles con tu perfil. "
                    + "Revisa tu caracterización o vuelve a intentarlo cuando se publique nueva oferta.");
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
        return RutaMapper.toDto(rutaRepository.save(nueva));
    }

    /**
     * Ruta más reciente (puede estar DESACTUALIZADA si el perfil o la oferta cambiaron).
     * Antes de devolverla verifica que la oferta no se haya alterado (RNF-14), por eso no es readOnly.
     */
    @Transactional
    public Optional<RutaDtos.RutaResponse> consultarActual(Long beneficiarioId) {
        return rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(beneficiarioId)
                .map(ruta -> {
                    verificarIntegridad(ruta);
                    return RutaMapper.toDto(ruta);
                });
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
