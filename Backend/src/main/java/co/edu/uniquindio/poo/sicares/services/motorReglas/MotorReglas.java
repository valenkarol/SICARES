package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;
import co.edu.uniquindio.poo.sicares.models.entities.Recomendacion;
import co.edu.uniquindio.poo.sicares.models.entities.RutaEducativa;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <<service>> MotorReglas (HU-17 Analizar compatibilidad).
 * Contexto del patrón Strategy: aplica cada ReglaCompatibilidad y combina los puntajes
 * en un promedio ponderado (0 a 100).
 */
@Service
public class MotorReglas {

    /** Máximo de recomendaciones por ruta. */
    public static final int MAX_RECOMENDACIONES = 10;
    /** Puntaje mínimo (0-100) para que una oportunidad entre en la ruta. */
    public static final double PUNTAJE_MINIMO = 40;

    private final List<ReglaCompatibilidad> reglas;
    private final OportunidadEducativaRepository oportunidadRepository;

    /** Spring inyecta automáticamente todas las clases @Component que implementan ReglaCompatibilidad. */
    public MotorReglas(List<ReglaCompatibilidad> reglas, OportunidadEducativaRepository oportunidadRepository) {
        this.reglas = new ArrayList<>(reglas);
        this.oportunidadRepository = oportunidadRepository;
    }

    public void registrarRegla(ReglaCompatibilidad r) {
        reglas.add(r);
    }

    /** evaluar(perfil, oportunidad): aplica todas las reglas. */
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa oportunidad) {
        double sumaPesos = 0;
        double sumaPuntajes = 0;
        Set<String> observaciones = new LinkedHashSet<>();

        for (ReglaCompatibilidad regla : reglas) {
            ResultadoEvaluacion r = regla.evaluar(perfil, oportunidad);
            if (!r.isCumple() && regla.esEliminatoria()) {
                return new ResultadoEvaluacion(false, 0, r.getObservaciones());
            }
            sumaPesos += regla.getPeso();
            sumaPuntajes += regla.getPeso() * (r.isCumple() ? r.getPuntaje() : 0);
            observaciones.addAll(r.getObservaciones());
        }
        double puntaje = sumaPesos == 0 ? 0 : Math.round(sumaPuntajes / sumaPesos * 1000) / 10.0;
        return new ResultadoEvaluacion(true, puntaje, new ArrayList<>(observaciones));
    }

    /**
     * generarRuta(perfil): evalúa todas las oportunidades vigentes y arma la ruta con las
     * compatibles, ordenadas por puntaje. La ruta se devuelve sin guardar (lo hace RutaEducativaService).
     */
    public RutaEducativa generarRuta(PerfilEducativo perfil) {
        if (perfil == null || !perfil.isCompleto()) {
            throw new ReglaNegocioException("Completa tu caracterización (100%) para generar una ruta educativa");
        }
        RutaEducativa ruta = new RutaEducativa();

        for (OportunidadEducativa op : oportunidadRepository.findVigentes(LocalDate.now())) {
            ResultadoEvaluacion resultado = evaluar(perfil, op);
            if (resultado.isCumple() && resultado.getPuntaje() >= PUNTAJE_MINIMO) {
                Recomendacion rec = new Recomendacion();
                rec.setOportunidad(op);
                rec.setPuntajeCompatibilidad(resultado.getPuntaje());
                rec.setJustificacion(recortar(String.join(". ", resultado.getObservaciones()), 2000));
                ruta.agregarRecomendacion(rec);
            }
        }
        ruta.ordenarPorCompatibilidad();
        // conservar solo las mejores
        while (ruta.getRecomendaciones().size() > MAX_RECOMENDACIONES) {
            ruta.getRecomendaciones().remove(ruta.getRecomendaciones().size() - 1);
        }
        return ruta;
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
