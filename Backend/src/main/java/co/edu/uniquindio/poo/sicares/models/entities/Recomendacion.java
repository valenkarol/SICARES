package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Recomendacion 0..* ──recomienda── 1 OportunidadEducativa */
@Entity
@Table(name = "recomendaciones")
@Getter
@Setter
@NoArgsConstructor
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int orden;

    /** 0 a 100 */
    @Column(name = "puntaje_compatibilidad", nullable = false)
    private double puntajeCompatibilidad;

    @Column(length = 2000)
    private String justificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private RutaEducativa ruta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidad_id", nullable = false)
    private OportunidadEducativa oportunidad;

    /** RNF-14: huella SHA-256 de la oferta en el momento de generar la ruta (ver HuellaIntegridad). */
    @Column(name = "huella_oferta", length = 64)
    private String huellaOferta;

    /** Beneficios vigentes del programa recomendado. */
    public List<Beneficio> getBeneficiosAplicables() {
        return oportunidad.getPrograma().getBeneficios().stream()
                .filter(Beneficio::estaVigente)
                .toList();
    }
}
