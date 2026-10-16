package co.edu.uniquindio.poo.sicares.services;


import co.edu.uniquindio.poo.sicares.dto.CaracterizacionDtos;
import co.edu.uniquindio.poo.sicares.dto.CaracterizacionMapper;
import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.EstadoRuta;
import co.edu.uniquindio.poo.sicares.models.enums.TipoDocumento;
import co.edu.uniquindio.poo.sicares.repository.BeneficiarioRepository;
import co.edu.uniquindio.poo.sicares.repository.RutaEducativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    /** HU-04: valida edad (12 a 59) y coherencia del tipo de documento. */
    @Transactional
    public CaracterizacionDtos.PerfilResponse guardarInformacionPersonal(Long beneficiarioId, CaracterizacionDtos.InformacionPersonalDto dto) {
        validarEdadYDocumento(dto);
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
        info.setDepartamento(dto.departamento().trim());
        info.setMunicipio(dto.municipio().trim());
        info.setZona(dto.zona());
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
        Beneficiario b = buscarBeneficiarioConAutorizacion(beneficiarioId);
        PerfilEducativo perfil = b.obtenerOCrearPerfil();

        SituacionVictima v = perfil.getSituacionVictima() != null ? perfil.getSituacionVictima() : new SituacionVictima();
        v.setEsVictima(dto.esVictima());
        v.setInscritoRUV(dto.inscritoRUV());
        v.setHechoVictimizante(dto.esVictima() ? dto.hechoVictimizante() : null);
        v.setFechaHecho(dto.esVictima() ? dto.fechaHecho() : null);
        v.setMunicipioExpulsor(dto.esVictima() ? dto.municipioExpulsor() : null);
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
        b.getPerfil().actualizar();          // recalcula porcentaje y completitud
        marcarRutasDesactualizadas(b.getId()); // HU-11/HU-27: el perfil cambió, las rutas anteriores ya no aplican
        beneficiarioRepository.save(b);
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