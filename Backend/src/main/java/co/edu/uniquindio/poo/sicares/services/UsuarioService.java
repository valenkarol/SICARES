package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.AdminDtos.*;
import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.NotificacionResponse;
import co.edu.uniquindio.poo.sicares.dto.PostulacionMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Administrador.gestionarUsuarios(), indicadores (KPIs) y bandeja de notificaciones. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final ProgramaEducativoRepository programaRepository;
    private final OportunidadEducativaRepository oportunidadRepository;
    private final RutaEducativaRepository rutaRepository;
    private final PostulacionRepository postulacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecordatorioRepository recordatorioRepository;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("fechaRegistro").descending()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long adminActualId, Long usuarioId, boolean activo) {
        if (adminActualId.equals(usuarioId) && !activo) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
        }
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));
        u.setActivo(activo);
        return toResponse(u);
    }

    @Transactional
    public UsuarioResponse crearAdministrador(CrearAdministradorRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese correo");
        }
        Administrador a = new Administrador();
        a.setCorreo(correo);
        a.setContrasenaHash(passwordEncoder.encode(req.contrasena()));
        return toResponse(usuarioRepository.save(a));
    }

    /** KPIs del documento de definición del proyecto (procesos 1, 2 y 3). */
    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas() {
        long totalBeneficiarios = beneficiarioRepository.count();
        long completos = beneficiarioRepository.countByPerfilCompletoTrue();
        List<Postulacion> postulaciones = postulacionRepository.findAll();
        Map<EstadoPostulacion, Long> porEstado = new EnumMap<>(EstadoPostulacion.class);
        for (EstadoPostulacion e : EstadoPostulacion.values()) porEstado.put(e, 0L);
        postulaciones.stream()
                .collect(Collectors.groupingBy(Postulacion::getEstado, Collectors.counting()))
                .forEach(porEstado::put);
        List<RutaEducativa> rutas = rutaRepository.findAll();
        long conRuta = rutas.stream()
                .filter(r -> r.getBeneficiario() != null)
                .map(r -> r.getBeneficiario().getId())
                .distinct()
                .count();

        // ---------- Proceso 1: Caracterización ----------
        List<PerfilEducativo> perfiles = beneficiarioRepository.findAll().stream()
                .map(Beneficiario::getPerfil)
                .filter(Objects::nonNull)
                .toList();
        long perfilesIniciados = perfiles.size();
        double minutosCaracterizacion = perfiles.stream()
                .filter(pe -> pe.getFechaCreacion() != null && pe.getFechaCompletado() != null)
                .mapToLong(pe -> Duration.between(pe.getFechaCreacion(), pe.getFechaCompletado()).toMinutes())
                .average().orElse(0);

        // ---------- Proceso 2: Rutas educativas ----------
        double msGeneracionRuta = rutas.stream()
                .filter(r -> r.getDuracionGeneracionMs() != null)
                .mapToLong(RutaEducativa::getDuracionGeneracionMs)
                .average().orElse(0);
        long conPostulacion = postulaciones.stream()
                .filter(p -> p.getBeneficiario() != null)
                .map(p -> p.getBeneficiario().getId())
                .distinct()
                .count();

        // ---------- Proceso 3: Postulación y seguimiento ----------
        long completadas = postulaciones.stream().filter(p -> p.getFechaPostulacion() != null).count();
        double horasSeleccionAPostulacion = postulaciones.stream()
                .filter(p -> p.getFechaSeleccion() != null && p.getFechaPostulacion() != null)
                .mapToLong(p -> Duration.between(p.getFechaSeleccion(), p.getFechaPostulacion()).toMinutes())
                .average().orElse(0) / 60.0;
        // Un recordatorio de cierre se "atiende a tiempo" si el usuario registró la postulación a más tardar ese día
        List<Recordatorio> cierres = recordatorioRepository.findAll().stream()
                .filter(r -> r.isEnviado() && r.getTitulo() != null && r.getTitulo().startsWith("Cierre"))
                .toList();
        long atendidos = cierres.stream()
                .filter(r -> r.getPostulacion() != null && r.getPostulacion().getFechaPostulacion() != null
                        && r.getFechaEvento() != null
                        && !r.getPostulacion().getFechaPostulacion().toLocalDate().isAfter(r.getFechaEvento()))
                .count();

        return new EstadisticasResponse(totalBeneficiarios, completos, porcentaje(completos, totalBeneficiarios),
                conRuta, postulaciones.size(), porEstado, programaRepository.count(),
                oportunidadRepository.findVigentes(LocalDate.now()).size(),
                porcentaje(perfilesIniciados - Math.min(completos, perfilesIniciados), perfilesIniciados),
                redondear(minutosCaracterizacion),
                porcentaje(conRuta, totalBeneficiarios),
                redondear(msGeneracionRuta),
                porcentaje(conPostulacion, conRuta),
                porcentaje(completadas, postulaciones.size()),
                redondear(horasSeleccionAPostulacion),
                porcentaje(atendidos, cierres.size()));
    }

    private static double porcentaje(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 1000.0 / total) / 10.0;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    // ------------------------------------------------ notificaciones del usuario autenticado (HU-26)

    @Transactional(readOnly = true)
    public List<NotificacionResponse> notificaciones(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaEnvioDesc(usuarioId).stream()
                .map(PostulacionMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long notificacionesNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Transactional
    public NotificacionResponse marcarLeida(Long usuarioId, Long notificacionId) {
        Notificacion n = notificacionRepository.findById(notificacionId)
                .filter(x -> x.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación", notificacionId));
        n.marcarLeida();
        return PostulacionMapper.toDto(n);
    }

    private UsuarioResponse toResponse(Usuario u) {
        String nombre = null;
        Integer porcentaje = null;
        if (u instanceof Beneficiario b && b.getPerfil() != null) {
            porcentaje = b.getPerfil().getPorcentajeCompletitud();
            InformacionPersonal info = b.getPerfil().getInformacionPersonal();
            if (info != null) nombre = info.getNombres() + " " + info.getApellidos();
        } else if (u instanceof Beneficiario) {
            porcentaje = 0;
        }
        return new UsuarioResponse(u.getId(), u.getCorreo(), u.getRol(), u.isActivo(), u.getFechaRegistro(),
                u.getUltimoAcceso(), nombre, porcentaje);
    }
}
