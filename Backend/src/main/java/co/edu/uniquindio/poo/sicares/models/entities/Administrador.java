package co.edu.uniquindio.poo.sicares.models.entities;



import co.edu.uniquindio.poo.sicares.models.enums.Rol;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Administrador: hereda de Usuario.
 * Sus operaciones del UML (gestionarInstitucion, gestionarPrograma, gestionarRequisito,
 * gestionarBeneficio, actualizarEstadoPostulacion, gestionarUsuarios) se exponen en los
 * controladores protegidos con el rol ADMINISTRADOR.
 */
@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Administrador extends Usuario {

    public Administrador() {
        setRol(Rol.ADMINISTRADOR);
    }
}
