package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.PostulacionResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import co.edu.uniquindio.poo.sicares.repository.PostulacionRepository;
import co.edu.uniquindio.poo.sicares.services.PostulacionDirectaService;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaNivelEducativo;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaRangoEdad;
import co.edu.uniquindio.poo.sicares.services.motorReglas.ReglaRequisitos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Auditoría - Plan de mejoramiento #2: postulación desde el catálogo de ofertas,
 * sin depender de los intereses marcados ni de la ruta educativa, pero SÍ de los requisitos del programa.
 * Las reglas del motor son objetos reales (no mocks): así se prueba la validación de requisitos de verdad.
 */
@ExtendWith(MockitoExtension.class)
class PostulacionDirectaServiceTest {

    private static final Long BENEFICIARIO_ID = 1L;
    private static final Long OPORTUNIDAD_ID = 4L;

    @Mock private PostulacionRepository postulacionRepository;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private OportunidadEducativaRepository oportunidadRepository;
    @Mock private ServicioNotificaciones servicioNotificaciones;

    private PostulacionDirectaService service;
    private Beneficiario beneficiario;

    @BeforeEach
    void setUp() {
        service = new PostulacionDirectaService(postulacionRepository, beneficiarioRepository,
                oportunidadRepository, servicioNotificaciones,
                new ReglaNivelEducativo(), new ReglaRangoEdad(), new ReglaRequisitos());
        beneficiario = new Beneficiario();
        beneficiario.setId(BENEFICIARIO_ID);
        beneficiario.setCorreo("ana@correo.com");
        beneficiario.setPerfil(perfilCompleto(NivelEducativo.MEDIA, 11, 22));
    }

    /** Perfil 100% caracterizado: víctima en el RUV, SISBÉN A3, Armenia; nivel, grado y edad variables. */
    private PerfilEducativo perfilCompleto(NivelEducativo nivel, int ultimoGrado, int edad) {
        PerfilEducativo perfil = new PerfilEducativo();
        InformacionPersonal info = new InformacionPersonal();
        info.setNombres("Ana");
        info.setApellidos("Prueba");
        info.setTipoDocumento(TipoDocumento.CC);
        info.setNumeroDocumento("1094000000");
        info.setFechaNacimiento(LocalDate.now().minusYears(edad));
        info.setDepartamento("Quindío");
        info.setMunicipio("Armenia");
        info.setZona(Zona.URBANA);
        perfil.setInformacionPersonal(info);

        SituacionVictima v = new SituacionVictima();
        v.setEsVictima(true);
        v.setInscritoRUV(true);
        perfil.setSituacionVictima(v);

        SituacionSocioeconomica s = new SituacionSocioeconomica();
        s.setGrupoSisben("A3");
        perfil.setSituacionSocioeconomica(s);

        TrayectoriaEducativa t = new TrayectoriaEducativa();
        t.setNivelActual(nivel);
        t.setUltimoGradoAprobado(ultimoGrado);
        perfil.setTrayectoriaEducativa(t);

        perfil.setCompleto(true);
        return perfil;
    }

    /** Requisito "Haber culminado la educación media" (grado 11), como en DataInitializer. */
    private static Requisito requisitoMedia() {
        Requisito r = new Requisito();
        r.setId(20L);
        r.setNombre("Haber culminado la educación media");
        r.setTipo(TipoRequisito.NIVEL_EDUCATIVO);
        r.setValorEsperado("MEDIA");
        return r;
    }

