package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.BeneficioRequest;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.BeneficioResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficio;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoBeneficio;
import co.edu.uniquindio.poo.sicares.repository.BeneficioRepository;
import co.edu.uniquindio.poo.sicares.repository.InstitucionRepository;
import co.edu.uniquindio.poo.sicares.repository.ProgramaEducativoRepository;
import co.edu.uniquindio.poo.sicares.repository.RequisitoRepository;
import co.edu.uniquindio.poo.sicares.services.BeneficioService;
import co.edu.uniquindio.poo.sicares.services.InstitucionService;
import co.edu.uniquindio.poo.sicares.services.RequisitoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de BeneficioService — HU-16 Gestionar beneficios.
 * Caso de TestRail relacionado: CP-11 (paso 4, beneficio asociado al programa).
 * InstitucionService.buscar() no es público, así que se usan los servicios reales
 * con sus repositorios simulados (mock).
 */
@ExtendWith(MockitoExtension.class)
class BeneficioServiceTest {

    @Mock
    private BeneficioRepository beneficioRepository;
    @Mock
    private ProgramaEducativoRepository programaRepository;
    @Mock
    private InstitucionRepository institucionRepository;
    @Mock
    private RequisitoRepository requisitoRepository;

    private BeneficioService beneficioService;
    private Institucion institucion;
    private ProgramaEducativo programa;

    @BeforeEach
    void setUp() {
        beneficioService = new BeneficioService(beneficioRepository, programaRepository,
                new InstitucionService(institucionRepository), new RequisitoService(requisitoRepository));

        institucion = new Institucion();
        institucion.setId(1L);
        institucion.setNombre("Institución de Prueba 01");

        programa = new ProgramaEducativo();
        programa.setId(10L);
        programa.setNombre("Técnico de prueba");
        programa.setInstitucion(institucion);
    }

    private BeneficioRequest request(Long institucionId, Set<Long> programaIds, LocalDate fechaLimite) {
        return new BeneficioRequest(" Beca parcial ", TipoBeneficio.BECA, "Cubre parte de la matrícula",
                50, false, fechaLimite, institucionId, programaIds, Set.of());
    }

    @Test
    @DisplayName("CP-11: crea un beneficio asociado a la institución y al programa")
    void creaBeneficioAsociadoAlPrograma() {
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(institucion));
        when(programaRepository.findAllById(any())).thenReturn(List.of(programa));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(inv -> {
            Beneficio b = inv.getArgument(0);
            b.setId(20L);
            return b;
        });

        BeneficioResponse r = beneficioService.crear(request(1L, Set.of(10L), LocalDate.now().plusMonths(2)));

        assertEquals(20L, r.id());
        assertEquals("Beca parcial", r.nombre());
        assertEquals(1L, r.institucionId());
        assertEquals("Institución de Prueba 01", r.institucionNombre());
        assertEquals(List.of(10L), r.programaIds(), "El beneficio queda vinculado al programa");
        assertEquals(50.0, r.porcentajeCobertura());
        assertTrue(r.vigente());
    }

    @Test
    @DisplayName("HU-16: no crea el beneficio si la institución no existe")
    void institucionInexistente() {
        when(institucionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> beneficioService.crear(request(99L, Set.of(10L), null)));
        verify(beneficioRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-16: no crea el beneficio si alguno de los programas no existe")
    void programaInexistente() {
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(institucion));
        when(programaRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class,
                () -> beneficioService.crear(request(1L, Set.of(77L), null)));
        verify(beneficioRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-16: un beneficio con fecha límite vencida deja de estar vigente")
    void beneficioVencido() {
        Beneficio vencido = new Beneficio();
        vencido.setFechaLimite(LocalDate.now().minusDays(1));
        Beneficio sinFecha = new Beneficio();
        Beneficio hoy = new Beneficio();
        hoy.setFechaLimite(LocalDate.now());

        assertFalse(vencido.estaVigente());
        assertTrue(sinFecha.estaVigente(), "Sin fecha límite siempre está vigente");
        assertTrue(hoy.estaVigente(), "El día de la fecha límite todavía está vigente");
    }

    @Test
    @DisplayName("HU-16: eliminar un beneficio inexistente lanza 'no encontrado'")
    void eliminarInexistente() {
        when(beneficioRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> beneficioService.eliminar(5L));
        verify(beneficioRepository, never()).delete(any());
    }
}
