package co.edu.uniquindio.poo.sicares.services;
import co.edu.uniquindio.poo.sicares.dto.CaracterizacionDtos;
import co.edu.uniquindio.poo.sicares.dto.CaracterizacionMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.Departamento;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.models.enums.TipoDocumento;
import co.edu.uniquindio.poo.sicares.notifications.PerfilActualizadoEvent;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Proceso 1: Caracterización del usuario (HU-04 a HU-11).
 * Implementa completarCaracterizacion(), consultarPerfil() y actualizarPerfil() del Beneficiario.
 */
@Service
@RequiredArgsConstructor
public class CaracterizacionService {

    private final BeneficiarioRepository beneficiarioRepository;
    private final RutaEducativaRepository rutaRepository;
    /** Avisa que el perfil cambió para que la ruta se regenere sola (RegeneradorRutas). */
    private final ApplicationEventPublisher eventos;

    @Value("${sicares.politica-datos.version}")
    private String versionPolitica;

    // ---------------------------------------------------------------- consultas

    /** HU-10 consultarPerfil() */
    @Transactional(readOnly = true)
    public CaracterizacionDtos.PerfilResponse consultarPerfil(Long beneficiarioId) {
        return CaracterizacionMapper.toPerfil(buscarBeneficiario(beneficiarioId));
    }

    // ---------------------------------------------------------------- autorización de datos

    @Transactional
    public CaracterizacionDtos.PerfilResponse aceptarAutorizacion(Long beneficiarioId) {
        Beneficiario b = buscarBeneficiario(beneficiarioId);
        if (b.getAutorizacion() == null) {
            b.setAutorizacion(new AutorizacionDatos());
        }
        b.getAutorizacion().setVersionPolitica(versionPolitica);
        b.getAutorizacion().aceptar();
        return CaracterizacionMapper.toPerfil(b);
    }

    @Transactional
    public CaracterizacionDtos.PerfilResponse revocarAutorizacion(Long beneficiarioId) {
        Beneficiario b = buscarBeneficiario(beneficiarioId);
        if (b.getAutorizacion() != null) {
            b.getAutorizacion().revocar();
        }
        return CaracterizacionMapper.toPerfil(b);
    }

    // ---------------------------------------------------------------- secciones (HU-04 a HU-09, HU-11)

    /** HU-04: valida edad (12 a 59), coherencia del tipo de documento y que el municipio sea del departamento. */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarInformacionPersonal(Long beneficiarioId, CaracterizacionDtos.InformacionPersonalDto dto) {
        validarEdadYDocumento(dto);
        Departamento departamento = Departamento.buscar(dto.departamento())
                .orElseThrow(() -> new ReglaNegocioException("Selecciona un departamento de la lista"));
        String municipio = departamento.buscarMunicipio(dto.municipio())
                .orElseThrow(() -> new ReglaNegocioException("El municipio \"" + dto.municipio().trim()
                        + "\" no pertenece al departamento de " + departamento.getNombre()));
        // RNF-13: una misma persona no puede tener dos caracterizaciones (dos cuentas con el mismo documento)
        if (beneficiarioRepository.existsByPerfilInformacionPersonalNumeroDocumentoAndIdNot(
                dto.numeroDocumento(), beneficiarioId)) {
            throw new ReglaNegocioException("Ya existe una persona caracterizada con ese número de documento. "
                    + "Si es tu documento, inicia sesión con la cuenta con la que te registraste.");
        }
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();

        InformacionPersonal info = perfil.getInformacionPersonal() != null
                ? perfil.getInformacionPersonal() : new InformacionPersonal();
        info.setNombres(dto.nombres().trim());
        info.setApellidos(dto.apellidos().trim());
        info.setTipoDocumento(dto.tipoDocumento());
        info.setNumeroDocumento(dto.numeroDocumento());
        info.setFechaNacimiento(dto.fechaNacimiento());
        info.setGenero(dto.genero());
        info.setEtnia(dto.etnia());
        info.setTelefono(dto.telefono());
        info.setDepartamento(departamento.getNombre());   // nombre oficial, con tildes
        info.setMunicipio(municipio);
        info.setZona(dto.zona());
        info.setResponsable(info.esMenorDeEdad() ? crearResponsable(dto.responsable()) : null);
        perfil.setInformacionPersonal(info);

        return finalizarCambio(b);
    }