    private OportunidadEducativa oportunidad(LocalDate cierre, int cupos, boolean programaActivo) {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        inst.setTipo(TipoInstitucion.PUBLICA);

        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(3L);
        p.setNombre("Técnico en Cocina");
        p.setNivel(NivelEducativo.TECNICO); // el técnico admite desde grado 9°
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(BigDecimal.ZERO);
        p.setActivo(programaActivo);
        p.setInstitucion(inst);

        OportunidadEducativa o = new OportunidadEducativa();
        o.setId(OPORTUNIDAD_ID);
        o.setPrograma(p);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(5));
        o.setFechaCierreInscripcion(cierre);
        o.setFechaAdmision(LocalDate.now().plusDays(35));
        o.setFechaMatricula(LocalDate.now().plusDays(50));
        o.setCupos(cupos);
        return o;
    }

    @Test
    @DisplayName("Crea la postulación SELECCIONADA sin recomendación y con origen 'catálogo'")
    void postulaDesdeCatalogo() {
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, true);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));
        when(postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(eq(BENEFICIARIO_ID),
                eq(OPORTUNIDAD_ID), anyList())).thenReturn(false);
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(inv -> {
            Postulacion p = inv.getArgument(0);
            p.setId(50L);
            return p;
        });

        PostulacionResponse resp = service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID);

        assertNotNull(resp);
        ArgumentCaptor<Postulacion> captor = ArgumentCaptor.forClass(Postulacion.class);
        verify(postulacionRepository).save(captor.capture());
        Postulacion guardada = captor.getValue();
        assertEquals(EstadoPostulacion.SELECCIONADA, guardada.getEstado());
        assertNull(guardada.getRecomendacion());
        assertTrue(guardada.getHistorial().get(0).getObservacion().contains("catálogo"));
        verify(servicioNotificaciones).programarRecordatorios(any(Postulacion.class));
        verify(servicioNotificaciones).notificar(eq(beneficiario), any(Postulacion.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Rechaza convocatorias cerradas")
    void rechazaConvocatoriaCerrada() {
        OportunidadEducativa op = oportunidad(LocalDate.now().minusDays(1), 30, true);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));

        assertThrows(ReglaNegocioException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza programas desactivados por el administrador")
    void rechazaProgramaInactivo() {
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, false);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));

        assertThrows(ReglaNegocioException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("No permite dos postulaciones activas a la misma convocatoria")
    void rechazaDuplicada() {
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, true);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));
        when(postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(eq(BENEFICIARIO_ID),
                eq(OPORTUNIDAD_ID), anyList())).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Grado 9° NO puede postularse a Enfermería (exige grado 11 / media)")
    void noventoGradoNoPuedeEnfermeria() {
        beneficiario.setPerfil(perfilCompleto(NivelEducativo.BASICA_SECUNDARIA, 9, 16));
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, true);
        op.getPrograma().setNombre("Enfermería");
        op.getPrograma().setNivel(NivelEducativo.UNIVERSITARIO);
        op.getPrograma().getRequisitos().add(requisitoMedia());
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        assertTrue(ex.getMessage().startsWith("No puedes postularte a este programa"));
        assertTrue(ex.getMessage().contains("MEDIA"), ex.getMessage());
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Grado 9° SÍ puede postularse a un técnico (su siguiente paso), aunque no sea de sus intereses")
    void noventoGradoPuedeTecnico() {
        beneficiario.setPerfil(perfilCompleto(NivelEducativo.BASICA_SECUNDARIA, 9, 16));
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, true);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));
        when(postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(eq(BENEFICIARIO_ID),
                eq(OPORTUNIDAD_ID), anyList())).thenReturn(false);
        when(postulacionRepository.save(any(Postulacion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        verify(postulacionRepository).save(any(Postulacion.class));
    }

    @Test
    @DisplayName("Rechaza si la edad está fuera del rango exigido")
    void rechazaEdadFueraDeRango() {
        beneficiario.setPerfil(perfilCompleto(NivelEducativo.MEDIA, 11, 45));
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, true);
        Requisito edad = new Requisito();
        edad.setId(21L);
        edad.setNombre("Edad entre 15 y 28 años");
        edad.setTipo(TipoRequisito.EDAD);
        edad.setValorEsperado("15-28");
        op.getPrograma().getRequisitos().add(edad);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));

        assertThrows(ReglaNegocioException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        verify(postulacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin caracterización completa no puede postularse")
    void rechazaPerfilIncompleto() {
        beneficiario.getPerfil().setCompleto(false);
        OportunidadEducativa op = oportunidad(LocalDate.now().plusDays(20), 30, true);
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.of(op));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
        assertTrue(ex.getMessage().contains("caracterización"));
    }

    @Test
    @DisplayName("Elegibilidad: marca Enfermería como no disponible con el motivo, y el técnico como disponible")
    void elegibilidadPorConvocatoria() {
        beneficiario.setPerfil(perfilCompleto(NivelEducativo.BASICA_SECUNDARIA, 9, 16));
        OportunidadEducativa tecnico = oportunidad(LocalDate.now().plusDays(20), 30, true);
        OportunidadEducativa enfermeria = oportunidad(LocalDate.now().plusDays(20), 30, true);
        enfermeria.setId(5L);
        enfermeria.getPrograma().setNivel(NivelEducativo.UNIVERSITARIO);
        enfermeria.getPrograma().getRequisitos().add(requisitoMedia());
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findVigentes(any(LocalDate.class))).thenReturn(List.of(tecnico, enfermeria));

        var lista = service.elegibilidad(BENEFICIARIO_ID);

        assertTrue(lista.get(0).puedePostularse());
        assertNull(lista.get(0).motivo());
        assertFalse(lista.get(1).puedePostularse());
        assertNotNull(lista.get(1).motivo());
    }

    @Test
    @DisplayName("Oportunidad inexistente -> 404")
    void oportunidadInexistente() {
        when(beneficiarioRepository.findById(BENEFICIARIO_ID)).thenReturn(Optional.of(beneficiario));
        when(oportunidadRepository.findById(OPORTUNIDAD_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.postularDesdeCatalogo(BENEFICIARIO_ID, OPORTUNIDAD_ID));
    }
}
