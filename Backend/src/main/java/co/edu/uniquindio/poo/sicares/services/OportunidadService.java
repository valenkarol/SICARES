package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.*;
import co.edu.uniquindio.poo.sicares.dto.OfertaMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Convocatorias periodos, fechas y cupos de cada programa educativo. */
@Service
@RequiredArgsConstructor
public class OportunidadService {

    private final OportunidadEducativaRepository oportunidadRepository;
    private final ProgramaService programaService;

    @Transactional(readOnly = true)
    public List<OportunidadResponse> listar(boolean soloVigentes, Long programaId) {
        List<OportunidadEducativa> lista;
        if (programaId != null) {
            lista = oportunidadRepository.findByProgramaId(programaId);
        } else if (soloVigentes) {
            lista = oportunidadRepository.findVigentes(LocalDate.now());
        } else {
            lista = oportunidadRepository.findAll();
        }
        return lista.stream()
                .filter(o -> !soloVigentes || o.estaVigente())
                .sorted(Comparator.comparing(OportunidadEducativa::getFechaCierreInscripcion))
                .map(OfertaMapper::toDto)
                .toList();
    }

    @Transactional
    public OportunidadResponse crear(OportunidadRequest req) {
        validarFechas(req);
        OportunidadEducativa o = new OportunidadEducativa();
        copiar(req, o);
        return OfertaMapper.toDto(oportunidadRepository.save(o));
    }

    @Transactional
    public OportunidadResponse actualizar(Long id, OportunidadRequest req) {
        validarFechas(req);
        OportunidadEducativa o = buscar(id);
        copiar(req, o);
        return OfertaMapper.toDto(o);
    }

    @Transactional
    public void eliminar(Long id) {
        oportunidadRepository.delete(buscar(id));
    }

    public OportunidadEducativa buscar(Long id) {
        return oportunidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Oportunidad", id));
    }

    private void copiar(OportunidadRequest req, OportunidadEducativa o) {
        ProgramaEducativo programa = programaService.buscarEntidad(req.programaId());
        o.setPrograma(programa);
        o.setPeriodoAcademico(req.periodoAcademico().trim());
        o.setFechaAperturaInscripcion(req.fechaAperturaInscripcion());
        o.setFechaCierreInscripcion(req.fechaCierreInscripcion());
        o.setFechaAdmision(req.fechaAdmision());
        o.setFechaMatricula(req.fechaMatricula());
        o.setCupos(req.cupos());
    }

    private void validarFechas(OportunidadRequest req) {
        if (req.fechaCierreInscripcion().isBefore(req.fechaAperturaInscripcion())) {
            throw new ReglaNegocioException("La fecha de cierre no puede ser anterior a la de apertura");
        }
        if (req.fechaAdmision() != null && req.fechaAdmision().isBefore(req.fechaCierreInscripcion())) {
            throw new ReglaNegocioException("La fecha de admisión debe ser posterior al cierre de inscripciones");
        }
        if (req.fechaMatricula() != null && req.fechaAdmision() != null
                && req.fechaMatricula().isBefore(req.fechaAdmision())) {
            throw new ReglaNegocioException("La fecha de matrícula debe ser posterior a la de admisión");
        }
    }
}
