package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Autorización de tratamiento de datos (Ley 1581 de 2012 / Decreto 1377 de 2013).
 * En el diagrama aparece como la clase con atributos aceptada/fechaAceptacion/versionPolitica
 * unida a Beneficiario con la relación "otorga".
 */
@Entity
@Table(name = "autorizaciones_datos")
@Getter
@Setter
@NoArgsConstructor
public class AutorizacionDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean aceptada;

    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @Column(name = "version_politica", length = 20)
    private String versionPolitica;

    public void aceptar() {
        this.aceptada = true;
        this.fechaAceptacion = LocalDateTime.now();
    }

    public void revocar() {
        this.aceptada = false;
    }
}
