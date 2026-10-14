package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PerfilEducativo (paquete "Caracterización del usuario").
 * Composición con: InformacionPersonal (1), SituacionVictima (0..1), SituacionSocioeconomica (1),
 * CondicionAccesibilidad (1), InteresProfesional (1..*), TrayectoriaEducativa (1).
 */
@Entity
@Table(name = "perfiles_educativos")
@Getter
@Setter
@NoArgsConstructor
public class PerfilEducativo {

    /** Número de secciones que componen la caracterización. */
    private static final int TOTAL_SECCIONES = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(nullable = false)
    private boolean completo;

    @Column(name = "porcentaje_completitud", nullable = false)
    private int porcentajeCompletitud;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "informacion_personal_id")
    private InformacionPersonal informacionPersonal;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "situacion_victima_id")
    private SituacionVictima situacionVictima;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "situacion_socioeconomica_id")
    private SituacionSocioeconomica situacionSocioeconomica;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "condicion_accesibilidad_id")
    private CondicionAccesibilidad condicionAccesibilidad;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "trayectoria_educativa_id")
    private TrayectoriaEducativa trayectoriaEducativa;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "perfil_id")
    private List<InteresProfesional> intereses = new ArrayList<>();

    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
        actualizar();
    }

    public int calcularPorcentaje() {
        int secciones = 0;
        if (informacionPersonal != null) secciones++;
        if (situacionVictima != null) secciones++;
        if (situacionSocioeconomica != null) secciones++;
        if (condicionAccesibilidad != null) secciones++;
        if (trayectoriaEducativa != null) secciones++;
        if (intereses != null && !intereses.isEmpty()) secciones++;
        return secciones * 100 / TOTAL_SECCIONES;
    }

    public boolean validarCompletitud() {
        return calcularPorcentaje() == 100
                && informacionPersonal.validarRangoEdad();
    }

    /** Recalcula completitud y marca la fecha de actualización. */
    public void actualizar() {
        this.porcentajeCompletitud = calcularPorcentaje();
        this.completo = validarCompletitud();
        this.fechaActualizacion = LocalDateTime.now();
    }

    public void reemplazarIntereses(List<InteresProfesional> nuevos) {
        this.intereses.clear();
        this.intereses.addAll(nuevos);
    }
}
