package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.OportunidadRequest;
import co.edu.uniquindio.poo.sicares.dto.OfertaDtos.OportunidadResponse;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.Institucion;
import co.edu.uniquindio.poo.sicares.models.entities.OportunidadEducativa;
import co.edu.uniquindio.poo.sicares.models.entities.ProgramaEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.OportunidadService;
import co.edu.uniquindio.poo.sicares.services.ProgramaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de OportunidadService — HU-14 convocatorias.
 * Apoya los casos CP-10 y CP-14: solo las convocatorias vigentes llegan al usuario y al motor.
 */
@ExtendWith(MockitoExtension.class)
class OportunidadServiceTest {

    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private OportunidadEducativaRepository oportunidadRepository;
    @Mock
    private ProgramaService programaService;

    @InjectMocks
    private OportunidadService oportunidadService;

    private static ProgramaEducativo programa() {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(10L);
        p.setNombre("Técnico en Sistemas");
        p.setNivel(NivelEducativo.TECNICO);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setMunicipio("Armenia");
        p.setInstitucion(inst);
        return p;
    }

    private static OportunidadEducativa oportunidad(String periodo, LocalDate cierre, int cupos) {
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPrograma(programa());
        o.setPeriodoAcademico(periodo);
        o.setFechaAperturaInscripcion(HOY.minusDays(10));
        o.setFechaCierreInscripcion(cierre);
        o.setCupos(cupos);
        return o;
    }

    private static OportunidadRequest request(LocalDate apertura, LocalDate cierre, LocalDate admision, LocalDate matricula) {
        return new OportunidadRequest(" 2027-1 ", apertura, cierre, admision, matricula, 30, 10L);
    }

    @Test
    @DisplayName("HU-14: crea una convocatoria con fechas en orden correcto")
    void creaConvocatoria() {
        when(programaService.buscarEntidad(10L)).thenReturn(programa());
        when(oportunidadRepository.save(any(OportunidadEducativa.class))).thenAnswer(inv -> inv.getArgument(0));

        OportunidadResponse r = oportunidadService.crear(
                request(HOY, HOY.plusDays(30), HOY.plusDays(40), HOY.plusDays(50)));

        assertEquals("2027-1", r.periodoAcademico());
        assertEquals("Técnico en Sistemas", r.programaNombre());
        assertTrue(r.vigente());
        assertEquals(30, r.diasParaCierre());
    }

    @Test
    @DisplayName("HU-14: la fecha de cierre no puede ser anterior a la de apertura")
    void cierreAntesDeApertura() {
        assertThrows(ReglaNegocioException.class,
                () -> oportunidadService.crear(request(HOY, HOY.minusDays(1), null, null)));
        verify(oportunidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-14: la admisión debe ser posterior al cierre de inscripciones")
    void admisionAntesDelCierre() {
        assertThrows(ReglaNegocioException.class,
                () -> oportunidadService.crear(request(HOY, HOY.plusDays(30), HOY.plusDays(10), null)));
    }

    @Test
    @DisplayName("HU-14: la matrícula debe ser posterior a la admisión")
    void matriculaAntesDeAdmision() {
        assertThrows(ReglaNegocioException.class,
                () -> oportunidadService.crear(request(HOY, HOY.plusDays(30), HOY.plusDays(40), HOY.plusDays(35))));
    }

    @Test
    @DisplayName("CP-10/CP-14: solo se listan convocatorias vigentes y con cupos, ordenadas por cierre")
    void listaSoloVigentesOrdenadas() {
        OportunidadEducativa cierraTarde = oportunidad("2027-2", HOY.plusDays(60), 10);
        OportunidadEducativa cierraPronto = oportunidad("2027-1", HOY.plusDays(5), 10);
        OportunidadEducativa sinCupos = oportunidad("2027-1B", HOY.plusDays(20), 0);
        when(oportunidadRepository.findVigentes(any())).thenReturn(List.of(cierraTarde, sinCupos, cierraPronto));

        List<OportunidadResponse> r = oportunidadService.listar(true, null);

        assertEquals(List.of("2027-1", "2027-2"), r.stream().map(OportunidadResponse::periodoAcademico).toList());
    }

    @Test
    @DisplayName("HU-14: una convocatoria cerrada o sin cupos no está vigente")
    void vigencia() {
        assertFalse(oportunidad("cerrada", HOY.minusDays(1), 10).estaVigente());
        assertFalse(oportunidad("sin cupos", HOY.plusDays(10), 0).estaVigente());
        assertTrue(oportunidad("cierra hoy", HOY, 1).estaVigente(), "El día del cierre todavía está abierta");
    }
}
