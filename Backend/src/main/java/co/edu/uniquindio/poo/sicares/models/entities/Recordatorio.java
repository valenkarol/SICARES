package co.edu.uniquindio.poo.sicares.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recordatorios")
@Getter
@Setter
@NoArgsConstructor
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    /** Fecha del evento que se recuerda (cierre de inscripción, admisión, matrícula). */
    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    /** Cuándo debe enviarse el recordatorio. */
    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(nullable = false)
    private boolean enviado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "postulacion_id", nullable = false)
    private Postulacion postulacion;

    public boolean debeEnviarse(LocalDate hoy) {
        return !enviado
                && !fechaProgramada.toLocalDate().isAfter(hoy)
                && !fechaEvento.isBefore(hoy);
    }
}
