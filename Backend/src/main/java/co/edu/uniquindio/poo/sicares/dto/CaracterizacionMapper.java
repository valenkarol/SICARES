package co.edu.uniquindio.poo.sicares.dto;


import co.edu.uniquindio.poo.sicares.models.entities.*;

import java.util.List;

/**
 * Proceso 1 – Caracterización (HU-04 a HU-11).
 * Conversión Entidad -> DTO. Debe llamarse dentro de una transacción (@Transactional en el service)
 * porque accede a relaciones LAZY. Los services nunca devuelven entidades al frontend.
 */
public final class CaracterizacionMapper {

    private CaracterizacionMapper() {
    }

    public static CaracterizacionDtos.InformacionPersonalDto toDto(InformacionPersonal i) {
        if (i == null) return null;
        return new CaracterizacionDtos.InformacionPersonalDto(i.getNombres(), i.getApellidos(), i.getTipoDocumento(),
                i.getNumeroDocumento(), i.getFechaNacimiento(), i.getGenero(), i.getEtnia(), i.getTelefono(),
                i.getDepartamento(), i.getMunicipio(), i.getZona(), i.calcularEdad(), toDto(i.getResponsable()));
    }

    public static CaracterizacionDtos.ResponsableMenorDto toDto(ResponsableMenor r) {
        if (r == null) return null;
        return new CaracterizacionDtos.ResponsableMenorDto(r.getNombres(), r.getApellidos(), r.getTipoDocumento(),
                r.getNumeroDocumento(), r.getFechaNacimiento(), r.getParentesco(), r.getTelefono(), r.getCorreo());
    }

    public static CaracterizacionDtos.SituacionVictimaDto toDto(SituacionVictima v) {
        if (v == null) return null;
        return new CaracterizacionDtos.SituacionVictimaDto(v.isEsVictima(), v.isInscritoRUV(), v.getHechoVictimizante(),
                v.getFechaHecho(), v.getMunicipioExpulsor());
    }

    public static CaracterizacionDtos.SituacionSocioeconomicaDto toDto(SituacionSocioeconomica s) {
        if (s == null) return null;
        return new CaracterizacionDtos.SituacionSocioeconomicaDto(s.getGrupoSisben(), s.getEstrato(), s.getIngresoMensualHogar(),
                s.getSituacionLaboral(), s.getIntegrantesHogar(), s.getPersonasACargo());
    }

    public static CaracterizacionDtos.CondicionAccesibilidadDto toDto(CondicionAccesibilidad c) {
        if (c == null) return null;
        return new CaracterizacionDtos.CondicionAccesibilidadDto(c.isTieneInternet(), c.isTieneDispositivos(), c.getMedioTransporte(),
                c.getDistanciaMaximaKm(), c.getModalidadPreferida());
    }

    public static CaracterizacionDtos.TrayectoriaEducativaDto toDto(TrayectoriaEducativa t) {
        if (t == null) return null;
        List<CaracterizacionDtos.EstudioRealizadoDto> estudios = t.getEstudios().stream()
                .map(e -> new CaracterizacionDtos.EstudioRealizadoDto(e.getId(), e.getNivel(), e.getInstitucion(), e.getTitulo(),
                        e.getEstado(), e.getAnioInicio(), e.getAnioFin()))
                .toList();
        return new CaracterizacionDtos.TrayectoriaEducativaDto(t.getNivelActual(), t.getUltimoGradoAprobado(),
                t.getAnioUltimoEstudio(), estudios);
    }

    public static CaracterizacionDtos.PerfilResponse toPerfil(Beneficiario b) {
        PerfilEducativo p = b.getPerfil();
        boolean autorizada = b.getAutorizacion() != null && b.getAutorizacion().isAceptada();
        if (p == null) {
            return new CaracterizacionDtos.PerfilResponse(null, b.getCorreo(), false, 0, null, autorizada,
                    null, null, null, null, null, List.of());
        }
        List<CaracterizacionDtos.InteresProfesionalDto> intereses = p.getIntereses().stream()
                .map(i -> new CaracterizacionDtos.InteresProfesionalDto(i.getAreaConocimiento(), i.getDescripcion()))
                .toList();
        return new CaracterizacionDtos.PerfilResponse(p.getId(), b.getCorreo(), p.isCompleto(), p.getPorcentajeCompletitud(),
                p.getFechaActualizacion(), autorizada,
                toDto(p.getInformacionPersonal()), toDto(p.getSituacionVictima()),
                toDto(p.getSituacionSocioeconomica()), toDto(p.getCondicionAccesibilidad()),
                toDto(p.getTrayectoriaEducativa()), intereses);
    }
}
