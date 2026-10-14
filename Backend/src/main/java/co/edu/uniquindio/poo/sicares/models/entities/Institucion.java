package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.TipoInstitucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Institucion tiene ProgramaEducativo ; Institucion da Beneficio */
@Entity
@Table(name = "instituciones")
@Getter
@Setter
@NoArgsConstructor
public class Institucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 20)
    private String nit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoInstitucion tipo;

    @Column(nullable = false, length = 60)
    private String departamento;

    @Column(nullable = false, length = 60)
    private String municipio;

    @Column(name = "correo_contacto", length = 120)
    private String correoContacto;

    @OneToMany(mappedBy = "institucion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramaEducativo> programas = new ArrayList<>();

    @OneToMany(mappedBy = "institucion")
    private List<Beneficio> beneficios = new ArrayList<>();

    public void agregarPrograma(ProgramaEducativo p) {
        p.setInstitucion(this);
        programas.add(p);
    }
}
