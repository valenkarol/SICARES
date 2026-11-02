package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.Beneficio;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.entities.Requisito;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;

/**
 * Integridad de la información en la generación de rutas.
 */
public final class HuellaIntegridad {

    private static final Comparator<Long> IDS = Comparator.nullsFirst(Comparator.naturalOrder());

    private HuellaIntegridad() {
    }

    public static String deOportunidad(OportunidadEducativa o) {
        ProgramaEducativo p = o.getPrograma();
        Institucion inst = p.getInstitucion();
        StringBuilder sb = new StringBuilder()
                .append("O").append(o.getId()).append('|').append(o.getPeriodoAcademico()).append('|')
                .append(o.getFechaAperturaInscripcion()).append('|').append(o.getFechaCierreInscripcion()).append('|')
                .append(o.getFechaAdmision()).append('|').append(o.getFechaMatricula())
                .append("|P").append(p.getId()).append('|').append(p.getNombre()).append('|').append(p.getNivel())
                .append('|').append(p.getModalidad()).append('|').append(p.getAreaConocimiento())
                .append('|').append(texto(p.getCostoPeriodo())).append('|').append(p.getMunicipio())
                .append('|').append(p.isActivo())
                .append("|I").append(inst == null ? null : inst.getId())
                .append('|').append(inst == null ? null : inst.getDepartamento());
        p.getRequisitos().stream()
                .sorted(Comparator.comparing(Requisito::getId, IDS))
                .forEach(r -> sb.append("|R").append(r.getId()).append(',').append(r.getTipo())
                        .append(',').append(r.getValorEsperado()).append(',').append(r.isObligatorio())
                        .append(',').append(r.isRequiereDocumento()));
        p.getBeneficios().stream()
                .sorted(Comparator.comparing(Beneficio::getId, IDS))
                .forEach(b -> sb.append("|B").append(b.getId()).append(',').append(b.getPorcentajeCobertura())
                        .append(',').append(b.isExclusivoVictimas()).append(',').append(b.getFechaLimite()));
        return sha256(sb.toString());
    }

    /** true si la huella guardada ya no corresponde a los datos actuales de la oportunidad. */
    public static boolean fueAlterada(String huellaGuardada, OportunidadEducativa actual) {
        return huellaGuardada != null && !huellaGuardada.equals(deOportunidad(actual));
    }

    /** Evita falsos positivos por escala: 1000, 1000.0 y 1000.00 dan el mismo texto. */
    private static String texto(BigDecimal valor) {
        return valor == null ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
