package co.edu.uniquindio.poo.sicares.models.entities;

import co.edu.uniquindio.poo.sicares.models.enums.EstadoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/** DocumentoPostulacion soporta Requisito */
@Entity
@Table(name = "documentos_postulacion")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoPostulacion {

    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_archivo", nullable = false, length = 200)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 60)
    private String tipoArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 300)
    private String rutaAlmacenamiento;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoDocumento estado = EstadoDocumento.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "postulacion_id", nullable = false)
    private Postulacion postulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisito_id")
    private Requisito requisito;

    /**
     * detecta el tipo REAL del archivo por sus primeros bytes
     */
    public static String detectarTipoReal(byte[] b) {
        if (b == null) return null;
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return "application/pdf";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    /** Solo PDF, JPG o PNG de máximo 5 MB. */
    public boolean validarFormato() {
        return tipoArchivo != null
                && TIPOS_PERMITIDOS.contains(tipoArchivo.toLowerCase())
                && tamanoBytes > 0
                && tamanoBytes <= TAMANO_MAXIMO_BYTES;
    }
}
