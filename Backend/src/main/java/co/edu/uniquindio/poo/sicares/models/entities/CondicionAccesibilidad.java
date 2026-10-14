package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "condicion_accesibilidad")
@Getter
@Setter
@NoArgsConstructor
public class CondicionAccesibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tiene_internet", nullable = false)
    private boolean tieneInternet;

    @Column(name = "tiene_dispositivos", nullable = false)
    private boolean tieneDispositivos;

    /** Ej.: "PUBLICO", "PROPIO", "A_PIE", "NINGUNO". */
    @Column(name = "medio_transporte", length = 30)
    private String medioTransporte;

    @Column(name = "distancia_maxima_km", nullable = false)
    private double distanciaMaximaKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_preferida", length = 15)
    private Modalidad modalidadPreferida;
}
