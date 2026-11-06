package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.CaracterizacionDtos.*;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.AutorizacionDatos;
import co.edu.uniquindio.poo.sicares.models.entities.Beneficiario;
import co.edu.uniquindio.poo.sicares.models.entities.InformacionPersonal;
import co.edu.uniquindio.poo.sicares.models.entities.ResponsableMenor;
import co.edu.uniquindio.poo.sicares.models.entities.RutaEducativa;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import co.edu.uniquindio.poo.sicares.notifications.PerfilActualizadoEvent;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.CaracterizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de CaracterizacionService — Proceso 1 (HU-04 a HU-11).
 * Casos de TestRail relacionados: CP-06, CP-07, CP-08 y CP-09.
 */
@ExtendWith(MockitoExtension.class)
class CaracterizacionServiceTest {

    private static final Long ID = 1L;

    @Mock
    private BeneficiarioRepository beneficiarioRepository;
    @Mock
    private RutaEducativaRepository rutaRepository;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private CaracterizacionService service;

    private Beneficiario beneficiario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "versionPolitica", "1.0");

        beneficiario = new Beneficiario();
        beneficiario.setId(ID);
        beneficiario.setCorreo("usuario.prueba01@correo.com");
        AutorizacionDatos autorizacion = new AutorizacionDatos();
        autorizacion.aceptar();
        beneficiario.setAutorizacion(autorizacion);
    }

    // ---- datos de prueba

    private void existeBeneficiario() {
        when(beneficiarioRepository.findById(ID)).thenReturn(Optional.of(beneficiario));
    }

    private static LocalDate nacidoHace(int anios) {
        return LocalDate.now().minusYears(anios);
    }

    private static InformacionPersonalDto info(LocalDate nacimiento, TipoDocumento tipo, ResponsableMenorDto responsable) {
        return new InformacionPersonalDto(" Usuario ", " Prueba ", tipo, "1094000000", nacimiento,
                "Femenino", "", "3001234567", "Quindío", "Armenia", Zona.URBANA, null, responsable);
    }

    private static InformacionPersonalDto adulto(int edad) {
        return info(nacidoHace(edad), TipoDocumento.CC, null);
    }

    private static ResponsableMenorDto responsable(int edad, TipoDocumento tipo, String documento) {
        return new ResponsableMenorDto(" María ", " Prueba ", tipo, documento, nacidoHace(edad),
                "Madre", "3109876543", "");
    }

    private static ResponsableMenorDto responsableValido() {
        return responsable(40, TipoDocumento.CC, "41900000");
    }

    //  HU-04 Información personal

    @Nested
    @DisplayName("HU-04 Información personal")
    class InformacionPersonalTest {

        @Test
        @DisplayName("CP-06: guarda la información de un adulto de 25 años")
        void guardaAdultoValido() {
            existeBeneficiario();

            PerfilResponse r = service.guardarInformacionPersonal(ID, adulto(25));

            assertEquals("Usuario", r.informacionPersonal().nombres(), "Los textos se guardan sin espacios sobrantes");
            assertEquals(25, r.informacionPersonal().edad());
            assertNull(r.informacionPersonal().responsable(), "Un adulto no necesita responsable");
            assertEquals(16, r.porcentajeCompletitud(), "1 de 6 secciones = 16 %");
            verify(beneficiarioRepository).save(beneficiario);
        }

        @ParameterizedTest(name = "edad {0}")
        @ValueSource(ints = {11, 60})
        @DisplayName("CP-07: rechaza edades fuera del rango 12 a 59 y no guarda")
        void rechazaEdadFueraDeRango(int edad) {
            TipoDocumento tipo = edad < 18 ? TipoDocumento.TI : TipoDocumento.CC;

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, info(nacidoHace(edad), tipo, responsableValido())));

            assertEquals("SICARES atiende personas entre 12 y 59 años", ex.getMessage());
            verify(beneficiarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("CP-07: acepta el límite inferior (12 años, con TI y responsable)")
        void aceptaLimiteInferior() {
            existeBeneficiario();

            PerfilResponse r = service.guardarInformacionPersonal(ID,
                    info(nacidoHace(12), TipoDocumento.TI, responsableValido()));

            assertEquals(12, r.informacionPersonal().edad());
        }

        @Test
        @DisplayName("CP-07: acepta el límite superior (59 años)")
        void aceptaLimiteSuperior() {
            existeBeneficiario();

            PerfilResponse r = service.guardarInformacionPersonal(ID, adulto(59));

            assertEquals(59, r.informacionPersonal().edad());
        }

        @Test
        @DisplayName("HU-04: un menor de edad no puede registrarse con cédula")
        void menorConCedula() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, info(nacidoHace(15), TipoDocumento.CC, responsableValido())));

            assertTrue(ex.getMessage().contains("Tarjeta de Identidad"));
        }

        @Test
        @DisplayName("HU-04: un mayor de edad no puede registrarse con tarjeta de identidad")
        void mayorConTarjeta() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, info(nacidoHace(30), TipoDocumento.TI, null)));

            assertTrue(ex.getMessage().contains("Cédula de Ciudadanía"));
        }

        @Test
        @DisplayName("HU-04: sin autorización de tratamiento de datos no se guarda")
        void sinAutorizacion() {
            beneficiario.setAutorizacion(null);
            existeBeneficiario();

            assertThrows(ReglaNegocioException.class, () -> service.guardarInformacionPersonal(ID, adulto(25)));
            verify(beneficiarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("HU-04: un beneficiario que no existe lanza 'no encontrado'")
        void beneficiarioInexistente() {
            when(beneficiarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class, () -> service.guardarInformacionPersonal(99L, adulto(25)));
        }
    }

    //  Responsable de menores

    @Nested
    @DisplayName("HU-04 Responsable mayor de edad para menores")
    class ResponsableTest {

        @Test
        @DisplayName("Un menor sin responsable no se puede guardar")
        void menorSinResponsable() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, info(nacidoHace(15), TipoDocumento.TI, null)));

            assertTrue(ex.getMessage().contains("adulto responsable"));
            verify(beneficiarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("El responsable debe ser mayor de edad")
        void responsableMenorDeEdad() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID,
                            info(nacidoHace(15), TipoDocumento.TI, responsable(17, TipoDocumento.CC, "41900000"))));

            assertTrue(ex.getMessage().contains("mayor de edad"));
        }

        @Test
        @DisplayName("El responsable debe identificarse con cédula")
        void responsableConTarjeta() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID,
                            info(nacidoHace(15), TipoDocumento.TI, responsable(40, TipoDocumento.TI, "41900000"))));

            assertTrue(ex.getMessage().contains("Cédula de Ciudadanía"));
        }

        @Test
        @DisplayName("El documento del responsable no puede ser el del menor")
        void responsableMismoDocumento() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID,
                            info(nacidoHace(15), TipoDocumento.TI, responsable(40, TipoDocumento.CC, "1094000000"))));

            assertTrue(ex.getMessage().contains("no puede ser el mismo"));
        }

        @Test
        @DisplayName("Un menor con responsable válido se guarda con los datos del responsable")
        void menorConResponsableValido() {
            existeBeneficiario();

            PerfilResponse r = service.guardarInformacionPersonal(ID,
                    info(nacidoHace(15), TipoDocumento.TI, responsableValido()));

            ResponsableMenorDto guardado = r.informacionPersonal().responsable();
            assertNotNull(guardado);
            assertEquals("María", guardado.nombres());
            assertEquals("Madre", guardado.parentesco());
            assertNull(guardado.correo(), "Un correo vacío se guarda como null");

            ResponsableMenor entidad = beneficiario.getPerfil().getInformacionPersonal().getResponsable();
            assertTrue(entidad.esMayorDeEdad());
        }

        @Test
        @DisplayName("Si un adulto envía responsable, se ignora")
        void adultoIgnoraResponsable() {
            existeBeneficiario();

            PerfilResponse r = service.guardarInformacionPersonal(ID,
                    info(nacidoHace(30), TipoDocumento.CC, responsableValido()));

            assertNull(r.informacionPersonal().responsable());
        }
    }

    //  HU-05 a HU-09 (CP-08)

    @Nested
    @DisplayName("CP-08 Víctima, socioeconómica, trayectoria, intereses y accesibilidad")
    class SeccionesTest {

        @Test
        @DisplayName("HU-05: no puede estar inscrito en el RUV sin marcar que es víctima")
        void ruvSinSerVictima() {
            assertThrows(ReglaNegocioException.class, () -> service.guardarSituacionVictima(ID,
                    new SituacionVictimaDto(false, true, null, null, null, null)));
        }

        @Test
        @DisplayName("HU-05: si es víctima, el hecho victimizante es obligatorio")
        void victimaSinHecho() {
            assertThrows(ReglaNegocioException.class, () -> service.guardarSituacionVictima(ID,
                    new SituacionVictimaDto(true, true, " ", null, null, null)));
        }

        @Test
        @DisplayName("HU-05: si no es víctima, se borran los datos del hecho")
        void noVictimaLimpiaDatos() {
            existeBeneficiario();

            PerfilResponse r = service.guardarSituacionVictima(ID,
                    new SituacionVictimaDto(false, false, "Desplazamiento forzado", LocalDate.of(2015, 1, 1), "Quindío", "Armenia"));

            assertNull(r.situacionVictima().hechoVictimizante());
            assertNull(r.situacionVictima().municipioExpulsor());
        }

        @Test
        @DisplayName("HU-06: las personas a cargo no pueden superar a los integrantes del hogar")
        void personasACargoMayores() {
            assertThrows(ReglaNegocioException.class, () -> service.guardarSituacionSocioeconomica(ID,
                    new SituacionSocioeconomicaDto("A3", 1, new BigDecimal("1300000"), "Desempleado", 2, 3)));
        }

        @Test
        @DisplayName("HU-07: el año del último estudio no puede ser futuro")
        void anioFuturo() {
            int siguiente = LocalDate.now().getYear() + 1;
            assertThrows(ReglaNegocioException.class, () -> service.guardarTrayectoria(ID,
                    new TrayectoriaEducativaDto(NivelEducativo.MEDIA, 11, siguiente, List.of())));
        }

        @Test
        @DisplayName("HU-07: en un estudio el año de fin no puede ser menor al de inicio")
        void estudioConAniosInvertidos() {
            EstudioRealizadoDto estudio = new EstudioRealizadoDto(null, NivelEducativo.MEDIA, "Colegio",
                    "Bachiller", EstadoEstudio.COMPLETO, 2020, 2018);
            assertThrows(ReglaNegocioException.class, () -> service.guardarTrayectoria(ID,
                    new TrayectoriaEducativaDto(NivelEducativo.MEDIA, 11, 2020, List.of(estudio))));
        }

        @Test
        @DisplayName("HU-08: máximo 5 áreas de interés")
        void masDeCincoIntereses() {
            List<InteresProfesionalDto> seis = List.of("Salud", "Arte", "Deporte", "Idiomas", "Agro", "Turismo")
                    .stream().map(a -> new InteresProfesionalDto(a, null)).toList();

            assertThrows(ReglaNegocioException.class,
                    () -> service.guardarIntereses(ID, new InteresesRequest(seis)));
        }

        @Test
        @DisplayName("HU-08: los intereses repetidos (sin importar mayúsculas) se guardan una sola vez")
        void interesesSinDuplicados() {
            existeBeneficiario();

            PerfilResponse r = service.guardarIntereses(ID, new InteresesRequest(List.of(
                    new InteresProfesionalDto("Salud", null),
                    new InteresProfesionalDto(" salud ", null),
                    new InteresProfesionalDto("Tecnología e informática", null))));

            assertEquals(2, r.intereses().size());
        }

        @Test
        @DisplayName("CP-08: al completar las 6 secciones el perfil queda completo al 100 %")
        void perfilCompleto() {
            existeBeneficiario();

            service.guardarInformacionPersonal(ID, adulto(25));
            service.guardarSituacionVictima(ID,
                    new SituacionVictimaDto(true, true, "Desplazamiento forzado", LocalDate.of(2015, 1, 1), "Quindío", "Armenia"));
            service.guardarSituacionSocioeconomica(ID,
                    new SituacionSocioeconomicaDto("A3", 1, new BigDecimal("1300000"), "Desempleado", 4, 1));
            service.guardarTrayectoria(ID, new TrayectoriaEducativaDto(NivelEducativo.MEDIA, 11, 2020, List.of()));
            service.guardarIntereses(ID, new InteresesRequest(List.of(new InteresProfesionalDto("Salud", null))));
            PerfilResponse r = service.guardarCondicionAccesibilidad(ID,
                    new CondicionAccesibilidadDto(true, true, "PUBLICO", 10, Modalidad.PRESENCIAL));

            assertEquals(100, r.porcentajeCompletitud());
            assertTrue(r.completo());
        }
    }

    //  HU-10 / HU-11 (CP-09)

    @Nested
    @DisplayName("CP-09 Consulta y actualización del perfil")
    class ActualizacionTest {

        @Test
        @DisplayName("HU-10: consulta el perfil de un beneficiario sin caracterización")
        void consultaPerfilVacio() {
            existeBeneficiario();

            PerfilResponse r = service.consultarPerfil(ID);

            assertEquals("usuario.prueba01@correo.com", r.correo());
            assertEquals(0, r.porcentajeCompletitud());
            assertTrue(r.autorizacionAceptada());
        }

        @Test
        @DisplayName("HU-11: al editar el perfil, las rutas vigentes quedan desactualizadas")
        void editarDesactualizaRutas() {
            existeBeneficiario();
            RutaEducativa vigente = new RutaEducativa();
            vigente.setEstado(EstadoRuta.VIGENTE);
            when(rutaRepository.findByBeneficiarioIdAndEstadoNot(ID, EstadoRuta.DESACTUALIZADA))
                    .thenReturn(List.of(vigente));

            service.guardarTrayectoria(ID, new TrayectoriaEducativaDto(NivelEducativo.TECNICO, 11, 2023, List.of()));

            assertEquals(EstadoRuta.DESACTUALIZADA, vigente.getEstado());
        }

        @Test
        @DisplayName("CP-09: actualizar la edad a 70 se rechaza y se conservan los datos anteriores")
        void actualizarEdadInvalidaConservaDatos() {
            existeBeneficiario();
            service.guardarInformacionPersonal(ID, adulto(25));
            InformacionPersonal guardada = beneficiario.getPerfil().getInformacionPersonal();
            LocalDate fechaAnterior = guardada.getFechaNacimiento();

            assertThrows(ReglaNegocioException.class, () -> service.guardarInformacionPersonal(ID, adulto(70)));

            assertEquals(fechaAnterior, guardada.getFechaNacimiento());
            assertEquals(25, guardada.calcularEdad());
        }

        @Test
        @DisplayName("HU-11: aceptar y revocar la autorización de datos")
        void aceptarYRevocarAutorizacion() {
            beneficiario.setAutorizacion(null);
            existeBeneficiario();

            assertTrue(service.aceptarAutorizacion(ID).autorizacionAceptada());
            assertEquals("1.0", beneficiario.getAutorizacion().getVersionPolitica());
            assertFalse(service.revocarAutorizacion(ID).autorizacionAceptada());
        }
    }

    //  RNF-13, RNF-01 y RNF-16 (revisión de métricas)

    @Nested
    @DisplayName("RNF-13 Identificación única y RNF-01/16 consistencia del perfil")
    class MetricasCalidadTest {

        @Test
        @DisplayName("RNF-13: no permite caracterizar a otra cuenta con un número de documento ya registrado")
        void documentoDuplicadoRechazado() {
            when(beneficiarioRepository.existsByPerfilInformacionPersonalNumeroDocumentoAndIdNot("1094000000", ID))
                    .thenReturn(true);

            assertThrows(ReglaNegocioException.class, () -> service.guardarInformacionPersonal(ID, adulto(25)));
            verify(beneficiarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("RNF-01: nivel MEDIA con último grado 7 es una contradicción y no se guarda")
        void nivelYGradoIncoherentes() {
            existeBeneficiario();

            assertThrows(ReglaNegocioException.class, () -> service.guardarTrayectoria(ID,
                    new TrayectoriaEducativaDto(NivelEducativo.MEDIA, 7, 2020, List.of())));
            verify(beneficiarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("RNF-01: un estudio UNIVERSITARIO completo con nivel actual MEDIA es una contradicción")
        void estudioCompletoSuperiorAlNivel() {
            existeBeneficiario();
            EstudioRealizadoDto universidad = new EstudioRealizadoDto(null, NivelEducativo.UNIVERSITARIO,
                    "Universidad del Quindío", "Ingeniería", EstadoEstudio.COMPLETO, 2015, 2020);

            assertThrows(ReglaNegocioException.class, () -> service.guardarTrayectoria(ID,
                    new TrayectoriaEducativaDto(NivelEducativo.MEDIA, 11, 2020, List.of(universidad))));
        }

        @Test
        @DisplayName("RNF-01: preferir modalidad virtual sin internet es una contradicción")
        void virtualSinInternet() {
            existeBeneficiario();

            assertThrows(ReglaNegocioException.class, () -> service.guardarCondicionAccesibilidad(ID,
                    new CondicionAccesibilidadDto(false, true, "PUBLICO", 10, Modalidad.VIRTUAL)));
        }

        @Test
        @DisplayName("RNF-16: modificar otra sección no puede dejar el perfil contradictorio")
        void modificacionPosteriorNoGeneraContradiccion() {
            existeBeneficiario();
            // Trayectoria guardada primero: último estudio en 1995
            service.guardarTrayectoria(ID, new TrayectoriaEducativaDto(NivelEducativo.MEDIA, 11, 1995, List.of()));

            // Después registra que tiene 25 años (nació ~2001): habría estudiado antes de nacer => se rechaza
            assertThrows(ReglaNegocioException.class, () -> service.guardarInformacionPersonal(ID, adulto(25)));
        }

        @Test
        @DisplayName("RNF-01: el hecho victimizante no puede ser anterior a la fecha de nacimiento")
        void hechoAntesDeNacer() {
            existeBeneficiario();
            service.guardarInformacionPersonal(ID, adulto(25));

            assertThrows(ReglaNegocioException.class, () -> service.guardarSituacionVictima(ID,
                    new SituacionVictimaDto(true, true, "Desplazamiento forzado",
                            LocalDate.now().minusYears(30), "Caquetá", "Florencia")));
        }

        @Test
        @DisplayName("RNF-01: un perfil coherente en sus 6 secciones queda completo")
        void perfilCoherenteQuedaCompleto() {
            existeBeneficiario();

            service.guardarInformacionPersonal(ID, adulto(30));
            service.guardarSituacionVictima(ID,
                    new SituacionVictimaDto(true, true, "Desplazamiento forzado", LocalDate.of(2015, 1, 1), "Quindío", "Armenia"));
            service.guardarSituacionSocioeconomica(ID,
                    new SituacionSocioeconomicaDto("A3", 1, new BigDecimal("1300000"), "Desempleado", 4, 1));
            service.guardarTrayectoria(ID, new TrayectoriaEducativaDto(NivelEducativo.BASICA_SECUNDARIA, 9, 2012, List.of()));
            service.guardarIntereses(ID, new InteresesRequest(List.of(new InteresProfesionalDto("Salud", null))));
            PerfilResponse r = service.guardarCondicionAccesibilidad(ID,
                    new CondicionAccesibilidadDto(true, true, "PUBLICO", 10, Modalidad.VIRTUAL));

            assertTrue(r.completo());
            assertNotNull(beneficiario.getPerfil().getFechaCompletado(), "KPI: se guarda cuándo quedó completo");
        }
    }

    //  Correcciones: departamento/municipio, número de documento y ruta automática

    @Nested
    @DisplayName("Ubicación, documento y aviso de perfil actualizado")
    class CorreccionesTest {

        private InformacionPersonalDto conUbicacion(String departamento, String municipio) {
            return new InformacionPersonalDto("Usuario", "Prueba", TipoDocumento.CC, "1094000000", nacidoHace(25),
                    "", "", "3001234567", departamento, municipio, Zona.URBANA, null, null);
        }

        private InformacionPersonalDto conDocumento(TipoDocumento tipo, int edad, String numero) {
            return new InformacionPersonalDto("Usuario", "Prueba", tipo, numero, nacidoHace(edad),
                    "", "", "3001234567", "Quindío", "Armenia", Zona.URBANA, null,
                    edad < 18 ? responsableValido() : null);
        }

        @Test
        @DisplayName("Guarda el departamento y el municipio con el nombre oficial (con tildes)")
        void normalizaUbicacion() {
            existeBeneficiario();

            PerfilResponse r = service.guardarInformacionPersonal(ID, conUbicacion("quindio", "calarca"));

            assertEquals("Quindío", r.informacionPersonal().departamento());
            assertEquals("Calarcá", r.informacionPersonal().municipio());
        }

        @Test
        @DisplayName("Rechaza un municipio que no pertenece al departamento")
        void municipioDeOtroDepartamento() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, conUbicacion("Quindío", "Pereira")));

            assertTrue(ex.getMessage().contains("no pertenece al departamento de Quindío"));
            verify(beneficiarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza un departamento que no existe")
        void departamentoInexistente() {
            assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, conUbicacion("Narnia", "Armenia")));
        }

        @Test
        @DisplayName("Cédula con 11 dígitos: explica que la cédula tiene máximo 10")
        void cedulaMuyLarga() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, conDocumento(TipoDocumento.CC, 25, "10940000001")));

            assertTrue(ex.getMessage().contains("entre 6 y 10 dígitos"));
        }

        @Test
        @DisplayName("Documento con letras: explica que solo se admiten números")
        void documentoConLetras() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarInformacionPersonal(ID, conDocumento(TipoDocumento.CC, 25, "10940A")));

            assertTrue(ex.getMessage().contains("solo puede tener números"));
        }

        @Test
        @DisplayName("Tarjeta de identidad de 11 dígitos es válida")
        void tarjetaIdentidadOnceDigitos() {
            existeBeneficiario();

            assertDoesNotThrow(() -> service.guardarInformacionPersonal(ID,
                    conDocumento(TipoDocumento.TI, 15, "10940000001")));
        }

        @Test
        @DisplayName("Cada cambio publica el evento para actualizar la ruta automáticamente")
        void publicaEventoDePerfil() {
            existeBeneficiario();

            service.guardarInformacionPersonal(ID, adulto(25));

            ArgumentCaptor<PerfilActualizadoEvent> evento = ArgumentCaptor.forClass(PerfilActualizadoEvent.class);
            verify(eventos).publishEvent(evento.capture());
            assertEquals(ID, evento.getValue().beneficiarioId());
            assertFalse(evento.getValue().perfilCompleto(), "Con 1 de 6 secciones el perfil no está completo");
            assertFalse(evento.getValue().completadoAhora());
        }

        @Test
        @DisplayName("HU-05: guarda el departamento y el municipio expulsor con el nombre oficial")
        void lugarExpulsorNormalizado() {
            existeBeneficiario();

            PerfilResponse r = service.guardarSituacionVictima(ID, new SituacionVictimaDto(true, true,
                    "Desplazamiento forzado", LocalDate.of(2015, 1, 1), "caqueta", "san vicente del caguan"));

            assertEquals("Caquetá", r.situacionVictima().departamentoExpulsor());
            assertEquals("San Vicente del Caguán", r.situacionVictima().municipioExpulsor());
        }

        @Test
        @DisplayName("HU-05: rechaza un municipio expulsor que no pertenece al departamento")
        void municipioExpulsorDeOtroDepartamento() {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> service.guardarSituacionVictima(ID, new SituacionVictimaDto(true, true,
                            "Desplazamiento forzado", LocalDate.of(2015, 1, 1), "Quindío", "Florencia")));

            assertTrue(ex.getMessage().contains("no pertenece al departamento de Quindío"));
        }

        @Test
        @DisplayName("HU-05: un municipio expulsor sin departamento no se acepta")
        void municipioExpulsorSinDepartamento() {
            assertThrows(ReglaNegocioException.class,
                    () -> service.guardarSituacionVictima(ID, new SituacionVictimaDto(true, true,
                            "Desplazamiento forzado", LocalDate.of(2015, 1, 1), null, "Armenia")));
        }
    }
}