    /** HU-05 */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarSituacionVictima(Long beneficiarioId, CaracterizacionDtos.SituacionVictimaDto dto) {
        if (!dto.esVictima() && dto.inscritoRUV()) {
            throw new ReglaNegocioException("Si estás inscrito en el RUV debes marcar que eres víctima");
        }
        if (dto.esVictima() && (dto.hechoVictimizante() == null || dto.hechoVictimizante().isBlank())) {
            throw new ReglaNegocioException("Indica el hecho victimizante");
        }
        String[] expulsor = dto.esVictima()
                ? validarLugarExpulsor(dto.departamentoExpulsor(), dto.municipioExpulsor())
                : new String[]{null, null};
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();

        SituacionVictima v = perfil.getSituacionVictima() != null ? perfil.getSituacionVictima() : new SituacionVictima();
        v.setEsVictima(dto.esVictima());
        v.setInscritoRUV(dto.inscritoRUV());
        v.setHechoVictimizante(dto.esVictima() ? dto.hechoVictimizante() : null);
        v.setFechaHecho(dto.esVictima() ? dto.fechaHecho() : null);
        v.setDepartamentoExpulsor(expulsor[0]);
        v.setMunicipioExpulsor(expulsor[1]);
        perfil.setSituacionVictima(v);

        return finalizarCambio(b);
    }

    /** HU-06 */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarSituacionSocioeconomica(Long beneficiarioId, CaracterizacionDtos.SituacionSocioeconomicaDto dto) {
        if (dto.personasACargo() > dto.integrantesHogar()) {
            throw new ReglaNegocioException("Las personas a cargo no pueden superar a los integrantes del hogar");
        }
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();

        SituacionSocioeconomica s = perfil.getSituacionSocioeconomica() != null
                ? perfil.getSituacionSocioeconomica() : new SituacionSocioeconomica();
        s.setGrupoSisben(dto.grupoSisben());
        s.setEstrato(dto.estrato());
        s.setIngresoMensualHogar(dto.ingresoMensualHogar());
        s.setSituacionLaboral(dto.situacionLaboral());
        s.setIntegrantesHogar(dto.integrantesHogar());
        s.setPersonasACargo(dto.personasACargo());
        perfil.setSituacionSocioeconomica(s);

        return finalizarCambio(b);
    }

    /** HU-09 */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarCondicionAccesibilidad(Long beneficiarioId, CaracterizacionDtos.CondicionAccesibilidadDto dto) {
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();

        CondicionAccesibilidad c = perfil.getCondicionAccesibilidad() != null
                ? perfil.getCondicionAccesibilidad() : new CondicionAccesibilidad();
        c.setTieneInternet(dto.tieneInternet());
        c.setTieneDispositivos(dto.tieneDispositivos());
        c.setMedioTransporte(dto.medioTransporte());
        c.setDistanciaMaximaKm(dto.distanciaMaximaKm());
        c.setModalidadPreferida(dto.modalidadPreferida());
        perfil.setCondicionAccesibilidad(c);

        return finalizarCambio(b);
    }

    /** HU-07: reemplaza la lista de estudios con la enviada. */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarTrayectoria(Long beneficiarioId, CaracterizacionDtos.TrayectoriaEducativaDto dto) {
        int anioActual = LocalDate.now().getYear();
        if (dto.anioUltimoEstudio() > anioActual) {
            throw new ReglaNegocioException("El año del último estudio no puede ser futuro");
        }
        List<CaracterizacionDtos.EstudioRealizadoDto> estudios = dto.estudios() == null ? List.of() : dto.estudios();
        for (CaracterizacionDtos.EstudioRealizadoDto e : estudios) {
            if (e.anioInicio() != null && e.anioFin() != null && e.anioFin() < e.anioInicio()) {
                throw new ReglaNegocioException("En un estudio el año de fin es menor que el de inicio");
            }
        }
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();

        TrayectoriaEducativa t = perfil.getTrayectoriaEducativa() != null
                ? perfil.getTrayectoriaEducativa() : new TrayectoriaEducativa();
        t.setNivelActual(dto.nivelActual());
        t.setUltimoGradoAprobado(dto.ultimoGradoAprobado());
        t.setAnioUltimoEstudio(dto.anioUltimoEstudio());
        t.getEstudios().clear();
        for (CaracterizacionDtos.EstudioRealizadoDto e : estudios) {
            EstudioRealizado er = new EstudioRealizado();
            er.setNivel(e.nivel());
            er.setInstitucion(e.institucion());
            er.setTitulo(e.titulo());
            er.setEstado(e.estado());
            er.setAnioInicio(e.anioInicio());
            er.setAnioFin(e.anioFin());
            t.agregarEstudio(er);
        }
        perfil.setTrayectoriaEducativa(t);

        return finalizarCambio(b);
    }

