package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.RutaDtos.RecomendacionResponse;
import co.edu.uniquindio.poo.sicares.dto.RutaDtos.RutaResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.models.enums.Modalidad;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.models.enums.TipoInstitucion;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.OportunidadEducativaRepository;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.RutaEducativaService;
import co.edu.uniquindio.poo.sicares.services.motorReglas.MotorReglas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import co.edu.uniquindio.poo.sicares.services.motorReglas.HuellaIntegridad;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de RutaEducativaService — Proceso 2: HU-18 Generar ruta y HU-19 Consultar rutas.
 * Casos de TestRail relacionados: CP-14, CP-15 y CP-16.
 * Dueño: Integrante 2.
 *
 * El motor de reglas se simula (mock) para probar solo la lógica del servicio;
 * el motor tiene sus propias pruebas (Integrante 3).
 */
@ExtendWith(MockitoExtension.class)
class RutaEducativaServiceTest {

    private static final Long ID = 1L;

    @Mock
    private RutaEducativaRepository rutaRepository;
    @Mock
    private BeneficiarioRepository beneficiarioRepository;
    @Mock
    private MotorReglas motorReglas;

    private RutaEducativaService service;
    private Beneficiario beneficiario;

    @BeforeEach
    void setUp() {
        service = new RutaEducativaService(rutaRepository, beneficiarioRepository, motorReglas);
        beneficiario = new Beneficiario();
        beneficiario.setId(ID);
        beneficiario.setPerfil(new PerfilEducativo());
    }

    // ------------------------------------------------------------------ datos de prueba

    private static OportunidadEducativa oportunidad(long id, String programa) {
        Institucion inst = new Institucion();
        inst.setId(1L);
        inst.setNombre("SENA Regional Quindío");
        inst.setTipo(TipoInstitucion.PUBLICA);
        inst.setDepartamento("Quindío");

        ProgramaEducativo p = new ProgramaEducativo();
        p.setId(id);
        p.setNombre(programa);
        p.setNivel(NivelEducativo.TECNOLOGICO);
        p.setModalidad(Modalidad.PRESENCIAL);
        p.setAreaConocimiento("Tecnología e informática");
        p.setMunicipio("Armenia");
        p.setCostoPeriodo(BigDecimal.ZERO);
        p.setInstitucion(inst);

        OportunidadEducativa o = new OportunidadEducativa();
        o.setId(id);
        o.setPrograma(p);
        o.setPeriodoAcademico("2027-1");
        o.setFechaAperturaInscripcion(LocalDate.now().minusDays(1));
        o.setFechaCierreInscripcion(LocalDate.now().plusDays(20));
        o.setCupos(30);
        return o;
    }

    /** Ruta como la devolvería el motor: con recomendaciones, sin guardar. */
    private static RutaEducativa rutaDelMotor(double... puntajes) {
        RutaEducativa ruta = new RutaEducativa();
        for (int i = 0; i < puntajes.length; i++) {
            Recomendacion rec = new Recomendacion();
            rec.setOportunidad(oportunidad(i + 1, "Programa " + (i + 1)));
            rec.setPuntajeCompatibilidad(puntajes[i]);
            rec.setJustificacion("Justificación de prueba");
            ruta.agregarRecomendacion(rec);
        }
        ruta.ordenarPorCompatibilidad();
        return ruta;
    }

    private void guardarDevuelveLaMisma() {
        when(rutaRepository.save(any(RutaEducativa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ HU-18 Generar ruta

    @Test
    @DisplayName("CP-14: genera la primera ruta (versión 1, VIGENTE) con oportunidades ordenadas")
    void generaPrimeraRuta() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(rutaDelMotor(62.5, 91.0));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.empty());
        guardarDevuelveLaMisma();

        RutaResponse r = service.solicitarRuta(ID);

        assertEquals(1, r.version());
        assertEquals(EstadoRuta.VIGENTE, r.estado());
        assertNotNull(r.fechaGeneracion());
        assertEquals(2, r.recomendaciones().size());
        RecomendacionResponse primera = r.recomendaciones().get(0);
        assertEquals(1, primera.orden());
        assertEquals(91.0, primera.puntajeCompatibilidad(), "La más compatible va primero");
    }

    @Test
    @DisplayName("CP-14: la ruta no contiene oportunidades repetidas")
    void rutaSinDuplicados() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(rutaDelMotor(80, 70, 60));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.empty());
        guardarDevuelveLaMisma();

        RutaResponse r = service.solicitarRuta(ID);

        long distintas = r.recomendaciones().stream().map(rec -> rec.oportunidad().id()).distinct().count();
        assertEquals(r.recomendaciones().size(), distintas);
    }

