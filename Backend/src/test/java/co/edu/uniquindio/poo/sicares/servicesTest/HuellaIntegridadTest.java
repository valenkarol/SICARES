package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import co.edu.uniquindio.poo.sicares.services.motorReglas.HuellaIntegridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;


class HuellaIntegridadTest {

    private OportunidadEducativa oportunidad;
    private Requisito requisito;
    private Beneficio beneficio;
    private String huellaOriginal;

    @BeforeEach
    void setUp() {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        inst.setDepartamento("Quindío");
        inst.setMunicipio("Armenia");

        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(2L);
        p.setNombre("Tecnólogo en Análisis de Software");
        p.setNivel(NivelEducativo.TECNOLOGICO);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setAreaConocimiento("Tecnología e informática");
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(new BigDecimal("1000000.00"));
        p.setInstitucion(inst);

        requisito = new Requisito();
        requisito.setId(3L);
        requisito.setNombre("Edad");
        requisito.setTipo(TipoRequisito.EDAD);
        requisito.setValorEsperado("15-28");
        requisito.setObligatorio(true);
        p.getRequisitos().add(requisito);

        beneficio = new Beneficio();
        beneficio.setId(4L);
        beneficio.setNombre("Beca");
        beneficio.setPorcentajeCobertura(50);
        p.getBeneficios().add(beneficio);

        oportunidad = new OportunidadEducativa();
        oportunidad.setId(5L);
        oportunidad.setPrograma(p);
        oportunidad.setPeriodoAcademico("2027-1");
        oportunidad.setFechaAperturaInscripcion(LocalDate.of(2026, 11, 1));
        oportunidad.setFechaCierreInscripcion(LocalDate.of(2026, 12, 1));
        oportunidad.setCupos(30);

        huellaOriginal = HuellaIntegridad.deOportunidad(oportunidad);
    }

    @Test
    @DisplayName("La misma información siempre produce la misma huella (sin falsos positivos)")
    void mismaInformacionMismaHuella() {
        assertEquals(huellaOriginal, HuellaIntegridad.deOportunidad(oportunidad));
        assertEquals(64, huellaOriginal.length(), "SHA-256 en hexadecimal");
        assertFalse(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Escribir el mismo costo con otra escala (1000000 vs 1000000.00) no cuenta como alteración")
    void mismaCifraOtraEscala() {
        oportunidad.getPrograma().setCostoPeriodo(new BigDecimal("1000000"));
        assertFalse(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Que bajen los cupos no invalida la recomendación")
    void cuposNoAlteranHuella() {
        oportunidad.setCupos(29);
        assertFalse(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración del nivel del programa")
    void detectaNivel() {
        oportunidad.getPrograma().setNivel(NivelEducativo.UNIVERSITARIO);
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración del municipio del programa")
    void detectaMunicipio() {
        oportunidad.getPrograma().setMunicipio("Calarcá");
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración del costo del programa")
    void detectaCosto() {
        oportunidad.getPrograma().setCostoPeriodo(new BigDecimal("2000000"));
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración del valor esperado de un requisito")
    void detectaRequisito() {
        requisito.setValorEsperado("15-30");
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta un requisito agregado")
    void detectaRequisitoAgregado() {
        Requisito nuevo = new Requisito();
        nuevo.setId(9L);
        nuevo.setTipo(TipoRequisito.CONDICION_VICTIMA);
        nuevo.setValorEsperado("RUV");
        oportunidad.getPrograma().getRequisitos().add(nuevo);
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración de la cobertura de un beneficio")
    void detectaBeneficio() {
        beneficio.setPorcentajeCobertura(100);
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración de las fechas de la convocatoria")
    void detectaFechas() {
        oportunidad.setFechaCierreInscripcion(LocalDate.of(2026, 11, 15));
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Detecta alteración del departamento de la institución")
    void detectaInstitucion() {
        oportunidad.getPrograma().getInstitucion().setDepartamento("Risaralda");
        assertTrue(HuellaIntegridad.fueAlterada(huellaOriginal, oportunidad));
    }

    @Test
    @DisplayName("Una recomendación antigua (sin huella guardada) no se marca como alterada")
    void sinHuellaGuardada() {
        assertFalse(HuellaIntegridad.fueAlterada(null, oportunidad));
    }
}
