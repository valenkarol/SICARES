package co.edu.uniquindio.poo.sicares.models.entities;


import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ver TipoRequisito para el formato de valorEsperado. */
@Entity
@Table(name = "requisitos")
@Getter
@Setter
@NoArgsConstructor
public class Requisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRequisito tipo;

    @Column(nullable = false)
    private boolean obligatorio = true;

    @Column(name = "valor_esperado", length = 100)
    private String valorEsperado;

    @Column(name = "requiere_documento", nullable = false)
    private boolean requiereDocumento;
}
