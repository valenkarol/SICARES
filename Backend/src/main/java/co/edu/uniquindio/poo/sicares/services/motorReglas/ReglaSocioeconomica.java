package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Evalúa si el costo es asumible: programas gratuitos puntúan alto; si no, se consideran los
 * beneficios vigentes (becas, créditos, subsidios) y el ingreso del hogar. No es eliminatoria:
 * un programa costoso se recomienda con menor prioridad.
 */
@Component
public class ReglaSocioeconomica implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        ProgramaEducativo p = op.getPrograma();
        List<String> obs = new ArrayList<>();

        if (p.esGratuito()) {
            obs.add("Programa gratuito");
            return new ResultadoEvaluacion(true, 1.0, obs);
        }

        boolean esVictima = perfil.getSituacionVictima() != null && perfil.getSituacionVictima().isEsVictima();
        double mejorCobertura = p.getBeneficios().stream()
                .filter(Beneficio::estaVigente)
                .filter(b -> !b.isExclusivoVictimas() || esVictima)
                .mapToDouble(Beneficio::getPorcentajeCobertura)
                .max().orElse(0);

        BigDecimal costoReal = p.getCostoPeriodo()
                .multiply(BigDecimal.valueOf(100 - mejorCobertura))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (mejorCobertura > 0) {
            obs.add("Tiene beneficios que cubren hasta el " + Math.round(mejorCobertura) + "% del costo");
        }

        SituacionSocioeconomica s = perfil.getSituacionSocioeconomica();
        BigDecimal ingreso = s.getIngresoMensualHogar();
        double puntaje;
        if (costoReal.compareTo(BigDecimal.ZERO) == 0) {
            puntaje = 0.95;
        } else if (ingreso == null || ingreso.compareTo(BigDecimal.ZERO) == 0) {
            puntaje = 0.2;
            obs.add("El costo por periodo puede ser difícil de asumir sin ingresos");
        } else {
            // proporción del costo del periodo frente a 6 meses de ingreso del hogar
            double ratio = costoReal.doubleValue() / (ingreso.doubleValue() * 6);
            puntaje = Math.max(0.1, 1 - ratio * 2);
            if (ratio > 0.3) obs.add("El costo representa una carga alta para el hogar");
        }
        if (s.getEstrato() <= 2 && mejorCobertura == 0) {
            puntaje *= 0.8;
        }
        return new ResultadoEvaluacion(true, puntaje, obs);
    }

    @Override
    public double getPeso() {
        return 2;
    }

    @Override
    public boolean esEliminatoria() {
        return false;
    }
}