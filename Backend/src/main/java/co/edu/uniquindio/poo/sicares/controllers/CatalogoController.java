package co.edu.uniquindio.poo.sicares.controllers;

import co.edu.uniquindio.poo.sicares.config.Catalogos;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Listas para llenar los selects del frontend (público). */
@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    @GetMapping
    public Map<String, List<?>> catalogos() {
        Map<String, List<?>> c = new LinkedHashMap<>();
        c.put("areasConocimiento", Catalogos.AREAS_CONOCIMIENTO);
        c.put("hechosVictimizantes", Catalogos.HECHOS_VICTIMIZANTES);
        c.put("gruposSisben", Catalogos.GRUPOS_SISBEN);
        c.put("mediosTransporte", Catalogos.MEDIOS_TRANSPORTE);
        c.put("situacionesLaborales", Catalogos.SITUACIONES_LABORALES);
        c.put("nivelesEducativos", List.of(NivelEducativo.values()));
        c.put("modalidades", List.of(Modalidad.values()));
        c.put("tiposDocumento", List.of(TipoDocumento.values()));
        c.put("zonas", List.of(Zona.values()));
        c.put("estadosEstudio", List.of(EstadoEstudio.values()));
        c.put("tiposInstitucion", List.of(TipoInstitucion.values()));
        c.put("tiposBeneficio", List.of(TipoBeneficio.values()));
        c.put("tiposRequisito", List.of(TipoRequisito.values()));
        c.put("estadosPostulacion", List.of(EstadoPostulacion.values()));
        c.put("estadosDocumento", List.of(EstadoDocumento.values()));
        return c;
    }
}
