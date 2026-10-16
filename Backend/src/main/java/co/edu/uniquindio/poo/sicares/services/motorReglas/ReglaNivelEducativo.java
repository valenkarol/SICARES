package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.PerfilEducativo;
import co.edu.uniquindio.poo.sicares.models.entities.Requisito;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoEstudio;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import org.springframework.stereotype.Component;

/**
 * Valida que el programa represente una continuidad para el usuario:
 *  - el usuario debe tener aprobado el nivel previo requerido (y los requisitos NIVEL_EDUCATIVO),
 *  - el programa debe ser de un nivel superior al que ya tiene.
 * Da puntaje máximo al "siguiente paso" natural.
 */
@Component
public class ReglaNivelEducativo implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        NivelEducativo actual = perfil.getTrayectoriaEducativa().getNivelActual();
        NivelEducativo nivelPrograma = op.getPrograma().getNivel();

        if (!nivelPrograma.esMayorOIgualQue(NivelEducativo.BASICA_PRIMARIA)
                || nivelPrograma.ordinal() <= actual.ordinal()) {
            return ResultadoEvaluacion.noCumple("Ya alcanzaste el nivel " + nivelPrograma);
        }

        boolean yaLoCursa = perfil.getTrayectoriaEducativa().getEstudios().stream()
                .anyMatch(e -> e.getEstado() == EstadoEstudio.EN_CURSO && e.getNivel() == nivelPrograma);
        if (yaLoCursa) {
            return ResultadoEvaluacion.noCumple("Ya estás cursando un programa de nivel " + nivelPrograma);
        }

        NivelEducativo previo = nivelPrograma.nivelPrevioRequerido();
        if (!actual.esMayorOIgualQue(previo)) {
            return ResultadoEvaluacion.noCumple("Para " + nivelPrograma + " necesitas haber terminado " + previo);
        }

        for (Requisito r : op.getPrograma().getRequisitos()) {
            if (r.getTipo() == TipoRequisito.NIVEL_EDUCATIVO && r.getValorEsperado() != null) {
                NivelEducativo exigido = NivelEducativo.valueOf(r.getValorEsperado());
                if (!actual.esMayorOIgualQue(exigido)) {
                    return ResultadoEvaluacion.noCumple("El programa exige nivel " + exigido);
                }
            }
        }

        // ¿Es el siguiente paso inmediato? (ej. de MEDIA a TECNICO/TECNOLOGICO/UNIVERSITARIO)
        boolean siguientePaso = previo == actual || nivelPrograma.ordinal() == actual.ordinal() + 1;
        if (siguientePaso) {
            return ResultadoEvaluacion.cumple(1.0, "Es el siguiente paso en tu trayectoria (" + nivelPrograma + ")");
        }
        return ResultadoEvaluacion.cumple(0.7, "Cumples el nivel educativo requerido");
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