    @Test
    @DisplayName("HU-18: una nueva ruta sube la versión y desactualiza la ruta anterior")
    void nuevaRutaDesactualizaLaAnterior() {
        RutaEducativa anterior = new RutaEducativa();
        anterior.setVersion(2);
        anterior.setEstado(EstadoRuta.VIGENTE);

        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(rutaDelMotor(75));
        when(rutaRepository.findByBeneficiarioIdAndEstadoNot(ID, EstadoRuta.DESACTUALIZADA)).thenReturn(List.of(anterior));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.of(anterior));
        guardarDevuelveLaMisma();

        RutaResponse r = service.solicitarRuta(ID);

        assertEquals(3, r.version());
        assertEquals(EstadoRuta.DESACTUALIZADA, anterior.getEstado());
    }

    @Test
    @DisplayName("HU-18: si no hay oportunidades compatibles no guarda la ruta y avisa al usuario")
    void sinOportunidadesCompatibles() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(new RutaEducativa());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.solicitarRuta(ID));

        assertTrue(ex.getMessage().contains("no hay oportunidades vigentes"));
        verify(rutaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-16: con el perfil incompleto no se genera la ruta y se informa al usuario")
    void perfilIncompletoNoGeneraRuta() {
        // Motor real (sin reglas) para comprobar el mensaje que ve el usuario
        OportunidadEducativaRepository oportunidades = mock(OportunidadEducativaRepository.class);
        RutaEducativaService conMotorReal =
                new RutaEducativaService(rutaRepository, beneficiarioRepository, new MotorReglas(List.of(), oportunidades));
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario)); // perfil vacío (0 %)

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> conMotorReal.solicitarRuta(ID));

        assertTrue(ex.getMessage().contains("Completa tu caracterización"));
        verify(rutaRepository, never()).save(any());
        verifyNoInteractions(oportunidades);
    }

    @Test
    @DisplayName("HU-18: un beneficiario que no existe lanza 'no encontrado'")
    void beneficiarioInexistente() {
        when(beneficiarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.solicitarRuta(99L));
        verifyNoInteractions(motorReglas);
    }

    // ------------------------------------------------------------------ HU-19 Consultar rutas

    @Test
    @DisplayName("CP-15: la ruta consultada trae institución, programa, requisitos y beneficios")
    void consultaRutaConDetalle() {
        RutaEducativa guardada = rutaDelMotor(88);
        guardada.setId(7L);
        guardada.setVersion(1);
        guardada.setEstado(EstadoRuta.VIGENTE);
        ProgramaEducativo programa = guardada.getRecomendaciones().get(0).getOportunidad().getPrograma();

        Requisito requisito = new Requisito();
        requisito.setId(1L);
        requisito.setNombre("Título de bachiller");
        programa.getRequisitos().add(requisito);

        Beneficio beca = new Beneficio();
        beca.setId(1L);
        beca.setNombre("Beca parcial");
        beca.setPorcentajeCobertura(50);
        beca.setInstitucion(programa.getInstitucion());
        programa.getBeneficios().add(beca);

        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.of(guardada));

        RecomendacionResponse rec = service.consultarActual(ID).orElseThrow().recomendaciones().get(0);

        assertEquals("SENA Regional Quindío", rec.programa().institucionNombre());
        assertEquals("Programa 1", rec.programa().nombre());
        assertEquals("Título de bachiller", rec.programa().requisitos().get(0).nombre());
        assertEquals("Beca parcial", rec.beneficiosAplicables().get(0).nombre());
    }

    @Test
    @DisplayName("HU-19: si el usuario no tiene rutas, la consulta viene vacía (sin error)")
    void consultaSinRutas() {
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.empty());

        assertTrue(service.consultarActual(ID).isEmpty());
    }

    @Test
    @DisplayName("HU-19: el historial devuelve todas las versiones")
    void historial() {
        RutaEducativa v2 = rutaDelMotor(70);
        v2.setVersion(2);
        RutaEducativa v1 = rutaDelMotor(60);
        v1.setVersion(1);
        when(rutaRepository.findByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(List.of(v2, v1));

        List<RutaResponse> historial = service.historial(ID);

        assertEquals(List.of(2, 1), historial.stream().map(RutaResponse::version).toList());
    }

    // ------------------------------------------------------------------ RNF-14 y KPI (revisión de métricas)

    @Test
    @DisplayName("RNF-14 / KPI: al generar la ruta guarda la huella de cada oportunidad y la duración del cálculo")
    void generarGuardaHuellaYDuracion() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(rutaDelMotor(80, 70));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.empty());
        ArgumentCaptor<RutaEducativa> guardada = ArgumentCaptor.forClass(RutaEducativa.class);
        when(rutaRepository.save(guardada.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.solicitarRuta(ID);

        RutaEducativa ruta = guardada.getValue();
        assertNotNull(ruta.getDuracionGeneracionMs());
        ruta.getRecomendaciones().forEach(r -> assertEquals(
                HuellaIntegridad.deOportunidad(r.getOportunidad()), r.getHuellaOferta()));
    }

    @Test
    @DisplayName("RNF-14: si la oferta se altera después de generar la ruta, al consultarla queda DESACTUALIZADA")
    void consultarDetectaAlteracion() {
        RutaEducativa ruta = rutaDelMotor(80);
        ruta.setEstado(EstadoRuta.VIGENTE);
        Recomendacion rec = ruta.getRecomendaciones().get(0);
        rec.setHuellaOferta(HuellaIntegridad.deOportunidad(rec.getOportunidad()));
        rec.getOportunidad().getPrograma().setMunicipio("Calarcá");   // alteración no autorizada simulada
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.of(ruta));

        RutaResponse r = service.consultarActual(ID).orElseThrow();

        assertEquals(EstadoRuta.DESACTUALIZADA, r.estado());
    }

    @Test
    @DisplayName("RNF-14: si la oferta no cambió, la ruta sigue VIGENTE (sin falsos positivos)")
    void consultarSinAlteracion() {
        RutaEducativa ruta = rutaDelMotor(80);
        ruta.setEstado(EstadoRuta.VIGENTE);
        Recomendacion rec = ruta.getRecomendaciones().get(0);
        rec.setHuellaOferta(HuellaIntegridad.deOportunidad(rec.getOportunidad()));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.of(ruta));

        assertEquals(EstadoRuta.VIGENTE, service.consultarActual(ID).orElseThrow().estado());
    }

    // ------------------------------------------------------------------ Ruta automática (al cambiar el perfil)

    @Test
    @DisplayName("Ruta automática: con el perfil incompleto no corre el motor ni lanza error")
    void automaticaPerfilIncompleto() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario)); // perfil 0 %

        assertTrue(service.regenerarAutomaticamente(ID).isEmpty());
        verifyNoInteractions(motorReglas);
        verify(rutaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ruta automática: con el perfil completo genera la versión nueva y avisa si cambiaron las oportunidades")
    void automaticaPerfilCompleto() {
        beneficiario.getPerfil().setCompleto(true);
        RutaEducativa anterior = rutaDelMotor(70);           // oportunidad 1
        anterior.setVersion(1);
        anterior.setEstado(EstadoRuta.DESACTUALIZADA);
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.of(anterior));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(rutaDelMotor(90, 80)); // oportunidades 1 y 2
        guardarDevuelveLaMisma();

        RutaEducativaService.Regeneracion r = service.regenerarAutomaticamente(ID).orElseThrow();

        assertEquals(2, r.ruta().version());
        assertEquals(EstadoRuta.VIGENTE, r.ruta().estado());
        assertTrue(r.cambiaronLasRecomendaciones());
    }

    @Test
    @DisplayName("Ruta automática: sin oferta compatible no lanza error (la ruta anterior queda desactualizada)")
    void automaticaSinOferta() {
        beneficiario.getPerfil().setCompleto(true);
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.empty());
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(new RutaEducativa());

        assertTrue(service.regenerarAutomaticamente(ID).isEmpty());
        verify(rutaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-19: al consultar una ruta desactualizada con el perfil completo se entrega la versión nueva")
    void consultarDesactualizadaRegenera() {
        beneficiario.getPerfil().setCompleto(true);
        RutaEducativa vieja = rutaDelMotor(60);
        vieja.setVersion(1);
        vieja.setEstado(EstadoRuta.DESACTUALIZADA);
        when(rutaRepository.findFirstByBeneficiarioIdOrderByVersionDesc(ID)).thenReturn(Optional.of(vieja));
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
        when(motorReglas.generarRuta(beneficiario.getPerfil())).thenReturn(rutaDelMotor(85));
        guardarDevuelveLaMisma();

        RutaResponse r = service.consultarActual(ID).orElseThrow();

        assertEquals(2, r.version());
        assertEquals(EstadoRuta.VIGENTE, r.estado());
    }
}
