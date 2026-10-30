package co.edu.uniquindio.poo.sicares.repository;

import co.edu.uniquindio.poo.sicares.models.entities.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    @Query("SELECT r FROM Recordatorio r WHERE r.enviado = false AND r.fechaProgramada <= :ahora")
    List<Recordatorio> findPendientes(@Param("ahora") LocalDateTime ahora);

    @Query("SELECT r FROM Recordatorio r WHERE r.postulacion.beneficiario.id = :beneficiarioId ORDER BY r.fechaEvento")
    List<Recordatorio> findByBeneficiario(@Param("beneficiarioId") Long beneficiarioId);
}
