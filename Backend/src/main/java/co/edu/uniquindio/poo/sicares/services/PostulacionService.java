package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.dto.PostulacionMapper;
import co.edu.uniquindio.poo.sicares.dto.PostulacionDtos.*;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoDocumento;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoPostulacion;
import co.edu.uniquindio.poo.sicares.notifications.PostulacionEstadoCambiadoEvent;
import co.edu.uniquindio.poo.sicares.notifications.ServicioNotificaciones;
import co.edu.uniquindio.poo.sicares.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Proceso 3: Postulación y seguimiento (HU-20 a HU-25).
 * Implementa Beneficiario.seleccionarOportunidad(), Beneficiario.consultarPostulaciones()
 * y Administrador.actualizarEstadoPostulacion().
 */
@Service
@RequiredArgsConstructor
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RequisitoRepository requisitoRepository;
    private final DocumentoPostulacionRepository documentoRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final AlmacenamientoService almacenamiento;
    private final ServicioNotificaciones servicioNotificaciones;
    private final ApplicationEventPublisher eventos;

    // ======================================================== Beneficiario

    /** HU-20 seleccionarOportunidad(r: Recomendacion): Postulacion */
    @Transactional
    public PostulacionResponse seleccionarOportunidad(Long beneficiarioId, Long recomendacionId) {
        Beneficiario b = beneficiarioRepository.findById(beneficiarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", beneficiarioId));
        Recomendacion rec = recomendacionRepository.findById(recomendacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recomendación", recomendacionId));

        if (!rec.getRuta().getBeneficiario().getId().equals(beneficiarioId)) {
            throw new ReglaNegocioException("La recomendación no pertenece a tu ruta educativa");
        }
        OportunidadEducativa op = rec.getOportunidad();
        if (!op.estaVigente()) {
            throw new ReglaNegocioException("La convocatoria ya cerró o no tiene cupos");
        }
        boolean yaExiste = postulacionRepository.existsByBeneficiarioIdAndOportunidadIdAndEstadoNotIn(
                beneficiarioId, op.getId(),
                List.of(EstadoPostulacion.CERRADA, EstadoPostulacion.NO_ADMITIDA));
        if (yaExiste) {
            throw new ReglaNegocioException("Ya tienes una postulación activa para esta oportunidad");
        }

        Postulacion p = new Postulacion();
        p.setBeneficiario(b);
        p.setOportunidad(op);
        p.setRecomendacion(rec);
        p.iniciar(b);
        servicioNotificaciones.programarRecordatorios(p); // HU-26
        p = postulacionRepository.save(p);

        servicioNotificaciones.notificar(b, p, "Seleccionaste una oportunidad educativa",
                "Seleccionaste " + op.getPrograma().getNombre() + " (" + op.getPeriodoAcademico() + "). "
                        + "Revisa los requisitos y carga tus documentos antes del " + op.getFechaCierreInscripcion() + ".");
        return PostulacionMapper.toDto(p);
    }

    /** consultarPostulaciones(): List<Postulacion> */
    @Transactional(readOnly = true)
    public List<PostulacionResponse> consultarPostulaciones(Long beneficiarioId) {
        return postulacionRepository.findByBeneficiarioIdOrderByFechaSeleccionDesc(beneficiarioId).stream()
                .map(PostulacionMapper::toDto).toList();
    }

    /** HU-21 / HU-24: detalle con requisitos, documentos, historial y recordatorios. */
    @Transactional(readOnly = true)
    public PostulacionResponse obtenerDelBeneficiario(Long beneficiarioId, Long postulacionId) {
        return PostulacionMapper.toDto(buscarDelBeneficiario(beneficiarioId, postulacionId));
    }

    /** HU-22 Gestionar documentos: carga un archivo que soporta un requisito. */
    @Transactional
    public PostulacionResponse cargarDocumento(Long beneficiarioId, Long postulacionId, Long requisitoId,
                                               MultipartFile archivo) {
        Postulacion p = buscarDelBeneficiario(beneficiarioId, postulacionId);
        if (p.estaFinalizada() || p.getEstado().ordinal() >= EstadoPostulacion.POSTULADA.ordinal()) {
            throw new ReglaNegocioException("Ya no puedes modificar documentos de esta postulación");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Selecciona un archivo");
        }
        Requisito requisito = requisitoRepository.findById(requisitoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Requisito", requisitoId));
        boolean perteneceAlPrograma = p.getOportunidad().getPrograma().getRequisitos().stream()
                .anyMatch(r -> r.getId().equals(requisitoId));
        if (!perteneceAlPrograma) {
            throw new ReglaNegocioException("El requisito no corresponde a este programa");
        }

        DocumentoPostulacion doc = new DocumentoPostulacion();
        doc.setNombreArchivo(archivo.getOriginalFilename());
        doc.setTipoArchivo(archivo.getContentType());
        doc.setTamanoBytes(archivo.getSize());
        doc.setRequisito(requisito);
        if (!doc.validarFormato()) {
            throw new ReglaNegocioException("Formato no permitido. Solo PDF, JPG o PNG de máximo 5 MB");
        }

        // Si ya había un documento para ese requisito, se reemplaza
        p.getDocumentos().stream()
                .filter(d -> d.getRequisito() != null && d.getRequisito().getId().equals(requisitoId))
                .toList()
                .forEach(anterior -> {
                    almacenamiento.eliminar(anterior.getRutaAlmacenamiento());
                    p.getDocumentos().remove(anterior);
                });

        doc.setRutaAlmacenamiento(almacenamiento.guardar(archivo, "postulacion-" + p.getId()));
        doc.setEstado(EstadoDocumento.CARGADO);
        p.agregarDocumento(doc);
        postulacionRepository.flush();
        return PostulacionMapper.toDto(p);
    }

    /** HU-23 Registrar postulación. */
    @Transactional
    public PostulacionResponse registrar(Long beneficiarioId, Long postulacionId) {
        Postulacion p = buscarDelBeneficiario(beneficiarioId, postulacionId);
        EstadoPostulacion anterior = p.getEstado();
        if (!p.getOportunidad().estaVigente()) {
            throw new ReglaNegocioException("La convocatoria ya cerró");
        }
        boolean registrada = p.registrar(p.getBeneficiario());
        if (registrada) {
            eventos.publishEvent(new PostulacionEstadoCambiadoEvent(p, anterior, p.getEstado(),
                    "Postulación registrada"));
        } else if (anterior != p.getEstado()) {
            eventos.publishEvent(new PostulacionEstadoCambiadoEvent(p, anterior, p.getEstado(),
                    "Tienes documentos pendientes por cargar"));
        }
        return PostulacionMapper.toDto(p);
    }

    /** El beneficiario desiste del proceso. */
    @Transactional
    public PostulacionResponse cancelar(Long beneficiarioId, Long postulacionId) {
        Postulacion p = buscarDelBeneficiario(beneficiarioId, postulacionId);
        return cambiarEstadoInterno(p, EstadoPostulacion.CERRADA, p.getBeneficiario(), "Cerrada por el beneficiario");
    }

    /** HU-26: recordatorios de todas las postulaciones del beneficiario. */
    @Transactional(readOnly = true)
    public List<RecordatorioResponse> recordatorios(Long beneficiarioId) {
        return recordatorioRepository.findByBeneficiario(beneficiarioId).stream().map(PostulacionMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargarDocumentoDelBeneficiario(Long beneficiarioId, Long postulacionId, Long docId) {
        Postulacion p = buscarDelBeneficiario(beneficiarioId, postulacionId);
        DocumentoPostulacion d = p.getDocumentos().stream().filter(x -> x.getId().equals(docId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", docId));
        return new ArchivoDescarga(almacenamiento.cargar(d.getRutaAlmacenamiento()), d.getNombreArchivo(),
                d.getTipoArchivo());
    }

    // ======================================================== Administrador

    @Transactional(readOnly = true)
    public List<PostulacionResponse> listarTodas(EstadoPostulacion estado) {
        List<Postulacion> lista = estado == null
                ? postulacionRepository.findAllByOrderByFechaSeleccionDesc()
                : postulacionRepository.findByEstadoOrderByFechaSeleccionDesc(estado);
        return lista.stream().map(PostulacionMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PostulacionResponse obtener(Long postulacionId) {
        return PostulacionMapper.toDto(buscar(postulacionId));
    }

    /** HU-25 actualizarEstadoPostulacion(p, estado, obs) con historial de cambios. */
    @Transactional
    public PostulacionResponse actualizarEstado(Long adminId, Long postulacionId, CambioEstadoRequest req) {
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", adminId));
        Postulacion p = buscar(postulacionId);
        if (req.estado() == EstadoPostulacion.POSTULADA && !p.documentosPendientes().isEmpty()) {
            throw new ReglaNegocioException("La postulación aún tiene documentos pendientes");
        }
        return cambiarEstadoInterno(p, req.estado(), admin, req.observacion());
    }

    /** El administrador valida o rechaza un documento cargado. */
    @Transactional
    public PostulacionResponse cambiarEstadoDocumento(Long documentoId, EstadoDocumento estado) {
        DocumentoPostulacion d = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", documentoId));
        d.setEstado(estado);
        Postulacion p = d.getPostulacion();
        if (estado == EstadoDocumento.RECHAZADO) {
            servicioNotificaciones.notificar(p.getBeneficiario(), p, "Documento rechazado",
                    "El documento '" + d.getNombreArchivo() + "' fue rechazado. Por favor cárgalo de nuevo.");
        }
        return PostulacionMapper.toDto(p);
    }

    @Transactional(readOnly = true)
    public ArchivoDescarga descargarDocumento(Long documentoId) {
        DocumentoPostulacion d = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento", documentoId));
        return new ArchivoDescarga(almacenamiento.cargar(d.getRutaAlmacenamiento()), d.getNombreArchivo(),
                d.getTipoArchivo());
    }

    // ======================================================== auxiliares

    private PostulacionResponse cambiarEstadoInterno(Postulacion p, EstadoPostulacion nuevo, Usuario responsable,
                                                     String obs) {
        EstadoPostulacion anterior = p.getEstado();
        p.cambiarEstado(nuevo, responsable, obs);
        if (nuevo == EstadoPostulacion.POSTULADA && p.getFechaPostulacion() == null) {
            p.setFechaPostulacion(java.time.LocalDateTime.now());
        }
        // Publica el evento: ActualizadorTrayectoria notifica y actualiza la trayectoria (HU-27)
        eventos.publishEvent(new PostulacionEstadoCambiadoEvent(p, anterior, nuevo, obs));
        return PostulacionMapper.toDto(p);
    }

    private Postulacion buscar(Long id) {
        return postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));
    }

    private Postulacion buscarDelBeneficiario(Long beneficiarioId, Long postulacionId) {
        Postulacion p = buscar(postulacionId);
        if (!p.getBeneficiario().getId().equals(beneficiarioId)) {
            throw new RecursoNoEncontradoException("Postulación", postulacionId);
        }
        return p;
    }

    /** Archivo listo para enviarse en la respuesta HTTP. */
    public record ArchivoDescarga(Resource recurso, String nombre, String tipo) {
    }
}