package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.motorReglas.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del motor de reglas — HU-17 Analizar compatibilidad.
 * Casos de TestRail relacionados: CP-12 (programa compatible) y CP-13 (programa excluido).
 */
class MotorReglasCompatibilidadTest {

    private PerfilEducativo perfil;
    private Institucion institucion;

    /** Perfil completo: 22 años, Armenia, bachiller (MEDIA), víctima en el RUV, SISBÉN A3, sin internet. */
    @BeforeEach
    void setUp() {
        perfil = new PerfilEducativo();

        InformacionPersonal info = new InformacionPersonal();
        info.setNombres("Usuario");
        info.setApellidos("Prueba");
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
        institucion.setNombre("SENA Regional Quindío");
        institucion.setDepartamento("Quindío");
        institucion.setMunicipio("Armenia");
    }

    // ------------------------------------------------------------------ datos de prueba

    private OportunidadEducativa oportunidad(String nombre, NivelEducativo nivel, Requisito... requisitos) {
        ProgramaEducativo p = new ProgramaEducativo();
        p.setNombre(nombre);
        p.setNivel(nivel);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setAreaConocimiento("Tecnología e informática");
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(BigDecimal.ZERO);
        p.setInstitucion(institucion);
        for (Requisito r : requisitos) {
            p.getRequisitos().add(r);
        }
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPrograma(p);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(1));
        o.setFechaCierreInscripcion(LocalDate.now().plusDays(15));
        o.setCupos(20);
        return o;
    }

    private static Requisito requisito(long id, TipoRequisito tipo, String valor, boolean obligatorio) {
        Requisito r = new Requisito();
        r.setId(id);
        r.setNombre(tipo + " " + valor);
        r.setTipo(tipo);
        r.setValorEsperado(valor);
        r.setObligatorio(obligatorio);
        return r;
    }

    private static MotorReglas motor(List<OportunidadEducativa> vigentes) {
        OportunidadEducativaRepository repo = mock(OportunidadEducativaRepository.class);
        when(repo.findVigentes(any())).thenReturn(vigentes);
        return new MotorReglas(List.of(new ReglaRangoEdad(), new ReglaNivelEducativo(), new ReglaRequisitos(),
                new ReglaAccesibilidad(), new ReglaSocioeconomica(), new ReglaIntereses()), repo);
    }

    private static List<String> nombresEnRuta(RutaEducativa ruta) {
        return ruta.getRecomendaciones().stream().map(r -> r.getOportunidad().getPrograma().getNombre()).toList();
    }

    // ------------------------------------------------------------------ CP-12 compatible

    @Test
    @DisplayName("CP-12: un programa cuyo requisito de nivel se cumple aparece como compatible")
    void programaCompatible() {
        OportunidadEducativa compatible = oportunidad("Tecnólogo en Análisis de Software", NivelEducativo.TECNOLOGICO,
                requisito(1, TipoRequisito.NIVEL_EDUCATIVO, "MEDIA", true));
        MotorReglas motor = motor(List.of(compatible));

        ResultadoEvaluacion resultado = motor.evaluar(perfil, compatible);
        RutaEducativa ruta = motor.generarRuta(perfil);

        assertTrue(resultado.isCumple());
        assertTrue(resultado.getPuntaje() >= MotorReglas.PUNTAJE_MINIMO,
                "El puntaje (" + resultado.getPuntaje() + ") supera el mínimo para entrar en la ruta");
        assertEquals(List.of("Tecnólogo en Análisis de Software"), nombresEnRuta(ruta));
    }

    @Test
    @DisplayName("CP-12: el puntaje del motor está entre 0 y 100")
    void puntajeEnRango() {
        ResultadoEvaluacion r = motor(List.of()).evaluar(perfil, oportunidad("Técnico", NivelEducativo.TECNICO));

        assertTrue(r.getPuntaje() >= 0 && r.getPuntaje() <= 100);
    }

    // ------------------------------------------------------------------ CP-13 excluido

    @Test
    @DisplayName("CP-13: un programa que exige un nivel mayor al del usuario se excluye")
    void excluyeNivelNoCumplido() {
        OportunidadEducativa exigente = oportunidad("Especialización de prueba", NivelEducativo.UNIVERSITARIO,
                requisito(2, TipoRequisito.NIVEL_EDUCATIVO, "TECNOLOGICO", true));
        OportunidadEducativa compatible = oportunidad("Tecnólogo de prueba", NivelEducativo.TECNOLOGICO);
        MotorReglas motor = motor(List.of(exigente, compatible));

        ResultadoEvaluacion resultado = motor.evaluar(perfil, exigente);
        RutaEducativa ruta = motor.generarRuta(perfil);

        assertFalse(resultado.isCumple());
        assertEquals(0, resultado.getPuntaje());
        assertTrue(resultado.getObservaciones().get(0).contains("exige nivel TECNOLOGICO"));
        assertEquals(List.of("Tecnólogo de prueba"), nombresEnRuta(ruta), "Solo queda el programa compatible");
    }

    @Test
    @DisplayName("CP-13: un programa fuera del rango de edad se excluye (regla eliminatoria)")
    void excluyeRangoEdad() {
        OportunidadEducativa joven = oportunidad("Programa 14 a 18 años", NivelEducativo.TECNICO,
                requisito(3, TipoRequisito.EDAD, "14-18", true));

        ResultadoEvaluacion r = motor(List.of()).evaluar(perfil, joven);

        assertFalse(r.isCumple());
        assertTrue(r.getObservaciones().get(0).contains("tienes 22"));
    }

    @Test
    @DisplayName("CP-13: un requisito obligatorio de víctima excluye a quien no está en el RUV")
    void excluyeSinRUV() {
        perfil.getSituacionVictima().setInscritoRUV(false);
        OportunidadEducativa soloVictimas = oportunidad("Programa para víctimas", NivelEducativo.TECNICO,
                requisito(4, TipoRequisito.CONDICION_VICTIMA, "RUV", true));

        assertFalse(motor(List.of()).evaluar(perfil, soloVictimas).isCumple());
    }

    @Test
    @DisplayName("HU-17: un requisito de documento no excluye, se informa como observación")
    void documentoSoloInforma() {
        OportunidadEducativa conDocumento = oportunidad("Técnico con documento", NivelEducativo.TECNICO,
                requisito(5, TipoRequisito.DOCUMENTO, "Copia del diploma", true));

        ResultadoEvaluacion r = motor(List.of()).evaluar(perfil, conDocumento);

        assertTrue(r.isCumple());
        assertTrue(r.getObservaciones().stream().anyMatch(o -> o.startsWith("Deberás presentar")));
    }

    @Test
    @DisplayName("HU-17: un programa presencial en otro departamento sin forma de llegar se excluye")
    void excluyeSinTransporte() {
        perfil.getCondicionAccesibilidad().setMedioTransporte("NINGUNO");
        Institucion lejana = new Institucion();
        lejana.setNombre("Institución lejana");
        lejana.setDepartamento("Antioquia");
        OportunidadEducativa lejos = oportunidad("Técnico en Medellín", NivelEducativo.TECNICO);
        lejos.getPrograma().setMunicipio("Medellín");
        lejos.getPrograma().setInstitucion(lejana);

        assertFalse(motor(List.of()).evaluar(perfil, lejos).isCumple());
    }

    // ------------------------------------------------------------------ Generación de la ruta

    @Test
    @DisplayName("CP-16 / HU-17: con el perfil incompleto el motor no genera la ruta")
    void perfilIncompleto() {
        perfil.setCondicionAccesibilidad(null);
        perfil.actualizar();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> motor(List.of()).generarRuta(perfil));
        assertTrue(ex.getMessage().contains("100%"));
    }

    @Test
    @DisplayName("HU-17: la ruta conserva como máximo 10 recomendaciones")
    void maximoDiezRecomendaciones() {
        List<OportunidadEducativa> doce = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            doce.add(oportunidad("Tecnólogo " + i, NivelEducativo.TECNOLOGICO));
        }

        RutaEducativa ruta = motor(doce).generarRuta(perfil);

        assertEquals(MotorReglas.MAX_RECOMENDACIONES, ruta.getRecomendaciones().size());
        assertEquals(10, ruta.getRecomendaciones().get(9).getOrden());
    }

    @Test
    @DisplayName("HU-17: sin oportunidades vigentes la ruta queda vacía")
    void sinOportunidades() {
        assertTrue(motor(List.of()).generarRuta(perfil).getRecomendaciones().isEmpty());
    }
}
