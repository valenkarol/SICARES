package co.edu.uniquindio.poo.sicares.repository;

import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Long> {

    long countByPerfilCompletoTrue();

    /** RNF-13: ¿otro beneficiario ya se caracterizó con ese número de documento? */
    boolean existsByPerfilInformacionPersonalNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
}
