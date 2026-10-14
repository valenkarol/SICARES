package co.edu.uniquindio.poo.sicares.models.entities;


import com.sicares.models.enums.Modalidad;
import com.sicares.models.enums.NivelEducativo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ProgramaEducativo
 *  - pertenece a 1 Institucion
 *  - "exige" 0..* Requisito (tabla programa_requisito)
 *  - 1 ◆── 0..* OportunidadEducativa
 *  - Beneficio "aplica a" 0..* ProgramaEducativo (lado inverso)
 */
@Entity
@Table(name = "programas_educativos")
@Getter
@Setter
@NoArgsConstructor
public class ProgramaEducativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private NivelEducativo nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Modalidad modalidad;

    @Column(name = "area_conocimiento", nullable = false, length = 80)
    private String areaConocimiento;

    @Column(name = "duracion_periodos", nullable = false)
    private int duracionPeriodos;

    @Column(name = "costo_periodo", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoPeriodo = BigDecimal.ZERO;

    @Column(nullable = false, length = 60)
    private String municipio;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institucion_id", nullable = false)
    private Institucion institucion;

    @ManyToMany
    @JoinTable(name = "programa_requisito",
            joinColumns = @JoinColumn(name = "programa_id"),
            inverseJoinColumns = @JoinColumn(name = "requisito_id"))
    private Set<Requisito> requisitos = new HashSet<>();

    @OneToMany(mappedBy = "programa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OportunidadEducativa> oportunidades = new ArrayList<>();

    @ManyToMany(mappedBy = "programas")
    private Set<Beneficio> beneficios = new HashSet<>();

    public boolean esGratuito() {
        return costoPeriodo == null || costoPeriodo.compareTo(BigDecimal.ZERO) == 0;
    }

    public List<Requisito> getRequisitosObligatorios() {
        return requisitos.stream().filter(Requisito::isObligatorio).toList();
    }
}
