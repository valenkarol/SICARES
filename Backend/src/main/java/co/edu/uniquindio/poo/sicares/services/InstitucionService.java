package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.InstitucionDto;
import co.edu.uniquindio.poo.sicares.dto.OfertaMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.repository.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestionar instituciones (Administrador.gestionarInstitucion). */
@Service
@RequiredArgsConstructor
public class InstitucionService {

    private final InstitucionRepository institucionRepository;

    @Transactional(readOnly = true)
    public List<OfertaDtos.InstitucionDto> listar() {
        return institucionRepository.findAll(Sort.by("nombre")).stream().map(OfertaMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InstitucionDto obtener(Long id) {
        return OfertaMapper.toDto(buscar(id));
    }

    @Transactional
    public InstitucionDto crear(InstitucionDto dto) {
        if (institucionRepository.existsByNit(dto.nit())) {
            throw new ReglaNegocioException("Ya existe una institución con el NIT " + dto.nit());
        }
        Institucion i = new Institucion();
        copiar(dto, i);
        return OfertaMapper.toDto(institucionRepository.save(i));
    }

    @Transactional
    public InstitucionDto actualizar(Long id, InstitucionDto dto) {
        if (institucionRepository.existsByNitAndIdNot(dto.nit(), id)) {
            throw new ReglaNegocioException("Ya existe otra institución con el NIT " + dto.nit());
        }
        Institucion i = buscar(id);
        copiar(dto, i);
        return OfertaMapper.toDto(i);
    }

    @Transactional
    public void eliminar(Long id) {
        Institucion i = buscar(id);
        if (!i.getProgramas().isEmpty() || !i.getBeneficios().isEmpty()) {
            throw new ReglaNegocioException(
                    "No se puede eliminar: la institución tiene programas o beneficios asociados");
        }
        institucionRepository.delete(i);
    }

    Institucion buscar(Long id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Institución", id));
    }

    private void copiar(InstitucionDto dto, Institucion i) {
        i.setNombre(dto.nombre().trim());
        i.setNit(dto.nit().trim());
        i.setTipo(dto.tipo());
        i.setDepartamento(dto.departamento().trim());
        i.setMunicipio(dto.municipio().trim());
        i.setCorreoContacto(dto.correoContacto());
    }
}
