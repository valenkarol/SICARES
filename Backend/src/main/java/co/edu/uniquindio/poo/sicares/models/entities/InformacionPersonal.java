package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.TipoDocumento;
import co.edu.uniquindio.poo.sicares.models.enums.Zona;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "informacion_personal")
@Getter
@Setter
@NoArgsConstructor
public class InformacionPersonal {

    public static final int EDAD_MINIMA = 12;
    public static final int EDAD_MAXIMA = 59;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 5)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 30)
    private String genero;

    @Column(length = 50)
    private String etnia;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 60)
    private String departamento;

    @Column(nullable = false, length = 60)
    private String municipio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Zona zona;

    /** Adulto responsable; obligatorio solo si el beneficiario es menor de edad. */
    @Embedded
    private ResponsableMenor responsable;

    public int calcularEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /** Población objetivo de SICARES: 12 a 59 años. */
    public boolean validarRangoEdad() {
        if (fechaNacimiento == null) return false;
        int edad = calcularEdad();
        return edad >= EDAD_MINIMA && edad <= EDAD_MAXIMA;
    }

    public boolean esMenorDeEdad() {
        return fechaNacimiento != null && calcularEdad() < ResponsableMenor.MAYORIA_EDAD;
    }
}
