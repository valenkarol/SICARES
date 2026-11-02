package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.dto.AdminDtos.EstadisticasResponse;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.repository.*;
import co.edu.uniquindio.poo.sicares.services.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de UsuarioService — KPIs de postulaciones y bandeja de notificaciones (Sprint 3).
 * Dueño: Integrante 1.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private ProgramaEducativoRepository programaRepository;
    @Mock private OportunidadEducativaRepository oportunidadRepository;
    @Mock private RutaEducativaRepository rutaRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RecordatorioRepository recordatorioRepository;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, beneficiarioRepository, programaRepository,
                oportunidadRepository, rutaRepository, postulacionRepository, notificacionRepository, passwordEncoder,
                recordatorioRepository);
    }

    private static Postulacion postulacionEn(EstadoPostulacion estado) {
        Postulacion p = new Postulacion();
        p.setEstado(estado);
        return p;
    }

    @Test
    @DisplayName("KPI: cuenta las postulaciones por estado (incluye los estados en 0)")
    void estadisticasPorEstado() {
        when(beneficiarioRepository.count()).thenReturn(4L);
        when(beneficiarioRepository.countByPerfilCompletoTrue()).thenReturn(2L);
        when(postulacionRepository.findAll()).thenReturn(List.of(
                postulacionEn(EstadoPostulacion.POSTULADA),
                postulacionEn(EstadoPostulacion.POSTULADA),
                postulacionEn(EstadoPostulacion.ADMITIDA)));
        when(rutaRepository.findAll()).thenReturn(List.of());
        when(programaRepository.count()).thenReturn(8L);
        when(oportunidadRepository.findVigentes(any(LocalDate.class))).thenReturn(List.of());

        EstadisticasResponse e = service.estadisticas();

        assertEquals(3, e.totalPostulaciones());
        assertEquals(2L, e.postulacionesPorEstado().get(EstadoPostulacion.POSTULADA));
        assertEquals(1L, e.postulacionesPorEstado().get(EstadoPostulacion.ADMITIDA));
        assertEquals(0L, e.postulacionesPorEstado().get(EstadoPostulacion.MATRICULADA));
        assertEquals(EstadoPostulacion.values().length, e.postulacionesPorEstado().size());
        assertEquals(50.0, e.porcentajePerfilesCompletos());
    }

    @Test
    @DisplayName("KPIs: postulaciones completadas, rutas seleccionadas y tiempo entre selección y postulación")
    void kpisProcesos2y3() {
        Beneficiario ana = new Beneficiario();
        ana.setId(1L);
        Beneficiario luis = new Beneficiario();
        luis.setId(2L);
        RutaEducativa rutaAna = new RutaEducativa();
        rutaAna.setBeneficiario(ana);
        rutaAna.setDuracionGeneracionMs(200L);
        RutaEducativa rutaLuis = new RutaEducativa();
        rutaLuis.setBeneficiario(luis);
        rutaLuis.setDuracionGeneracionMs(400L);

        Postulacion registrada = postulacionEn(EstadoPostulacion.POSTULADA);
        registrada.setBeneficiario(ana);
        registrada.setFechaSeleccion(LocalDateTime.of(2026, 10, 1, 8, 0));
        registrada.setFechaPostulacion(LocalDateTime.of(2026, 10, 1, 10, 0));
        Postulacion sinRegistrar = postulacionEn(EstadoPostulacion.SELECCIONADA);
        sinRegistrar.setBeneficiario(ana);

        when(beneficiarioRepository.count()).thenReturn(4L);
        when(postulacionRepository.findAll()).thenReturn(List.of(registrada, sinRegistrar));
        when(rutaRepository.findAll()).thenReturn(List.of(rutaAna, rutaLuis));

        EstadisticasResponse e = service.estadisticas();

        assertEquals(50.0, e.porcentajeUsuariosConRuta(), "2 de 4 beneficiarios tienen ruta");
        assertEquals(300.0, e.msPromedioGeneracionRuta());
        assertEquals(50.0, e.porcentajeRutasSeleccionadas(), "1 de 2 usuarios con ruta se postuló");
        assertEquals(50.0, e.porcentajePostulacionesCompletadas(), "1 de 2 postulaciones se registró");
        assertEquals(2.0, e.horasPromedioSeleccionAPostulacion());
    }

    @Test
    @DisplayName("Seguridad: un usuario no puede marcar como leída la notificación de otro")
    void noMarcaNotificacionAjena() {
        Beneficiario otro = new Beneficiario();
        otro.setId(2L);
        Notificacion n = new Notificacion();
        n.setUsuario(otro);
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThrows(RecursoNoEncontradoException.class, () -> service.marcarLeida(1L, 5L));
        assertFalse(n.isLeida());
    }
}