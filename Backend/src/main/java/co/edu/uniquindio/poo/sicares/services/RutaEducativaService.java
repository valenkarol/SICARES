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
import co.edu.uniquindio.poo.sicares.services.motorReglas.MotorReglas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Proceso 2: HU-18 Generar ruta recomendada y HU-19 Consultar rutas (Beneficiario.solicitarRuta). */
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

        RutaEducativa nueva = motorReglas.generarRuta(b.getPerfil());
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

    /** Ruta más reciente (puede estar DESACTUALIZADA si el perfil cambió). */
    @Transactional(readOnly = true)
    public Optional<RutaDtos.RutaResponse> consultarActual(Long beneficiarioId) {
        return rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(beneficiarioId).map(RutaMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<RutaDtos.RutaResponse> historial(Long beneficiarioId) {
        return rutaRepository.findByBeneficiarioIdOrderByVersionDesc(beneficiarioId).stream()
                .map(RutaMapper::toDto).toList();
    }
}
