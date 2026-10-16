package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.OfertaMapper;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.BeneficioRequest;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.BeneficioResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficio;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.repository.BeneficioRepository;
import co.edu.uniquindio.poo.sicares.repository.ProgramaEducativoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** HU-16 Gestionar beneficios (Administrador.gestionarBeneficio). */
@Service
@RequiredArgsConstructor
public class BeneficioService {

    private final BeneficioRepository beneficioRepository;
    private final ProgramaEducativoRepository programaRepository;
    private final InstitucionService institucionService;
    private final RequisitoService requisitoService;

    @Transactional(readOnly = true)
    public List<BeneficioResponse> listar() {
        return beneficioRepository.findAll(Sort.by("nombre")).stream().map(OfertaMapper::toDto).toList();
    }

    @Transactional
    public BeneficioResponse crear(BeneficioRequest req) {
        Beneficio b = new Beneficio();
        copiar(req, b);
        return OfertaMapper.toDto(beneficioRepository.save(b));
    }

    @Transactional
    public BeneficioResponse actualizar(Long id, BeneficioRequest req) {
        Beneficio b = buscar(id);
        copiar(req, b);
        return OfertaMapper.toDto(b);
    }

    @Transactional
    public void eliminar(Long id) {
        beneficioRepository.delete(buscar(id));
    }

    private Beneficio buscar(Long id) {
        return beneficioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficio", id));
    }

    private void copiar(BeneficioRequest req, Beneficio b) {
        b.setNombre(req.nombre().trim());
        b.setTipo(req.tipo());
        b.setDescripcion(req.descripcion());
        b.setPorcentajeCobertura(req.porcentajeCobertura());
        b.setExclusivoVictimas(req.exclusivoVictimas());
        b.setFechaLimite(req.fechaLimite());
        b.setInstitucion(institucionService.buscar(req.institucionId()));
        b.setProgramas(buscarProgramas(req.programaIds()));
        b.setRequisitos(requisitoService.buscarVarios(req.requisitoIds()));
    }

    private Set<ProgramaEducativo> buscarProgramas(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        List<ProgramaEducativo> encontrados = programaRepository.findAllById(ids);
        if (encontrados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más programas no existen");
        }
        return new HashSet<>(encontrados);
    }
}
