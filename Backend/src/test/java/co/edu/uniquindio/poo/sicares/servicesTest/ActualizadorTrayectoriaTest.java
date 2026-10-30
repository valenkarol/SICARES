package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoEstudio;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.models.enums.NivelEducativo;
import co.edu.uniquindio.poo.sicares.notifications.PostulacionEstadoCambiadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import co.edu.uniquindio.poo.sicares.services.ActualizadorTrayectoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ActualizadorTrayectoria — HU-27 Actualizar trayectoria (retroalimentación).
 * Dueño: Integrante 2.
 */
@ExtendWith(MockitoExtension.class)
class ActualizadorTrayectoriaTest {

    @Mock private ServicioNotificaciones servicioNotificaciones;
    @Mock private RutaEducativaRepository rutaRepository;

    private ActualizadorTrayectoria actualizador;
    private Beneficiario beneficiario;
    private Postulacion postulacion;

    @BeforeEach
    void setUp() {
        actualizador = new ActualizadorTrayectoria(servicioNotificaciones, rutaRepository);

        TrayectoriaEducativa trayectoria = new TrayectoriaEducativa();
        trayectoria.setNivelActual(NivelEducativo.MEDIA);
        PerfilEducativo perfil = new PerfilEducativo();
        perfil.setTrayectoriaEducativa(trayectoria);

        beneficiario = new Beneficiario();
        beneficiario.setId(1L);
        beneficiario.setPerfil(perfil);

        Institucion inst = new Institucion();
        inst.setNombre("SENA Regional Quindío");
        ProgramaEducativo programa = new ProgramaEducativo();
        programa.setNombre("Técnico en Sistemas");
        programa.setNivel(NivelEducativo.TECNICO);
        programa.setInstitucion(inst);
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPrograma(programa);

        postulacion = new Postulacion();
        postulacion.setBeneficiario(beneficiario);
        postulacion.setOportunidad(o);
    }

    @Test
    @DisplayName("HU-27: un cambio de estado normal solo notifica al beneficiario")
    void cambioNormalSoloNotifica() {
        actualizador.onEstadoCambiado(new PostulacionEstadoCambiadoEvent(postulacion,
                EstadoPostulacion.POSTULADA, EstadoPostulacion.ADMITIDA, "Cumple requisitos"));

        verify(servicioNotificaciones).notificar(eq(beneficiario), eq(postulacion), anyString(),
                contains("ADMITIDA"));
        verifyNoInteractions(rutaRepository);
        assertTrue(beneficiario.getPerfil().getTrayectoriaEducativa().getEstudios().isEmpty());
    }

    @Test
    @DisplayName("HU-27: al quedar MATRICULADA agrega el estudio EN_CURSO y marca la ruta como DESACTUALIZADA")
    void matriculaActualizaTrayectoriaYRuta() {
        RutaEducativa vigente = new RutaEducativa();
        vigente.setEstado(EstadoRuta.VIGENTE);
        when(rutaRepository.findByBeneficiarioIdAndEstadoNot(1L, EstadoRuta.DESACTUALIZADA)).thenReturn(List.of(vigente));

        actualizador.onEstadoCambiado(new PostulacionEstadoCambiadoEvent(postulacion,
                EstadoPostulacion.ADMITIDA, EstadoPostulacion.MATRICULADA, null));

        List<EstudioRealizado> estudios = beneficiario.getPerfil().getTrayectoriaEducativa().getEstudios();
        assertEquals(1, estudios.size());
        assertEquals(EstadoEstudio.EN_CURSO, estudios.get(0).getEstado());
        assertEquals("Técnico en Sistemas", estudios.get(0).getTitulo());
        assertEquals(EstadoRuta.DESACTUALIZADA, vigente.getEstado());
        // 1 notificación por el cambio de estado + 1 por la trayectoria actualizada
        verify(servicioNotificaciones, times(2)).notificar(eq(beneficiario), any(), anyString(), anyString());
    }
}