    /** HU-08: reemplaza los intereses (sin duplicados por área). */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarIntereses(Long beneficiarioId, CaracterizacionDtos.InteresesRequest req) {
        Map<String, CaracterizacionDtos.InteresProfesionalDto> unicos = new LinkedHashMap<>();
        req.intereses().forEach(i -> unicos.putIfAbsent(i.areaConocimiento().trim().toLowerCase(), i));
        if (unicos.size() > 5) {
            throw new ReglaNegocioException("Selecciona máximo 5 áreas de interés");
        }
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();
        perfil.reemplazarIntereses(unicos.values().stream()
                .map(i -> new InteresProfesional(i.areaConocimiento().trim(), i.descripcion()))
                .toList());
        return finalizarCambio(b);
    }

    // ---------------------------------------------------------------- auxiliares

    private CaracterizacionDtos.PerfilResponse finalizarCambio(Beneficiario b) {
        // RNF-01 / RNF-16: un cambio no puede dejar secciones del perfil contradictorias.
        // La excepción hace rollback de la transacción: no se guarda nada.
        List<String> inconsistencias = b.getPerfil().inconsistencias();
        if (!inconsistencias.isEmpty()) {
            throw new ReglaNegocioException(String.join(". ", inconsistencias));
        }
        boolean estabaCompleto = b.getPerfil().isCompleto();
        b.getPerfil().actualizar();          // recalcula porcentaje y completitud
        marcarRutasDesactualizadas(b.getId()); // HU-11/HU-27: el perfil cambió, las rutas anteriores ya no aplican
        beneficiarioRepository.save(b);
        boolean completo = b.getPerfil().isCompleto();
        // Se procesa después del commit: si el perfil está al 100 % se genera la ruta nueva automáticamente
        eventos.publishEvent(new PerfilActualizadoEvent(b.getId(), completo, completo && !estabaCompleto));
        return CaracterizacionMapper.toPerfil(b);
    }

    private void marcarRutasDesactualizadas(Long beneficiarioId) {
        rutaRepository.findByBeneficiarioIdAndEstadoNot(beneficiarioId, EstadoRuta.DESACTUALIZADA)
                .forEach(RutaEducativa::marcarDesactualizada);
    }

    private void validarEdadYDocumento(CaracterizacionDtos.InformacionPersonalDto dto) {
        InformacionPersonal tmp = new InformacionPersonal();
        tmp.setFechaNacimiento(dto.fechaNacimiento());
        if (!tmp.validarRangoEdad()) {
            throw new ReglaNegocioException("SICARES atiende personas entre "
                    + InformacionPersonal.EDAD_MINIMA + " y " + InformacionPersonal.EDAD_MAXIMA + " años");
        }
        int edad = tmp.calcularEdad();
        if (edad < 18 && dto.tipoDocumento() == TipoDocumento.CC) {
            throw new ReglaNegocioException("Los menores de 18 años deben registrarse con Tarjeta de Identidad (TI)");
        }
        if (edad >= 18 && dto.tipoDocumento() == TipoDocumento.TI) {
            throw new ReglaNegocioException("Los mayores de edad deben registrarse con Cédula de Ciudadanía (CC)");
        }
        validarNumeroDocumento(dto.tipoDocumento(), dto.numeroDocumento(), "El número de documento");
        if (edad < ResponsableMenor.MAYORIA_EDAD) {
            validarResponsable(dto);
        }
    }

