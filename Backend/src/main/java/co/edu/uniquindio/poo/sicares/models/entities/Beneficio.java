package co.edu.uniquindio.poo.sicares.models.entities;

import com.sicares.models.enums.TipoBeneficio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Beneficio (beca, crédito condonable, subsidio, descuento)
 *  - Institucion 1 "otorga" 0..* Beneficio
 *  - "aplica a" 0..* ProgramaEducativo (tabla beneficio_programa)
 *  - "Exige" 0..* Requisito (tabla beneficio_requisito)
 */
@Entity
@Table(name = "beneficios")
@Getter
@Setter
@NoArgsConstructor
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoBeneficio tipo;

    @Column(length = 500)
    private String descripcion;

    /** 0 a 100 */
    @Column(name = "porcentaje_cobertura", nullable = false)
    private double porcentajeCobertura;

    @Column(name = "exclusivo_victimas", nullable = false)
    private boolean exclusivoVictimas;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @ManyToMany
    @JoinTable(name = "beneficio_programa",
            joinColumns = @JoinColumn(name = "beneficio_id"),
            inverseJoinColumns = @JoinColumn(name = "programa_id"))
    private Set<ProgramaEducativo> programas = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "beneficio_requisito",
            joinColumns = @JoinColumn(name = "beneficio_id"),
            inverseJoinColumns = @JoinColumn(name = "requisito_id"))
    private Set<Requisito> requisitos = new HashSet<>();

    public boolean estaVigente() {
        return fechaLimite == null || !fechaLimite.isBefore(LocalDate.now());
    }
}
