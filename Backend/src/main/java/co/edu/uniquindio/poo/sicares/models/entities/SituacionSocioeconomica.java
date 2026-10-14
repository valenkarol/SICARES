package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "situacion_socioeconomica")
@Getter
@Setter
@NoArgsConstructor
public class SituacionSocioeconomica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Grupo SISBÉN IV, ej. "A1", "B3", "C2", "D10" o "NO_TIENE". */
    @Column(name = "grupo_sisben", length = 10)
    private String grupoSisben;

    @Column(nullable = false)
    private int estrato;

    @Column(name = "ingreso_mensual_hogar", precision = 14, scale = 2)
    private BigDecimal ingresoMensualHogar;

    @Column(name = "situacion_laboral", length = 50)
    private String situacionLaboral;

    @Column(name = "integrantes_hogar", nullable = false)
    private int integrantesHogar;

    @Column(name = "personas_a_cargo", nullable = false)
    private int personasACargo;
}
