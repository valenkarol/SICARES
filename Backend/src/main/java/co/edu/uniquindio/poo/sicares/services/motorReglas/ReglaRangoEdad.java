package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;
import co.edu.uniquindio.poo.sicares.models.entities.Requisito;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import org.springframework.stereotype.Component;

/** Verifica la edad del usuario contra los requisitos de tipo EDAD del programa (ej. "15-28"). */
@Component
public class ReglaRangoEdad implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        int edad = perfil.getInformacionPersonal().calcularEdad();
        for (Requisito r : op.getPrograma().getRequisitos()) {
            if (r.getTipo() != TipoRequisito.EDAD || r.getValorEsperado() == null) continue;
            String[] rango = r.getValorEsperado().split("-");
            int min = Integer.parseInt(rango[0].trim());
            int max = Integer.parseInt(rango[1].trim());
            if (edad < min || edad > max) {
                return ResultadoEvaluacion.noCumple(
                        "El programa es para personas entre " + min + " y " + max + " años (tienes " + edad + ")");
            }
            return ResultadoEvaluacion.cumple(1.0, "Cumples el rango de edad del programa");
        }
        return ResultadoEvaluacion.cumple(1.0);
    }

    @Override
    public double getPeso() {
        return 1;
    }

    @Override
    public boolean esEliminatoria() {
        return true;
    }
}
