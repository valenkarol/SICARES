package co.edu.uniquindio.poo.sicares.repository;

import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {

    boolean existsByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Long id);
}
