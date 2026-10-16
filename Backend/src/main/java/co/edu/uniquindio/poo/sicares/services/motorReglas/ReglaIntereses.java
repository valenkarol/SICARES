package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.InteresProfesional;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

/** Compara el área de conocimiento del programa con los intereses profesionales del usuario. */
@Component
public class ReglaIntereses implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        String area = normalizar(op.getPrograma().getAreaConocimiento());

        for (InteresProfesional i : perfil.getIntereses()) {
            String interes = normalizar(i.getAreaConocimiento());
            if (area.equals(interes) || area.contains(interes) || interes.contains(area)) {
                return ResultadoEvaluacion.cumple(1.0, "Coincide con tu interés en " + i.getAreaConocimiento());
            }
        }
        if (area.contains("formacion general")) {
            // Primaria, secundaria y media son base para cualquier interés
            return ResultadoEvaluacion.cumple(0.8, "Formación básica necesaria para avanzar hacia tus intereses");
        }
        return ResultadoEvaluacion.cumple(0.3);
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
        return false;
    }
}
