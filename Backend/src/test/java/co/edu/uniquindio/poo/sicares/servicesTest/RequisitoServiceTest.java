package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.RequisitoDto;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Requisito;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import co.edu.uniquindio.poo.sicares.repository.RequisitoRepository;
import co.edu.uniquindio.poo.sicares.services.RequisitoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de RequisitoService — HU-15 Gestionar requisitos.
 * Caso de TestRail relacionado: CP-11 (paso 3, requisito asociado al programa).

 */
@ExtendWith(MockitoExtension.class)
class RequisitoServiceTest {

    @Mock
    private RequisitoRepository requisitoRepository;

    @InjectMocks
    private RequisitoService requisitoService;

    private RequisitoDto dto(TipoRequisito tipo, String valor, boolean requiereDocumento) {
        return new RequisitoDto(null, "  Requisito de prueba ", "Descripción", tipo, true, valor, requiereDocumento);
    }

    private void guardarDevuelveConId() {
        when(requisitoRepository.save(any(Requisito.class))).thenAnswer(inv -> {
            Requisito r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });
    }

    @ParameterizedTest(name = "acepta {0} = \"{1}\"")
    @CsvSource({
            "EDAD, 15-28",
            "NIVEL_EDUCATIVO, MEDIA",
            "SISBEN, 'A,B,C'",
            "CONDICION_VICTIMA, RUV",
            "OTRO, Carta de motivación"
    })
    @DisplayName("CP-11: crea requisitos con un valor esperado válido para su tipo")
    void creaRequisitoValido(TipoRequisito tipo, String valor) {
        guardarDevuelveConId();

        RequisitoDto r = requisitoService.crear(dto(tipo, valor, false));

        assertEquals(10L, r.id());
        assertEquals("Requisito de prueba", r.nombre(), "El nombre se guarda sin espacios sobrantes");
        assertEquals(tipo, r.tipo());
        assertEquals(valor, r.valorEsperado());
    }

    @ParameterizedTest(name = "rechaza {0} = \"{1}\"")
    @CsvSource({
            "EDAD, quince",
            "EDAD, 15",
            "NIVEL_EDUCATIVO, BACHILLER",
            "SISBEN, E",
            "SISBEN, A;B"
    })
    @DisplayName("HU-15: rechaza un valor esperado con formato inválido y no guarda")
    void rechazaValorInvalido(TipoRequisito tipo, String valor) {
        assertThrows(ReglaNegocioException.class, () -> requisitoService.crear(dto(tipo, valor, false)));
        verify(requisitoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-15: un requisito de nivel educativo sin valor se rechaza")
    void nivelEducativoSinValor() {
        assertThrows(ReglaNegocioException.class,
                () -> requisitoService.crear(dto(TipoRequisito.NIVEL_EDUCATIVO, null, false)));
    }

    @Test
    @DisplayName("HU-15: un requisito de tipo DOCUMENTO siempre exige cargar archivo")
    void documentoRequiereArchivo() {
        guardarDevuelveConId();

        RequisitoDto r = requisitoService.crear(dto(TipoRequisito.DOCUMENTO, "Copia del documento", false));

        assertTrue(r.requiereDocumento());
    }

    @Test
    @DisplayName("HU-15: actualiza un requisito existente")
    void actualizaRequisito() {
        Requisito existente = new Requisito();
        existente.setId(3L);
        existente.setNombre("Edad");
        existente.setTipo(TipoRequisito.EDAD);
        existente.setValorEsperado("15-28");
        when(requisitoRepository.findById(3L)).thenReturn(Optional.of(existente));

        RequisitoDto r = requisitoService.actualizar(3L, dto(TipoRequisito.EDAD, "14-30", false));

        assertEquals("14-30", r.valorEsperado());
        assertEquals("14-30", existente.getValorEsperado());
    }

    @Test
    @DisplayName("HU-15: actualizar un requisito que no existe lanza 'no encontrado'")
    void actualizaRequisitoInexistente() {
        when(requisitoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> requisitoService.actualizar(99L, dto(TipoRequisito.EDAD, "15-28", false)));
    }

    @Test
    @DisplayName("HU-15: buscarVarios falla si alguno de los requisitos no existe")
    void buscarVariosConFaltantes() {
        Requisito r = new Requisito();
        r.setId(1L);
        when(requisitoRepository.findAllById(any())).thenReturn(List.of(r));

        assertThrows(RecursoNoEncontradoException.class, () -> requisitoService.buscarVarios(Set.of(1L, 2L)));
    }

    @Test
    @DisplayName("HU-15: buscarVarios sin ids devuelve un conjunto vacío sin consultar la BD")
    void buscarVariosVacio() {
        assertTrue(requisitoService.buscarVarios(Set.of()).isEmpty());
        verifyNoInteractions(requisitoRepository);
    }
}
