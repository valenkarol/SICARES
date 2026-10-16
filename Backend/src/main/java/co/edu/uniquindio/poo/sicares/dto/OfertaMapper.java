package co.edu.uniquindio.poo.sicares.dto;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.*;
import co.edu.uniquindio.poo.sicares.models.entities.*;

import java.util.Comparator;

public final class OfertaMapper {

    private OfertaMapper() {
    }

    public static InstitucionDto toDto(Institucion i) {
        return new InstitucionDto(i.getId(), i.getNombre(), i.getNit(), i.getTipo(), i.getDepartamento(),
                i.getMunicipio(), i.getCorreoContacto(), i.getProgramas().size());
    }

    public static RequisitoDto toDto(Requisito r) {
        return new RequisitoDto(r.getId(), r.getNombre(), r.getDescripcion(), r.getTipo(), r.isObligatorio(),
                r.getValorEsperado(), r.isRequiereDocumento());
    }

    public static BeneficioResumen toResumen(Beneficio b) {
        return new BeneficioResumen(b.getId(), b.getNombre(), b.getTipo(), b.getPorcentajeCobertura(),
                b.isExclusivoVictimas(), b.getFechaLimite());
    }

    public static BeneficioResponse toDto(Beneficio b) {
        return new BeneficioResponse(b.getId(), b.getNombre(), b.getTipo(), b.getDescripcion(),
                b.getPorcentajeCobertura(), b.isExclusivoVictimas(), b.getFechaLimite(), b.estaVigente(),
                b.getInstitucion().getId(), b.getInstitucion().getNombre(),
                b.getProgramas().stream().map(ProgramaEducativo::getId).sorted().toList(),
                b.getRequisitos().stream().map(OfertaMapper::toDto).toList());
    }

    public static ProgramaResponse toDto(ProgramaEducativo p) {
        Institucion i = p.getInstitucion();
        return new ProgramaResponse(p.getId(), p.getNombre(), p.getNivel(), p.getModalidad(),
                p.getAreaConocimiento(), p.getDuracionPeriodos(), p.getCostoPeriodo(), p.getMunicipio(),
                p.isActivo(), p.esGratuito(), i.getId(), i.getNombre(), i.getTipo(),
                p.getRequisitos().stream().sorted(Comparator.comparing(Requisito::getId)).map(OfertaMapper::toDto).toList(),
                p.getBeneficios().stream().filter(Beneficio::estaVigente).map(OfertaMapper::toResumen).toList());
    }

    public static OportunidadResponse toDto(OportunidadEducativa o) {
        ProgramaEducativo p = o.getPrograma();
        return new OportunidadResponse(o.getId(), o.getPeriodoAcademico(), o.getFechaAperturaInscripcion(),
                o.getFechaCierreInscripcion(), o.getFechaAdmision(), o.getFechaMatricula(), o.getCupos(),
                o.estaVigente(), o.diasParaCierre(), p.getId(), p.getNombre(), p.getInstitucion().getNombre(),
                p.getNivel(), p.getModalidad(), p.getMunicipio());
    }
}