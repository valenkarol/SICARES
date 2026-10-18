package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.InstitucionDto;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoInstitucion;
import co.edu.uniquindio.poo.sicares.repository.InstitucionRepository;
import co.edu.uniquindio.poo.sicares.services.InstitucionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de InstitucionService — HU-13 Gestionar instituciones.
 * Caso de TestRail relacionado: CP-11 registrar institución.
 */
@ExtendWith(MockitoExtension.class)
class InstitucionServiceTest {

    private static final String NIT = "900123456-7";

    @Mock
    private InstitucionRepository institucionRepository;

    @InjectMocks
    private InstitucionService institucionService;

    private static InstitucionDto dto(String nombre, String nit) {
        return new InstitucionDto(null, nombre, nit, TipoInstitucion.PUBLICA, " Quindío ", " Armenia ",
                "contacto@prueba.edu.co", null);
    }

    private static Institucion existente(Long id) {
        Institucion i = new Institucion();
        i.setId(id);
        i.setNombre("Institución de Prueba 01");
        i.setNit(NIT);
        i.setTipo(TipoInstitucion.PUBLICA);
        i.setDepartamento("Quindío");
        i.setMunicipio("Armenia");
        return i;
    }

    @Test
    @DisplayName("CP-11: registra una institución nueva y aparece sin programas")
    void creaInstitucion() {
        when(institucionRepository.existsByNit(NIT)).thenReturn(false);
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(inv -> {
            Institucion i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        InstitucionDto r = institucionService.crear(dto("  Institución de Prueba 01 ", NIT));

        assertEquals(1L, r.id());
        assertEquals("Institución de Prueba 01", r.nombre());
        assertEquals("Armenia", r.municipio());
        assertEquals(0, r.totalProgramas());
    }

    @Test
    @DisplayName("HU-13: no permite dos instituciones con el mismo NIT")
    void nitDuplicado() {
        when(institucionRepository.existsByNit(NIT)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> institucionService.crear(dto("Otra institución", NIT)));

        assertTrue(ex.getMessage().contains(NIT));
        verify(institucionRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-13: al editar, no permite usar el NIT de otra institución")
    void actualizarConNitDeOtra() {
        when(institucionRepository.existsByNitAndIdNot(NIT, 2L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> institucionService.actualizar(2L, dto("Editada", NIT)));
    }

    @Test
    @DisplayName("HU-13: edita el nombre de una institución existente")
    void actualizaNombre() {
        Institucion i = existente(1L);
        when(institucionRepository.existsByNitAndIdNot(NIT, 1L)).thenReturn(false);
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(i));

        InstitucionDto r = institucionService.actualizar(1L, dto("Institución de Prueba 01 (editada)", NIT));

        assertEquals("Institución de Prueba 01 (editada)", r.nombre());
        assertEquals("Institución de Prueba 01 (editada)", i.getNombre());
    }

    @Test
    @DisplayName("HU-13: no elimina una institución que tiene programas asociados")
    void noEliminaConProgramas() {
        Institucion i = existente(1L);
        i.agregarPrograma(new ProgramaEducativo());
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(i));

        assertThrows(ReglaNegocioException.class, () -> institucionService.eliminar(1L));
        verify(institucionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("HU-13: elimina una institución sin programas ni beneficios")
    void eliminaSinAsociados() {
        Institucion i = existente(1L);
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(i));

        institucionService.eliminar(1L);

        verify(institucionRepository).delete(i);
    }

    @Test
    @DisplayName("HU-13: consultar una institución que no existe lanza 'no encontrado'")
    void obtenerInexistente() {
        when(institucionRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> institucionService.obtener(9L));
    }

    @Test
    @DisplayName("CP-11: una institución sin nombre o con NIT inválido no pasa la validación")
    void validacionCamposObligatorios() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<InstitucionDto>> errores = validator.validate(dto("", "abc"));

        assertTrue(errores.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
        assertTrue(errores.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nit")));
    }
}

