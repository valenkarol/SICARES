package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.*;
import co.edu.uniquindio.poo.sicares.dto.OfertaMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.repository.ProgramaEducativoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consultar programas y Gestionar programas. */
@Service
@RequiredArgsConstructor
public class ProgramaService {

    private final ProgramaEducativoRepository programaRepository;
    private final InstitucionService institucionService;
    private final RequisitoService requisitoService;

    /**  listado con filtros. Los beneficiarios solo ven programas activos. */
    @Transactional(readOnly = true)
    public List<ProgramaResponse> buscar(boolean soloActivos, NivelEducativo nivel, Modalidad modalidad,
                                         String municipio, String area, String texto) {
        return programaRepository.buscar(soloActivos, nivel, modalidad, vacioANull(municipio),
                        vacioANull(area), vacioANull(texto))
                .stream().map(OfertaMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProgramaResponse obtener(Long id) {
        return OfertaMapper.toDto(buscarEntidad(id));
    }

    @Transactional
    public ProgramaResponse crear(ProgramaRequest req) {
        Institucion institucion = institucionService.buscar(req.institucionId());
        ProgramaEducativo p = new ProgramaEducativo();
        copiar(req, p);
        institucion.agregarPrograma(p); // relación institución-programa
        return OfertaMapper.toDto(programaRepository.save(p));
    }

    @Transactional
    public ProgramaResponse actualizar(Long id, ProgramaRequest req) {
        ProgramaEducativo p = buscarEntidad(id);
        if (!p.getInstitucion().getId().equals(req.institucionId())) {
            p.getInstitucion().getProgramas().remove(p);
            institucionService.buscar(req.institucionId()).agregarPrograma(p);
        }
        copiar(req, p);
        return OfertaMapper.toDto(p);
    }

    /**
     * Elimina el programa y sus convocatorias. Si alguna convocatoria ya está en la ruta educativa o en una
     * postulación de un usuario, la base de datos no permite borrarla: se informa con un mensaje claro y se
     * recomienda desactivar el programa (así deja de ofrecerse sin perder el historial).
     */
    @Transactional
    public void eliminar(Long id) {
        ProgramaEducativo p = buscarEntidad(id);
        p.getBeneficios().forEach(b -> b.getProgramas().remove(p));
        p.getInstitucion().getProgramas().remove(p);
        try {
            programaRepository.delete(p);
            programaRepository.flush(); // fuerza el borrado aquí para poder capturar el error
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("No se puede eliminar el programa porque sus convocatorias ya aparecen en "
                    + "rutas educativas o postulaciones de usuarios. Desactívalo (Editar → desmarcar «Activo») "
                    + "para que deje de ofrecerse.");
        }
    }

    public ProgramaEducativo buscarEntidad(Long id) {
        return programaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa", id));
    }

    private void copiar(ProgramaRequest req, ProgramaEducativo p) {
        p.setNombre(req.nombre().trim());
        p.setNivel(req.nivel());
        p.setModalidad(req.modalidad());
        p.setAreaConocimiento(req.areaConocimiento());
        p.setDuracionPeriodos(req.duracionPeriodos());
        p.setCostoPeriodo(req.costoPeriodo());
        p.setMunicipio(req.municipio().trim());
        p.setActivo(req.activo());
        p.setRequisitos(requisitoService.buscarVarios(req.requisitoIds()));
    }

    private static String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

