package co.edu.uniquindio.poo.sicares.repository;

import co.edu.uniquindio.poo.sicares.models.entities.Postulacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    List<Postulacion> findByBeneficiarioIdOrderByFechaSeleccionDesc(Long beneficiarioId);

    List<Postulacion> findAllByOrderByFechaSeleccionDesc();

    List<Postulacion> findByEstadoOrderByFechaSeleccionDesc(EstadoPostulacion estado);

    boolean existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(Long beneficiarioId, Long oportunidadId,
                                                                 List<EstadoPostulacion> estados);
}
