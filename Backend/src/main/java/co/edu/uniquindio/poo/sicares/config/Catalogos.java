package co.edu.uniquindio.poo.sicares.config;

import java.util.List;

/**
 * Catálogos fijos usados en formularios. Las áreas de conocimiento deben coincidir entre
 * InteresProfesional.areaConocimiento y ProgramaEducativo.areaConocimiento para que la
 * ReglaIntereses encuentre coincidencias.
 */
public final class Catalogos {

    private Catalogos() {
    }

    public static final List<String> AREAS_CONOCIMIENTO = List.of(
            "Formación general",
            "Tecnología e informática",
            "Salud",
            "Agropecuaria y ambiental",
            "Administración y negocios",
            "Educación",
            "Artes y diseño",
            "Ingeniería",
            "Ciencias sociales y humanas",
            "Construcción y oficios",
            "Turismo y gastronomía"
    );

    public static final List<String> HECHOS_VICTIMIZANTES = List.of(
            "Desplazamiento forzado",
            "Amenaza",
            "Homicidio de familiar",
            "Desaparición forzada",
            "Despojo de tierras",
            "Minas antipersonal",
            "Reclutamiento forzado",
            "Otro"
    );

    public static final List<String> GRUPOS_SISBEN = List.of(
            "A1", "A2", "A3", "A4", "A5",
            "B1", "B2", "B3", "B4", "B5", "B6", "B7",
            "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10",
            "C11", "C12", "C13", "C14", "C15", "C16", "C17", "C18",
            "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10",
            "D11", "D12", "D13", "D14", "D15", "D16", "D17", "D18", "D19", "D20", "D21",
            "NO_TIENE"
    );

    public static final List<String> MEDIOS_TRANSPORTE = List.of(
            "PUBLICO", "PROPIO", "BICICLETA", "A_PIE", "NINGUNO"
    );

    public static final List<String> SITUACIONES_LABORALES = List.of(
            "Desempleado", "Empleado formal", "Trabajo informal", "Independiente", "Estudiante", "Hogar"
    );
}
