package co.edu.uniquindio.poo.sicares.services.motorReglas;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Considera internet, dispositivos, transporte y ubicación frente a la modalidad del programa.
 * VIRTUAL sin internet o sin dispositivo => no es viable (eliminatoria).
 */
@Component
public class ReglaAccesibilidad implements ReglaCompatibilidad {

    @Override
    public ResultadoEvaluacion evaluar(PerfilEducativo perfil, OportunidadEducativa op) {
        CondicionAccesibilidad c = perfil.getCondicionAccesibilidad();
        InformacionPersonal info = perfil.getInformacionPersonal();
        ProgramaEducativo p = op.getPrograma();
        List<String> obs = new ArrayList<>();
        double puntaje;

        switch (p.getModalidad()) {
            case VIRTUAL -> {
                if (!c.isTieneInternet() || !c.isTieneDispositivos()) {
                    return ResultadoEvaluacion.noCumple("Es virtual y no cuentas con internet o dispositivo");
                }
                puntaje = 1.0;
                obs.add("Puedes estudiarlo desde casa");
            }
            case DISTANCIA -> {
                puntaje = c.isTieneDispositivos() ? 0.9 : 0.6;
                if (!c.isTieneInternet()) obs.add("Necesitarás acceso a internet en algunos momentos");
            }
            case HIBRIDA -> {
                puntaje = c.isTieneInternet() ? 0.8 : 0.4;
                puntaje *= factorUbicacion(info, p, c, obs);
            }
            default -> puntaje = factorUbicacion(info, p, c, obs); // PRESENCIAL
        }

        if (puntaje == 0) {
            return ResultadoEvaluacion.noCumple(obs.toArray(String[]::new));
        }
        if (c.getModalidadPreferida() != null && c.getModalidadPreferida() == p.getModalidad()) {
            puntaje = Math.min(1.0, puntaje + 0.15);
            obs.add("Coincide con tu modalidad preferida");
        }
        return new ResultadoEvaluacion(true, puntaje, obs);
    }

    /** 1.0 mismo municipio; 0.5 mismo departamento con transporte; 0 si no hay forma de llegar. */
    private double factorUbicacion(InformacionPersonal info, ProgramaEducativo p, CondicionAccesibilidad c,
                                   List<String> obs) {
        if (p.getMunicipio().equalsIgnoreCase(info.getMunicipio())) {
            obs.add("Se ofrece en tu municipio");
            return 1.0;
        }
        boolean sinTransporte = c.getMedioTransporte() == null
                || "NINGUNO".equalsIgnoreCase(c.getMedioTransporte());
        boolean mismoDepartamento = p.getInstitucion().getDepartamento().equalsIgnoreCase(info.getDepartamento());
        if (mismoDepartamento && !sinTransporte && c.getDistanciaMaximaKm() > 0) {
            obs.add("Se ofrece en " + p.getMunicipio() + "; requiere desplazamiento");
            return 0.5;
        }
        obs.add("Se ofrece en " + p.getMunicipio() + " y tus condiciones de transporte no permiten llegar");
        return p.getModalidad() == Modalidad.PRESENCIAL ? 0 : 0.3;
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
