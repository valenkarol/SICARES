package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.InteresProfesional;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class ReglaIntereses implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        String area = normalizar(op.getPrograma().getAreaConocimiento());
        if (area.isEmpty()) {
            return ResultadoEvaluacion.noCumple("El programa no tiene área de conocimiento registrada");
        }
        for (InteresProfesional i : perfil.getIntereses()) {
            String interes = normalizar(i.getAreaConocimiento());
            if (interes.isEmpty()) {
                continue;   // un interés vacío no debe coincidir con todo ("" está contenido en cualquier texto)
            }
            if (area.equals(interes) || area.contains(interes) || interes.contains(area)) {
                return ResultadoEvaluacion.cumple(1.0, "Coincide con tu interés en " + i.getAreaConocimiento());
            }
        }
        return ResultadoEvaluacion.noCumple("El área " + op.getPrograma().getAreaConocimiento()
                + " no está entre tus intereses");
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    @Override
    public double getPeso() {
        return 3;
    }

    @Override
    public boolean esEliminatoria() {
        return true;
    }
}