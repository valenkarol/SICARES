package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Información protegida por la Ley 1448 de 2011 (reserva de la información de víctimas). */
@Entity
@Table(name = "situacion_victima")
@Getter
@Setter
@NoArgsConstructor
public class SituacionVictima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "es_victima", nullable = false)
    private boolean esVictima;

    @Column(name = "inscrito_ruv", nullable = false)
    private boolean inscritoRUV;

    @Column(name = "hecho_victimizante", length = 100)
    private String hechoVictimizante;

    @Column(name = "fecha_hecho")
    private LocalDate fechaHecho;

    @Column(name = "municipio_expulsor", length = 60)
    private String municipioExpulsor;
}
