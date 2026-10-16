package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.OfertaMapper;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.RequisitoDto;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Requisito;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import co.edu.uniquindio.poo.sicares.repository.RequisitoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** HU-15 Gestionar requisitos (Administrador.gestionarRequisito). */
@Service
@RequiredArgsConstructor
public class RequisitoService {

    private final RequisitoRepository requisitoRepository;

    @Transactional(readOnly = true)
    public List<RequisitoDto> listar() {
        return requisitoRepository.findAll(Sort.by("nombre")).stream().map(OfertaMapper::toDto).toList();
    }

    @Transactional
    public RequisitoDto crear(RequisitoDto dto) {
        validarValorEsperado(dto);
        Requisito r = new Requisito();
        copiar(dto, r);
        return OfertaMapper.toDto(requisitoRepository.save(r));
    }

    @Transactional
    public RequisitoDto actualizar(Long id, RequisitoDto dto) {
        validarValorEsperado(dto);
        Requisito r = buscar(id);
        copiar(dto, r);
        return OfertaMapper.toDto(r);
    }

    @Transactional
    public void eliminar(Long id) {
        requisitoRepository.delete(buscar(id));
    }

    public Requisito buscar(Long id) {
        return requisitoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Requisito", id));
    }

    public Set<Requisito> buscarVarios(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        List<Requisito> encontrados = requisitoRepository.findAllById(ids);
        if (encontrados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más requisitos no existen");
        }
        return new HashSet<>(encontrados);
    }

    private void copiar(RequisitoDto dto, Requisito r) {
        r.setNombre(dto.nombre().trim());
        r.setDescripcion(dto.descripcion());
        r.setTipo(dto.tipo());
        r.setObligatorio(dto.obligatorio());
        r.setValorEsperado(dto.valorEsperado() == null ? null : dto.valorEsperado().trim());
        // Un requisito de tipo DOCUMENTO siempre exige cargar archivo
        r.setRequiereDocumento(dto.requiereDocumento() || dto.tipo() == TipoRequisito.DOCUMENTO);
    }

    /** Valida el formato de valorEsperado según el tipo (ver TipoRequisito). */
    private void validarValorEsperado(RequisitoDto dto) {
        String v = dto.valorEsperado();
        switch (dto.tipo()) {
            case EDAD -> {
                if (v == null || !v.matches("\\d{1,2}-\\d{1,2}")) {
                    throw new ReglaNegocioException("Para EDAD el valor esperado debe ser un rango, ej. 15-28");
                }
            }
            case NIVEL_EDUCATIVO -> {
                try {
                    NivelEducativo.valueOf(v);
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw new ReglaNegocioException("Para NIVEL_EDUCATIVO el valor esperado debe ser uno de "
                            + List.of(NivelEducativo.values()));
                }
            }
            case SISBEN -> {
                if (v == null || !v.matches("[A-Da-d](\\s*,\\s*[A-Da-d])*")) {
                    throw new ReglaNegocioException("Para SISBEN indica los grupos permitidos, ej. A,B,C");
                }
            }
            default -> {
                // DOCUMENTO, CONDICION_VICTIMA y OTRO: texto libre
            }
        }
    }
}
