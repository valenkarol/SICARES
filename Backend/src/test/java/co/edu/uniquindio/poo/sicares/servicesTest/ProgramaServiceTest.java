package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.ProgramaRequest;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.ProgramaResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.entities.Requisito;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoInstitucion;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import co.edu.uniquindio.poo.sicares.repository.InstitucionRepository;
import co.edu.uniquindio.poo.sicares.repository.ProgramaEducativoRepository;
import co.edu.uniquindio.poo.sicares.repository.RequisitoRepository;
import co.edu.uniquindio.poo.sicares.services.InstitucionService;
import co.edu.uniquindio.poo.sicares.services.ProgramaService;
import co.edu.uniquindio.poo.sicares.services.RequisitoService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ProgramaService — HU-12 Consultar programas y HU-14 Gestionar programas.
 * Casos de TestRail relacionados: CP-10 (filtros) y CP-11
 */
@ExtendWith(MockitoExtension.class)
class ProgramaServiceTest {

    @Mock
    private ProgramaEducativoRepository programaRepository;
    @Mock
    private InstitucionRepository institucionRepository;
    @Mock
    private RequisitoRepository requisitoRepository;

    private ProgramaService programaService;
    private Institucion institucion;

    @BeforeEach
    void setUp() {
        programaService = new ProgramaService(programaRepository,
                new InstitucionService(institucionRepository), new RequisitoService(requisitoRepository));

        institucion = new Institucion();
        institucion.setId(1L);
        institucion.setNombre("Institución de Prueba 01");
        institucion.setTipo(TipoInstitucion.PUBLICA);
    }

    private static ProgramaRequest request(String nombre, Long institucionId, Set<Long> requisitoIds) {
        return new ProgramaRequest(nombre, NivelEducativo.TECNICO, Modalidad.PRESENCIAL, "Tecnología e informática",
                3, BigDecimal.ZERO, " Armenia ", true, institucionId, requisitoIds);
    }

    private ProgramaEducativo programaExistente(Long id, String nombre) {
        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(id);
        p.setNombre(nombre);
        p.setNivel(NivelEducativo.TECNICO);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setAreaConocimiento("Tecnología e informática");
        p.setMunicipio("Armenia");
        institucion.agregarPrograma(p);
        return p;
    }

    // ------------------------------------------------------------------ HU-14 Gestionar programas (CP-11)

    @Test
    @DisplayName("CP-11: crea un programa vinculado a la institución y con su requisito")
    void creaProgramaConInstitucionYRequisito() {
        Requisito bachiller = new Requisito();
        bachiller.setId(5L);
        bachiller.setNombre("Nivel educativo mínimo: bachiller");
        bachiller.setTipo(TipoRequisito.NIVEL_EDUCATIVO);
        bachiller.setValorEsperado("MEDIA");

        when(institucionRepository.findById(1L)).thenReturn(Optional.of(institucion));
        when(requisitoRepository.findAllById(any())).thenReturn(List.of(bachiller));
        when(programaRepository.save(any(ProgramaEducativo.class))).thenAnswer(inv -> {
            ProgramaEducativo p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProgramaResponse r = programaService.crear(request(" Técnico de Prueba ", 1L, Set.of(5L)));

        assertEquals(10L, r.id());
        assertEquals("Técnico de Prueba", r.nombre());
        assertEquals("Armenia", r.municipio());
        assertEquals(1L, r.institucionId(), "El programa queda vinculado a la institución");
        assertEquals(1, r.requisitos().size(), "El requisito queda asociado al programa");
        assertEquals("MEDIA", r.requisitos().get(0).valorEsperado());
        assertTrue(r.gratuito());
        assertEquals(1, institucion.getProgramas().size());
    }

    @Test
    @DisplayName("HU-14: no crea el programa si la institución no existe")
    void institucionInexistente() {
        when(institucionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> programaService.crear(request("Técnico", 99L, null)));
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-11: edita el nombre del programa y el cambio se refleja")
    void editaNombre() {
        ProgramaEducativo p = programaExistente(7L, "Técnico de Prueba");
        when(programaRepository.findById(7L)).thenReturn(Optional.of(p));

        ProgramaResponse r = programaService.actualizar(7L, request("Técnico de Prueba (editado)", 1L, null));

        assertEquals("Técnico de Prueba (editado)", r.nombre());
        assertEquals("Técnico de Prueba (editado)", p.getNombre());
    }

    @Test
    @DisplayName("HU-14: al cambiar de institución, el programa se mueve a la nueva")
    void cambiaDeInstitucion() {
        ProgramaEducativo p = programaExistente(7L, "Técnico de Prueba");
        Institucion otra = new Institucion();
        otra.setId(2L);
        otra.setNombre("Otra institución");
        when(programaRepository.findById(7L)).thenReturn(Optional.of(p));
        when(institucionRepository.findById(2L)).thenReturn(Optional.of(otra));

        programaService.actualizar(7L, request("Técnico de Prueba", 2L, null));

        assertSame(otra, p.getInstitucion());
        assertTrue(institucion.getProgramas().isEmpty());
        assertEquals(1, otra.getProgramas().size());
    }

    @Test
    @DisplayName("CP-11: un programa con campos obligatorios vacíos no pasa la validación")
    void validacionCamposObligatorios() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ProgramaRequest incompleto = new ProgramaRequest("", null, Modalidad.PRESENCIAL, "", 0,
                BigDecimal.ZERO, "Armenia", true, null, null);

        Set<ConstraintViolation<ProgramaRequest>> errores = validator.validate(incompleto);
        List<String> campos = errores.stream().map(v -> v.getPropertyPath().toString()).toList();

        assertTrue(campos.containsAll(List.of("nombre", "nivel", "areaConocimiento", "duracionPeriodos", "institucionId")),
                "Campos con error: " + campos);
    }

    // ------------------------------------------------------------------ HU-12 Consultar programas (CP-10)

    @Test
    @DisplayName("CP-10: los filtros vacíos se envían como null y el texto se limpia")
    void filtrosNormalizados() {
        ProgramaEducativo p = programaExistente(3L, "Técnico en Sistemas");
        when(programaRepository.buscar(true, NivelEducativo.TECNICO, null, "Armenia", null, null))
                .thenReturn(List.of(p));

        List<ProgramaResponse> r = programaService.buscar(true, NivelEducativo.TECNICO, null, " Armenia ", "  ", "");

        assertEquals(1, r.size());
        assertEquals("Técnico en Sistemas", r.get(0).nombre());
    }

    @Test
    @DisplayName("CP-10: un filtro sin resultados devuelve una lista vacía sin error")
    void filtroSinResultados() {
        when(programaRepository.buscar(true, null, null, "Municipio sin programas", null, null)).thenReturn(List.of());

        assertTrue(programaService.buscar(true, null, null, "Municipio sin programas", null, null).isEmpty());
    }

    @Test
    @DisplayName("CP-10: sin filtros se consulta el listado completo")
    void sinFiltros() {
        ProgramaEducativo a = programaExistente(1L, "A");
        ProgramaEducativo b = programaExistente(2L, "B");
        when(programaRepository.buscar(true, null, null, null, null, null)).thenReturn(List.of(a, b));

        assertEquals(2, programaService.buscar(true, null, null, null, null, null).size());
    }

    @Test
    @DisplayName("HU-12: consultar un programa que no existe lanza 'no encontrado'")
    void programaInexistente() {
        when(programaRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> programaService.obtener(50L));
    }
}
