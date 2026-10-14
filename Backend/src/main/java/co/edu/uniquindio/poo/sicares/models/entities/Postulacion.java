package co.edu.uniquindio.poo.sicares.models.entities;

import com.sicares.exceptions.ReglaNegocioException;
import com.sicares.models.enums.EstadoDocumento;
import com.sicares.models.enums.EstadoPostulacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Postulacion (paquete "Postulación y seguimiento")
 *  - Beneficiario 1 "Realiza" 0..* Postulacion
 *  - 0..* "a" 1 OportunidadEducativa
 *  - 0..* "Se origina en" 0..1 Recomendacion
 *  - 1 ◆── 0..* DocumentoPostulacion, 1..* HistorialEstado, 0..* Recordatorio, 0..* Notificacion
 */
@Entity
@Table(name = "postulaciones")
@Getter
@Setter
@NoArgsConstructor
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_seleccion", nullable = false)
    private LocalDateTime fechaSeleccion = LocalDateTime.now();

    @Column(name = "fecha_postulacion")
    private LocalDateTime fechaPostulacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoPostulacion estado = EstadoPostulacion.SELECCIONADA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Beneficiario beneficiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oportunidad_id", nullable = false)
    private OportunidadEducativa oportunidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recomendacion_id")
    private Recomendacion recomendacion;

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoPostulacion> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCambio ASC")
    private List<HistorialEstado> historial = new ArrayList<>();

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaEvento ASC")
    private List<Recordatorio> recordatorios = new ArrayList<>();

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notificacion> notificaciones = new ArrayList<>();

    /** Primer registro del historial: la oportunidad fue seleccionada (HistorialEstado 1..*). */
    public void iniciar(Usuario responsable) {
        HistorialEstado h = new HistorialEstado();
        h.setPostulacion(this);
        h.setEstadoAnterior(null);
        h.setEstadoNuevo(EstadoPostulacion.SELECCIONADA);
        h.setObservacion("Oportunidad seleccionada desde la ruta educativa");
        h.setResponsable(responsable);
        historial.add(h);
    }

    /**
     * Registra formalmente la postulación (HU-23).
     * Si faltan documentos obligatorios la deja en DOCUMENTOS_PENDIENTES y devuelve false.
     * Si está todo completo pasa a POSTULADA y devuelve true.
     */
    public boolean registrar(Usuario responsable) {
        if (estado != EstadoPostulacion.SELECCIONADA && estado != EstadoPostulacion.DOCUMENTOS_PENDIENTES) {
            throw new ReglaNegocioException("La postulación ya fue registrada (estado actual: " + estado + ")");
        }
        if (!documentosPendientes().isEmpty()) {
            if (estado != EstadoPostulacion.DOCUMENTOS_PENDIENTES) {
                cambiarEstado(EstadoPostulacion.DOCUMENTOS_PENDIENTES, responsable,
                        "Faltan documentos obligatorios por cargar");
            }
            return false;
        }
        cambiarEstado(EstadoPostulacion.POSTULADA, responsable, "Postulación registrada por el beneficiario");
        this.fechaPostulacion = LocalDateTime.now();
        return true;
    }

    public void cambiarEstado(EstadoPostulacion nuevo, Usuario responsable, String obs) {
        if (!estado.puedeCambiarA(nuevo)) {
            throw new ReglaNegocioException(
                    "No se puede pasar de " + estado + " a " + nuevo + ". Permitidos: " + estado.siguientesPermitidos());
        }
        HistorialEstado h = new HistorialEstado();
        h.setPostulacion(this);
        h.setEstadoAnterior(estado);
        h.setEstadoNuevo(nuevo);
        h.setObservacion(obs);
        h.setResponsable(responsable);
        historial.add(h);
        this.estado = nuevo;
    }

    /** Requisitos del programa que exigen documento y aún no tienen uno CARGADO o VALIDADO. */
    public List<Requisito> documentosPendientes() {
        Set<Long> cubiertos = documentos.stream()
                .filter(d -> d.getRequisito() != null)
                .filter(d -> d.getEstado() == EstadoDocumento.CARGADO || d.getEstado() == EstadoDocumento.VALIDADO)
                .map(d -> d.getRequisito().getId())
                .collect(Collectors.toSet());
        return oportunidad.getPrograma().getRequisitos().stream()
                .filter(Requisito::isRequiereDocumento)
                .filter(Requisito::isObligatorio)
                .filter(r -> !cubiertos.contains(r.getId()))
                .toList();
    }

    public boolean estaFinalizada() {
        return estado.esFinal();
    }

    public void agregarDocumento(DocumentoPostulacion d) {
        d.setPostulacion(this);
        documentos.add(d);
    }

    public void agregarRecordatorio(Recordatorio r) {
        r.setPostulacion(this);
        recordatorios.add(r);
    }
}