package co.edu.uniquindio.poo.sicares.models.entities;

import com.sicares.models.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <<abstract>> Usuario (paquete "Usuario y seguridad").
 * Herencia JOINED: tabla "usuarios" + una tabla por subclase (beneficiarios, administradores).
 * Los métodos iniciarSesion/cerrarSesion/cambiarContrasena del UML se implementan en AuthService,
 * porque requieren el codificador de contraseñas y el token (capa de servicios).
 */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @PrePersist
    protected void alCrear() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public void registrarAcceso() {
        this.ultimoAcceso = LocalDateTime.now();
    }
}
