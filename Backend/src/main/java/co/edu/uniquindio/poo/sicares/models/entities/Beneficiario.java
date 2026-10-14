package co.edu.uniquindio.poo.sicares.models.entities;

import com.sicares.models.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Beneficiario: hereda de Usuario.
 *  - 1 "Tiene" 1 PerfilEducativo (composición)
 *  - 1 "otorga" 1 AutorizacionDatos
 *  - "Realiza" 0..* Postulacion y recibe 0..* RutaEducativa (se consultan por repositorio).
 * Los métodos del UML (completarCaracterizacion, solicitarRuta, seleccionarOportunidad, ...)
 * están en CaracterizacionService, RutaEducativaService y PostulacionService.
 */
@Entity
@Table(name = "beneficiarios")
@PrimaryKeyJoinColumn(name = "usuario_id")
@Getter
@Setter
public class Beneficiario extends Usuario {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id", unique = true)
    private PerfilEducativo perfil;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizacion_id", unique = true)
    private AutorizacionDatos autorizacion;

    public Beneficiario() {
        setRol(Rol.BENEFICIARIO);
    }

    /** Crea el perfil vacío la primera vez que se necesita. */
    public PerfilEducativo obtenerOCrearPerfil() {
        if (perfil == null) {
            perfil = new PerfilEducativo();
        }
        return perfil;
    }
}