    /** HU-04: un menor de edad debe registrar un adulto responsable (mayor de 18, con CC). */
    private void validarResponsable(CaracterizacionDtos.InformacionPersonalDto dto) {
        CaracterizacionDtos.ResponsableMenorDto r = dto.responsable();
        if (r == null) {
            throw new ReglaNegocioException("Como eres menor de edad debes registrar los datos de un adulto responsable");
        }
        ResponsableMenor tmp = new ResponsableMenor();
        tmp.setFechaNacimiento(r.fechaNacimiento());
        if (!tmp.esMayorDeEdad()) {
            throw new ReglaNegocioException("El responsable debe ser mayor de edad (18 años o más)");
        }
        if (r.tipoDocumento() != TipoDocumento.CC) {
            throw new ReglaNegocioException("El responsable debe identificarse con Cédula de Ciudadanía (CC)");
        }
        validarNumeroDocumento(TipoDocumento.CC, r.numeroDocumento(), "La cédula del responsable");
        if (r.numeroDocumento().equals(dto.numeroDocumento())) {
            throw new ReglaNegocioException("El documento del responsable no puede ser el mismo del beneficiario");
        }
    }

    /**
     * Lugar expulsor (opcional): si se indica el municipio, el departamento es obligatorio y el municipio debe
     * pertenecer a él. Devuelve {departamento, municipio} con los nombres oficiales (o null si no se indicaron).
     */
    static String[] validarLugarExpulsor(String departamento, String municipio) {
        boolean hayDepartamento = departamento != null && !departamento.isBlank();
        boolean hayMunicipio = municipio != null && !municipio.isBlank();
        if (!hayDepartamento && !hayMunicipio) {
            return new String[]{null, null};
        }
        if (!hayDepartamento) {
            throw new ReglaNegocioException("Selecciona el departamento del municipio expulsor");
        }
        Departamento d = Departamento.buscar(departamento)
                .orElseThrow(() -> new ReglaNegocioException("Selecciona un departamento expulsor de la lista"));
        if (!hayMunicipio) {
            return new String[]{d.getNombre(), null};
        }
        String m = d.buscarMunicipio(municipio)
                .orElseThrow(() -> new ReglaNegocioException("El municipio expulsor \"" + municipio.trim()
                        + "\" no pertenece al departamento de " + d.getNombre()));
        return new String[]{d.getNombre(), m};
    }

    /** Cédula: 6 a 10 dígitos. Tarjeta de identidad (NUIP): 10 u 11 dígitos. Solo números. */
    static void validarNumeroDocumento(TipoDocumento tipo, String numero, String campo) {
        String n = numero == null ? "" : numero.trim();
        if (!n.matches("\\d+")) {
            throw new ReglaNegocioException(campo + " solo puede tener números (sin puntos, espacios ni letras)");
        }
        if (tipo == TipoDocumento.CC && (n.length() < 6 || n.length() > 10)) {
            throw new ReglaNegocioException(campo + " (cédula) debe tener entre 6 y 10 dígitos; escribiste " + n.length());
        }
        if (tipo == TipoDocumento.TI && (n.length() < 10 || n.length() > 11)) {
            throw new ReglaNegocioException(campo + " (tarjeta de identidad) debe tener 10 u 11 dígitos; escribiste "
                    + n.length());
        }
    }

    private ResponsableMenor crearResponsable(CaracterizacionDtos.ResponsableMenorDto r) {
        ResponsableMenor resp = new ResponsableMenor();
        resp.setNombres(r.nombres().trim());
        resp.setApellidos(r.apellidos().trim());
        resp.setTipoDocumento(r.tipoDocumento());
        resp.setNumeroDocumento(r.numeroDocumento());
        resp.setFechaNacimiento(r.fechaNacimiento());
        resp.setParentesco(r.parentesco());
        resp.setTelefono(r.telefono());
        resp.setCorreo(r.correo() == null || r.correo().isBlank() ? null : r.correo().trim());
        return resp;
    }

    private Beneficiario buscarBeneficiario(Long id) {
        return beneficiarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Beneficiario", id));
    }

    /** No se guarda información personal sin autorización de tratamiento de datos (Ley 1581 de 2012). */
    private Beneficiario buscarBeneficiarioConAutorizacion(Long id) {
        Beneficiario b = buscarBeneficiario(id);
        if (b.getAutorizacion() == null || !b.getAutorizacion().isAceptada()) {
            throw new ReglaNegocioException("Debes aceptar la política de tratamiento de datos para continuar");
        }
        return b;
    }
}