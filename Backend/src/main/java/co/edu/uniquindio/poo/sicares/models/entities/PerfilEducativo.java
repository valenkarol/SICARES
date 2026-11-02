package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.EstadoEstudio;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
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

    /** Momento en que el perfil quedó completo por primera vez (KPI "tiempo promedio para completar la caracterización"). */
    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

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

    /** Completo = las 6 secciones, edad válida y sin contradicciones entre secciones (RNF-01). */
    public boolean validarCompletitud() {
        return calcularPorcentaje() == 100
                && informacionPersonal.validarRangoEdad()
                && inconsistencias().isEmpty();
    }

    /**
     * RNF-01 y RNF-16: contradicciones entre las secciones del perfil.
     * Lista vacía = perfil coherente. CaracterizacionService no deja guardar un cambio que genere alguna.
     * Regla de grados: "nivel actual" es el último nivel aprobado completamente.
     */
    public List<String> inconsistencias() {
        List<String> errores = new ArrayList<>();
        InformacionPersonal info = informacionPersonal;
        TrayectoriaEducativa t = trayectoriaEducativa;
        boolean conNacimiento = info != null && info.getFechaNacimiento() != null;

        if (t != null && t.getNivelActual() != null) {
            int g = t.getUltimoGradoAprobado();
            switch (t.getNivelActual()) {
                case NINGUNO -> {
                    if (g > 4) errores.add("Con grado " + g + " aprobado tu nivel educativo no puede ser 'Ninguno'");
                }
                case BASICA_PRIMARIA -> {
                    if (g < 5 || g > 8) errores.add("Básica primaria terminada corresponde a un último grado aprobado entre 5 y 8");
                }
                case BASICA_SECUNDARIA -> {
                    if (g < 9 || g > 10) errores.add("Básica secundaria terminada corresponde a un último grado aprobado de 9 o 10");
                }
                default -> {
                    if (g != 11) errores.add("Para el nivel " + t.getNivelActual() + " el último grado aprobado debe ser 11");
                }
            }
            for (EstudioRealizado e : t.getEstudios()) {
                if (e.getEstado() == EstadoEstudio.COMPLETO && e.getNivel() != null
                        && e.getNivel().ordinal() > t.getNivelActual().ordinal()) {
                    errores.add("Registraste un estudio de nivel " + e.getNivel()
                            + " completo, pero tu nivel actual es " + t.getNivelActual());
                }
            }
            if (conNacimiento && t.getAnioUltimoEstudio() < info.getFechaNacimiento().getYear() + 5) {
                errores.add("El año del último estudio no es coherente con tu fecha de nacimiento");
            }
        }
        if (conNacimiento && situacionVictima != null && situacionVictima.getFechaHecho() != null
                && situacionVictima.getFechaHecho().isBefore(info.getFechaNacimiento())) {
            errores.add("La fecha del hecho victimizante no puede ser anterior a tu fecha de nacimiento");
        }
        if (condicionAccesibilidad != null && condicionAccesibilidad.getModalidadPreferida() == Modalidad.VIRTUAL
                && !condicionAccesibilidad.isTieneInternet()) {
            errores.add("Elegiste la modalidad virtual como preferida pero indicaste que no tienes internet");
        }
        return errores;
    }

    /** Recalcula completitud y marca la fecha de actualización. */
    public void actualizar() {
        this.porcentajeCompletitud = calcularPorcentaje();
        this.completo = validarCompletitud();
        this.fechaActualizacion = LocalDateTime.now();
        if (completo && fechaCompletado == null) {
            this.fechaCompletado = fechaActualizacion;
        }
    }

    public void reemplazarIntereses(List<InteresProfesional> nuevos) {
        this.intereses.clear();
        this.intereses.addAll(nuevos);
    }
}
