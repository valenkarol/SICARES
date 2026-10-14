package co.edu.uniquindio.poo.sicares.config;

import co.edu.uniquindio.poo.sicares.models.entities.*;
import co.edu.uniquindio.poo.sicares.models.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Carga inicial al arrancar:
 *  Crea el administrador por defecto si no existe.
 *  Si no hay instituciones, carga una oferta educativa DE EJEMPLO para pruebas
 *     (HU-12 "cargar información inicial"). Los datos son ilustrativos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final InstitucionRepository institucionRepository;
    private final RequisitoRepository requisitoRepository;
    private final BeneficioRepository beneficioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sicares.admin-inicial.correo}")
    private String correoAdmin;

    @Value("${sicares.admin-inicial.contrasena}")
    private String contrasenaAdmin;

    @Override
    @Transactional
    public void run(String... args) {
        crearAdministrador();
        if (institucionRepository.count() == 0) {
            cargarOfertaDeEjemplo();
        }
    }

    private void crearAdministrador() {
        if (usuarioRepository.existsByCorreoIgnoreCase(correoAdmin)) return;
        Administrador admin = new Administrador();
        admin.setCorreo(correoAdmin);
        admin.setContrasenaHash(passwordEncoder.encode(contrasenaAdmin));
        usuarioRepository.save(admin);
        log.info("Administrador inicial creado: {} (cambia la contraseña)", correoAdmin);
    }

    private void cargarOfertaDeEjemplo() {
        // ---------------- Requisitos
        Requisito docIdentidad = requisito("Documento de identidad", TipoRequisito.DOCUMENTO, null, true);
        Requisito certRuv = requisito("Certificado de inscripción en el RUV", TipoRequisito.CONDICION_VICTIMA, "RUV", true);
        Requisito diploma = requisito("Diploma o acta de grado de bachiller", TipoRequisito.DOCUMENTO, null, true);
        Requisito certNoveno = requisito("Certificado de grado 9° aprobado", TipoRequisito.DOCUMENTO, null, true);
        Requisito nivelMedia = requisito("Haber culminado la educación media", TipoRequisito.NIVEL_EDUCATIVO, "MEDIA", false);
        Requisito edadSena = requisito("Edad entre 15 y 59 años", TipoRequisito.EDAD, "15-59", false);
        Requisito sisben = requisito("SISBÉN grupos A, B o C", TipoRequisito.SISBEN, "A,B,C", false);
        requisitoRepository.saveAll(Set.of(docIdentidad, certRuv, diploma, certNoveno, nivelMedia, edadSena, sisben));

        // ---------------- Instituciones
        Institucion sena = institucion("SENA Regional Quindío", "899999034-1", TipoInstitucion.PUBLICA,
                "Quindío", "Armenia", "contacto@sena.edu.co");
        Institucion uniquindio = institucion("Universidad del Quindío", "890000432-8", TipoInstitucion.PUBLICA,
                "Quindío", "Armenia", "admisiones@uniquindio.edu.co");
        Institucion feyalegria = institucion("Fe y Alegría Colombia", "860010268-9", TipoInstitucion.PRIVADA,
                "Quindío", "Armenia", "info@feyalegria.org.co");
        Institucion icetex = institucion("ICETEX", "899999035-7", TipoInstitucion.PUBLICA,
                "Bogotá D.C.", "Bogotá", "contacto@icetex.gov.co");

        // ---------------- Programas
        ProgramaEducativo alfabetizacion = programa(feyalegria, "Alfabetización y básica primaria (CLEI 1-2)",
                NivelEducativo.BASICA_PRIMARIA, Modalidad.PRESENCIAL, "Formación general", 2, 0, "Armenia",
                docIdentidad);
        ProgramaEducativo secundaria = programa(feyalegria, "Básica secundaria flexible (CLEI 3-4)",
                NivelEducativo.BASICA_SECUNDARIA, Modalidad.PRESENCIAL, "Formación general", 2, 0, "Armenia",
                docIdentidad);
        ProgramaEducativo media = programa(feyalegria, "Bachillerato por ciclos (CLEI 5-6)",
                NivelEducativo.MEDIA, Modalidad.HIBRIDA, "Formación general", 2, 0, "Armenia",
                docIdentidad, certNoveno);
        ProgramaEducativo tecSistemas = programa(sena, "Técnico en Sistemas",
                NivelEducativo.TECNICO, Modalidad.PRESENCIAL, "Tecnología e informática", 3, 0, "Armenia",
                docIdentidad, certNoveno, edadSena);
        ProgramaEducativo tecAgro = programa(sena, "Técnico en Producción Agropecuaria",
                NivelEducativo.TECNICO, Modalidad.PRESENCIAL, "Agropecuaria y ambiental", 3, 0, "Calarcá",
                docIdentidad, certNoveno, edadSena);
        ProgramaEducativo adso = programa(sena, "Tecnólogo en Análisis y Desarrollo de Software",
                NivelEducativo.TECNOLOGICO, Modalidad.VIRTUAL, "Tecnología e informática", 6, 0, "Armenia",
                docIdentidad, diploma);
        ProgramaEducativo enfermeria = programa(uniquindio, "Enfermería",
                NivelEducativo.UNIVERSITARIO, Modalidad.PRESENCIAL, "Salud", 10, 1_200_000, "Armenia",
                docIdentidad, diploma, nivelMedia);
        ProgramaEducativo administracion = programa(uniquindio, "Administración de Negocios (distancia)",
                NivelEducativo.UNIVERSITARIO, Modalidad.DISTANCIA, "Administración y negocios", 10, 900_000, "Armenia",
                docIdentidad, diploma, nivelMedia);

        for (ProgramaEducativo p : new ProgramaEducativo[]{alfabetizacion, secundaria, media, tecSistemas, tecAgro,
                adso, enfermeria, administracion}) {
            oportunidad(p, "2027-1", 80);
        }

        // ---------------- Beneficios
        Beneficio fondo = beneficio(icetex, "Fondo de Reparación para población víctima (ejemplo)",
                TipoBeneficio.CREDITO_CONDONABLE, 100, true,
                "Crédito condonable para matrícula y sostenimiento en educación superior", certRuv);
        fondo.getProgramas().addAll(Set.of(enfermeria, administracion));
        Beneficio descuento = beneficio(uniquindio, "Descuento en matrícula por condición socioeconómica (ejemplo)",
                TipoBeneficio.DESCUENTO, 50, false, "Descuento para estratos 1 y 2", sisben);
        descuento.getProgramas().addAll(Set.of(enfermeria, administracion));
        institucionRepository.saveAll(Set.of(sena, uniquindio, feyalegria, icetex)); // guarda programas y oportunidades en cascada
        beneficioRepository.saveAll(Set.of(fondo, descuento));
        log.info("Oferta educativa de ejemplo cargada");
    }

    // ---------------------------------------------------------------- helpers

    private Requisito requisito(String nombre, TipoRequisito tipo, String valor, boolean requiereDoc) {
        Requisito r = new Requisito();
        r.setNombre(nombre);
        r.setTipo(tipo);
        r.setValorEsperado(valor);
        r.setObligatorio(true);
        r.setRequiereDocumento(requiereDoc);
        return r;
    }

    private Institucion institucion(String nombre, String nit, TipoInstitucion tipo, String depto, String mpio,
                                    String correo) {
        Institucion i = new Institucion();
        i.setNombre(nombre);
        i.setNit(nit);
        i.setTipo(tipo);
        i.setDepartamento(depto);
        i.setMunicipio(mpio);
        i.setCorreoContacto(correo);
        return i;
    }

    private ProgramaEducativo programa(Institucion inst, String nombre, NivelEducativo nivel, Modalidad modalidad,
                                       String area, int periodos, long costo, String municipio, Requisito... reqs) {
        ProgramaEducativo p = new ProgramaEducativo();
        p.setNombre(nombre);
        p.setNivel(nivel);
        p.setModalidad(modalidad);
        p.setAreaConocimiento(area);
        p.setDuracionPeriodos(periodos);
        p.setCostoPeriodo(BigDecimal.valueOf(costo));
        p.setMunicipio(municipio);
        p.setActivo(true);
        p.getRequisitos().addAll(Set.of(reqs));
        inst.agregarPrograma(p);
        return p;
    }

    /** Fechas relativas a hoy para que la convocatoria siempre esté abierta al probar. */
    private void oportunidad(ProgramaEducativo p, String periodo, int cupos) {
        LocalDate hoy = LocalDate.now();
        OportunidadEducativa o = new OportunidadEducativa();
        o.setPeriodoAcademico(periodo);
        o.setFechaAperturaInscripcion(hoy.minusDays(10));
        o.setFechaCierreInscripcion(hoy.plusDays(20));
        o.setFechaAdmision(hoy.plusDays(35));
        o.setFechaMatricula(hoy.plusDays(50));
        o.setCupos(cupos);
        o.setPrograma(p);
        p.getOportunidades().add(o);
    }

    private Beneficio beneficio(Institucion inst, String nombre, TipoBeneficio tipo, double cobertura,
                                boolean soloVictimas, String descripcion, Requisito... reqs) {
        Beneficio b = new Beneficio();
        b.setNombre(nombre);
        b.setTipo(tipo);
        b.setPorcentajeCobertura(cobertura);
        b.setExclusivoVictimas(soloVictimas);
        b.setDescripcion(descripcion);
        b.setFechaLimite(LocalDate.now().plusMonths(6));
        b.setInstitucion(inst);
        b.getRequisitos().addAll(Set.of(reqs));
        return b;
    }
}
