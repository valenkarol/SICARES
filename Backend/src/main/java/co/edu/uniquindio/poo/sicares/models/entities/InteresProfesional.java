package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "intereses_profesionales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InteresProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Debe coincidir con las áreas usadas en ProgramaEducativo.areaConocimiento. */
    @Column(name = "area_conocimiento", nullable = false, length = 80)
    private String areaConocimiento;

    @Column(length = 255)
    private String descripcion;

    public InteresProfesional(String areaConocimiento, String descripcion) {
        this.areaConocimiento = areaConocimiento;
        this.descripcion = descripcion;
    }
}
