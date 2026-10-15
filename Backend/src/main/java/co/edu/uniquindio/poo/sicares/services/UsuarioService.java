package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.AdminDtos.*;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Administrador.gestionarUsuarios() e indicadores (KPIs) del Sprint 2. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final ProgramaEducativoRepository programaRepository;
    private final OportunidadEducativaRepository oportunidadRepository;
    private final RutaEducativaRepository rutaRepository;
    private final PasswordEncoder passwordEncoder;

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

    /** KPIs del documento de definición del proyecto. */
    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas() {
        long totalBeneficiarios = beneficiarioRepository.count();
        long completos = beneficiarioRepository.countByPerfilCompletoTrue();
        long conRuta = rutaRepository.findAll().stream()
                .map(r -> r.getBeneficiario().getId())
                .distinct()
                .count();
        double porcentaje = totalBeneficiarios == 0 ? 0 : Math.round(completos * 1000.0 / totalBeneficiarios) / 10.0;
        return new EstadisticasResponse(totalBeneficiarios, completos, porcentaje, conRuta,
                programaRepository.count(),
                oportunidadRepository.findVigentes(LocalDate.now()).size());
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
