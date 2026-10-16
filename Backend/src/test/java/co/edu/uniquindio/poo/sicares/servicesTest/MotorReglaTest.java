package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoDocumento;
import co.edu.uniquindio.poo.sicares.models.enums.Zona;
import co.edu.uniquindio.poo.sicares.models.enums.TipoRequisito;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.motorReglas.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas unitarias (caja blanca) del motor de reglas — patrón Strategy (HU-17). */
class MotorReglasTest {

    private PerfilEducativo perfil;
    private Institucion institucion;

    @BeforeEach
    void setUp() {
        perfil = new PerfilEducativo();

        InformacionPersonal info = new InformacionPersonal();
        info.setNombres("Ana");
        info.setApellidos("Pérez");
        info.setTipoDocumento(TipoDocumento.CC);
        info.setNumeroDocumento("1094000000");
        info.setFechaNacimiento(LocalDate.now().minusYears(22));
        info.setDepartamento("Quindío");
        info.setMunicipio("Armenia");
        info.setZona(Zona.URBANA);
        perfil.setInformacionPersonal(info);

        SituacionVictima v = new SituacionVictima();
        v.setEsVictima(true);
        v.setInscritoRUV(true);
        perfil.setSituacionVictima(v);

        SituacionSocioeconomica s = new SituacionSocioeconomica();
        s.setEstrato(1);
        s.setGrupoSisben("A3");
        s.setIngresoMensualHogar(new BigDecimal("1300000"));
        s.setIntegrantesHogar(4);
        perfil.setSituacionSocioeconomica(s);

        CondicionAccesibilidad c = new CondicionAccesibilidad();
        c.setTieneInternet(false);
        c.setTieneDispositivos(true);
        c.setMedioTransporte("PUBLICO");
        c.setDistanciaMaximaKm(10);
        perfil.setCondicionAccesibilidad(c);

        TrayectoriaEducativa t = new TrayectoriaEducativa();
        t.setNivelActual(NivelEducativo.MEDIA);
        t.setUltimoGradoAprobado(11);
        t.setAnioUltimoEstudio(2020);
        perfil.setTrayectoriaEducativa(t);

        perfil.getIntereses().add(new InteresProfesional("Tecnología e informática", null));
        perfil.actualizar();

        institucion = new Institucion();
        institucion.setNombre("SENA");
        institucion.setDepartamento("Quindío");
        institucion.setMunicipio("Armenia");
    }

    private OportunidadEducativa oportunidad(NivelEducativo nivel, Modalidad modalidad, String area, long costo) {
        ProgramaEducativo p = new ProgramaEducativo();
        p.setNombre("Programa de prueba");
        p.setNivel(nivel);
        p.setModalidad(modalidad);
        p.setAreaConocimiento(area);
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(BigDecimal.valueOf(costo));
        p.setInstitucion(institucion);
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPrograma(p);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(1));
        o.setFechaCierreInscripcion(LocalDate.now().plusDays(10));
        o.setCupos(10);
        return o;
    }

    private MotorReglas motor(OportunidadEducativa... ops) {
        OportunidadEducativaRepository repo = mock(OportunidadEducativaRepository.class);
        when(repo.findVigentes(any())).thenReturn(List.of(ops));
        return new MotorReglas(List.of(new ReglaRangoEdad(), new ReglaNivelEducativo(), new ReglaRequisitos(),
                new ReglaAccesibilidad(), new ReglaSocioeconomica(), new ReglaIntereses()), repo);
    }

    @Test
    @DisplayName("El perfil de prueba queda completo al 100%")
    void perfilCompleto() {
        assertTrue(perfil.isCompleto());
        assertEquals(100, perfil.getPorcentajeCompletitud());
    }

    @Test
    @DisplayName("No recomienda un nivel que el usuario ya alcanzó")
    void descartaNivelInferior() {
        ResultadoEvaluacion r = new ReglaNivelEducativo()
                .evaluar(perfil, oportunidad(NivelEducativo.BASICA_SECUNDARIA, Modalidad.PRESENCIAL, "Formación general", 0));
        assertFalse(r.isCumple());
    }

    @Test
    @DisplayName("Un tecnólogo es el siguiente paso para quien terminó la media")
    void siguientePaso() {
        ResultadoEvaluacion r = new ReglaNivelEducativo()
                .evaluar(perfil, oportunidad(NivelEducativo.TECNOLOGICO, Modalidad.PRESENCIAL, "Salud", 0));
        assertTrue(r.isCumple());
        assertEquals(1.0, r.getPuntaje());
    }

    @Test
    @DisplayName("Un programa virtual se descarta si el usuario no tiene internet")
    void virtualSinInternet() {
        ResultadoEvaluacion r = new ReglaAccesibilidad()
                .evaluar(perfil, oportunidad(NivelEducativo.TECNOLOGICO, Modalidad.VIRTUAL, "Salud", 0));
        assertFalse(r.isCumple());
    }

    @Test
    @DisplayName("La ruta ordena primero el programa que coincide con los intereses")
    void rutaOrdenada() {
        OportunidadEducativa salud = oportunidad(NivelEducativo.TECNOLOGICO, Modalidad.PRESENCIAL, "Salud", 0);
        OportunidadEducativa tec = oportunidad(NivelEducativo.TECNOLOGICO, Modalidad.PRESENCIAL, "Tecnología e informática", 0);
        RutaEducativa ruta = motor(salud, tec).generarRuta(perfil);

        assertEquals(2, ruta.getRecomendaciones().size());
        assertSame(tec, ruta.getRecomendaciones().get(0).getOportunidad());
        assertEquals(1, ruta.getRecomendaciones().get(0).getOrden());
    }

    @Test
    @DisplayName("Requisito SISBEN A,B,C se cumple con grupo A3")
    void requisitoSisben() {
        OportunidadEducativa op = oportunidad(NivelEducativo.TECNOLOGICO, Modalidad.PRESENCIAL, "Salud", 0);
        Requisito r = new Requisito();
        r.setId(1L);
        r.setTipo(TipoRequisito.SISBEN);
        r.setValorEsperado("A,B,C");
        r.setObligatorio(true);
        op.getPrograma().getRequisitos().add(r);
        assertTrue(new ReglaRequisitos().evaluar(perfil, op).isCumple());

        perfil.getSituacionSocioeconomica().setGrupoSisben("D5");
        assertFalse(new ReglaRequisitos().evaluar(perfil, op).isCumple());
    }
}
