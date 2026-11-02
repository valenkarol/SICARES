package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** RutaEducativa 1 ◆── 1..* Recomendacion. Beneficiario 1 "Recibe" 0..* RutaEducativa. */
@Entity
@Table(name = "rutas_educativas")
@Getter
@Setter
@NoArgsConstructor
public class RutaEducativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRuta estado = EstadoRuta.GENERADA;

    @Column(nullable = false)
    private int version;

    /** KPI "tiempo promedio de generación de una ruta" y métrica 3.4.2 del Plan de Calidad. */
    @Column(name = "duracion_generacion_ms")
    private Long duracionGeneracionMs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Beneficiario beneficiario;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Recomendacion> recomendaciones = new ArrayList<>();

    public void agregarRecomendacion(Recomendacion r) {
        r.setRuta(this);
        recomendaciones.add(r);
    }

    /** Ordena de mayor a menor puntaje y reasigna el campo "orden" (1, 2, 3...). */
    public void ordenarPorCompatibilidad() {
        recomendaciones.sort(Comparator.comparingDouble(Recomendacion::getPuntajeCompatibilidad).reversed());
        for (int i = 0; i < recomendaciones.size(); i++) {
            recomendaciones.get(i).setOrden(i + 1);
        }
    }

    public void marcarDesactualizada() {
        this.estado = EstadoRuta.DESACTUALIZADA;
    }
}
