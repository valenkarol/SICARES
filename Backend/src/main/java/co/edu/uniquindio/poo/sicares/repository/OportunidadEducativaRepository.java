package co.edu.uniquindio.poo.sicares.repository;

import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OportunidadEducativaRepository extends JpaRepository<OportunidadEducativa, Long> {

    /** Oportunidades abiertas (no han cerrado) con cupos y de programas activos. */
    @Query("""
            SELECT o FROM OportunidadEducativa o
            JOIN FETCH o.programa p
            JOIN FETCH p.institucion
            WHERE o.fechaCierreInscripcion >= :hoy
              AND o.cupos > 0
              AND p.activo = true
            """)
    List<OportunidadEducativa> findVigentes(@Param("hoy") LocalDate hoy);

    List<OportunidadEducativa> findByProgramaId(Long programaId);
}
