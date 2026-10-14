package co.edu.uniquindio.poo.sicares.models.entities;


import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** TrayectoriaEducativa 1 ◆── 1..* EstudioRealizado */
@Entity
@Table(name = "trayectorias_educativas")
@Getter
@Setter
@NoArgsConstructor
public class TrayectoriaEducativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Último nivel aprobado completamente. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_actual", nullable = false, length = 25)
    private NivelEducativo nivelActual;

    @Column(name = "ultimo_grado_aprobado")
    private int ultimoGradoAprobado;

    @Column(name = "anio_ultimo_estudio")
    private int anioUltimoEstudio;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "trayectoria_id")
    private List<EstudioRealizado> estudios = new ArrayList<>();

    public void agregarEstudio(EstudioRealizado e) {
        estudios.add(e);
    }

    public void actualizarNivel(NivelEducativo n) {
        // Solo sube de nivel, nunca baja.
        if (nivelActual == null || n.ordinal() > nivelActual.ordinal()) {
            this.nivelActual = n;
        }
    }
}
