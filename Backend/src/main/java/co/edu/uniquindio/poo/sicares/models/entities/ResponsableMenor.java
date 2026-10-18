package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

/**
 * Adulto responsable de un beneficiario menor de edad (HU-04).
 * Se guarda embebido en la tabla informacion_personal (columnas responsable_*);
 * queda en null cuando el beneficiario es mayor de edad.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ResponsableMenor {

    public static final int MAYORIA_EDAD = 18;

    @Column(name = "responsable_nombres", length = 80)
    private String nombres;

    @Column(name = "responsable_apellidos", length = 80)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsable_tipo_documento", length = 5)
    private TipoDocumento tipoDocumento;

    @Column(name = "responsable_numero_documento", length = 20)
    private String numeroDocumento;

    @Column(name = "responsable_fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "responsable_parentesco", length = 40)
    private String parentesco;

    @Column(name = "responsable_telefono", length = 20)
    private String telefono;

    @Column(name = "responsable_correo", length = 120)
    private String correo;

    public int calcularEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public boolean esMayorDeEdad() {
        return fechaNacimiento != null && calcularEdad() >= MAYORIA_EDAD;
    }
}
