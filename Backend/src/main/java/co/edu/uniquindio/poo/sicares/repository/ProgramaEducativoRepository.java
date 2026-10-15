package co.edu.uniquindio.poo.sicares.repository;

import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgramaEducativoRepository extends JpaRepository<ProgramaEducativo, Long> {

    /** consulta con filtros opcionales, si un parámetro llega null no filtra por el */
    @Query("""
            SELECT p FROM ProgramaEducativo p
            WHERE (:soloActivos = false OR p.activo = true)
              AND (:nivel IS NULL OR p.nivel = :nivel)
              AND (:modalidad IS NULL OR p.modalidad = :modalidad)
              AND (:municipio IS NULL OR LOWER(p.municipio) = LOWER(:municipio))
              AND (:area IS NULL OR LOWER(p.areaConocimiento) LIKE LOWER(CONCAT('%', :area, '%')))
              AND (:texto IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
            ORDER BY p.nombre
            """)
    List<ProgramaEducativo> buscar(@Param("soloActivos") boolean soloActivos,
                                   @Param("nivel") NivelEducativo nivel,
                                   @Param("modalidad") Modalidad modalidad,
                                   @Param("municipio") String municipio,
                                   @Param("area") String area,
                                   @Param("texto") String texto);

    List<ProgramaEducativo> findByInstitucionId(Long institucionId);
}
