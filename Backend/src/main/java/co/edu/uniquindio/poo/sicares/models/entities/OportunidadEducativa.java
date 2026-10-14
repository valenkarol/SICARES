package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Convocatoria concreta de un ProgramaEducativo para un periodo académico. */
@Entity
@Table(name = "oportunidades_educativas")
@Getter
@Setter
@NoArgsConstructor
public class OportunidadEducativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo_academico", nullable = false, length = 20)
    private String periodoAcademico;

    @Column(name = "fecha_apertura_inscripcion", nullable = false)
    private LocalDate fechaAperturaInscripcion;

    @Column(name = "fecha_cierre_inscripcion", nullable = false)
    private LocalDate fechaCierreInscripcion;

    @Column(name = "fecha_admision")
    private LocalDate fechaAdmision;

    @Column(name = "fecha_matricula")
    private LocalDate fechaMatricula;

    @Column(nullable = false)
    private int cupos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private ProgramaEducativo programa;

    /** Vigente si hoy no ha pasado la fecha de cierre y hay cupos. */
    public boolean estaVigente() {
        return !LocalDate.now().isAfter(fechaCierreInscripcion) && cupos > 0;
    }

    public int diasParaCierre() {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), fechaCierreInscripcion);
    }
}
