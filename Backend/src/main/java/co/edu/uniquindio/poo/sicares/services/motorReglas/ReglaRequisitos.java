package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compara el perfil con los requisitos del programa de tipo CONDICION_VICTIMA y SISBEN.
 * Los requisitos de DOCUMENTO/OTRO no descartan: se informan como observación.
 * (EDAD y NIVEL_EDUCATIVO los evalúan sus reglas específicas.)
 */
@Component
public class ReglaRequisitos implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        List<String> obs = new ArrayList<>();
        int evaluados = 0;
        int cumplidos = 0;

        for (Requisito r : op.getPrograma().getRequisitos()) {
            TipoRequisito tipo = r.getTipo();
            if (tipo == TipoRequisito.CONDICION_VICTIMA) {
                evaluados++;
                SituacionVictima v = perfil.getSituacionVictima();
                boolean ok = v != null && v.isEsVictima() && v.isInscritoRUV();
                if (ok) {
                    cumplidos++;
                } else if (r.isObligatorio()) {
                    return ResultadoEvaluacion.noCumple("Requiere estar inscrito en el Registro Único de Víctimas");
                }
            } else if (tipo == TipoRequisito.SISBEN) {
                evaluados++;
                if (cumpleSisben(perfil.getSituacionSocioeconomica(), r.getValorEsperado())) {
                    cumplidos++;
                } else if (r.isObligatorio()) {
                    return ResultadoEvaluacion.noCumple("Requiere SISBÉN en los grupos " + r.getValorEsperado());
                }
            } else if (tipo == TipoRequisito.DOCUMENTO || tipo == TipoRequisito.OTRO) {
                obs.add("Deberás presentar: " + r.getNombre());
            }
        }
        double puntaje = evaluados == 0 ? 1.0 : (double) cumplidos / evaluados;
        return new ResultadoEvaluacion(true, puntaje, obs);
    }

    private boolean cumpleSisben(SituacionSocioeconomica s, String gruposPermitidos) {
        if (s == null || s.getGrupoSisben() == null || gruposPermitidos == null) return false;
        String grupo = s.getGrupoSisben().trim().toUpperCase();
        if (grupo.isEmpty() || grupo.startsWith("NO")) return false;
        String letra = grupo.substring(0, 1);
        return Arrays.stream(gruposPermitidos.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(letra::equals);
    }

    @Override
    public double getPeso() {
        return 2;
    }

    @Override
    public boolean esEliminatoria() {
        return true;
    }
}
