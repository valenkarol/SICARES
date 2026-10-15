package co.edu.uniquindio.poo.sicares.repository;


import co.edu.uniquindio.poo.sicares.models.entities.RutaEducativa;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RutaEducativaRepository extends JpaRepository<RutaEducativa, Long> {

    Optional<RutaEducativa> findFirstByBeneficiarioIdOrderByVersionDesc(Long beneficiarioId);

    List<RutaEducativa> findByBeneficiarioIdOrderByVersionDesc(Long beneficiarioId);

    List<RutaEducativa> findByBeneficiarioIdAndEstadoNot(Long beneficiarioId, EstadoRuta estado);
}
