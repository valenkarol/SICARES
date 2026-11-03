package co.edu.uniquindio.poo.sicares.servicesTest;

import co.edu.uniquindio.poo.sicares.models.enums.Departamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del enum Departamento (relación departamento 1 ── 1..* municipios, DIVIPOLA).
 * Dueño: Integrante 3.
 */
class DepartamentoTest {

    @Test
    @DisplayName("Tiene los 32 departamentos más Bogotá D.C. y el Quindío con sus 12 municipios")
    void catalogoCompleto() {
        assertEquals(33, Departamento.values().length);
        assertEquals(12, Departamento.QUINDIO.getMunicipios().size());
        assertEquals("Armenia", Departamento.QUINDIO.getMunicipios().get(0), "La capital va primero");
        int total = Arrays.stream(Departamento.values()).mapToInt(d -> d.getMunicipios().size()).sum();
        assertTrue(total > 1100, "DIVIPOLA tiene más de 1.100 municipios");
    }

    @Test
    @DisplayName("Ningún departamento repite municipios")
    void sinRepetidos() {
        for (Departamento d : Departamento.values()) {
            assertEquals(d.getMunicipios().size(), new HashSet<>(d.getMunicipios()).size(), d.getNombre());
        }
    }

    @Test
    @DisplayName("Busca el departamento sin importar tildes, mayúsculas ni si llega la constante")
    void buscarDepartamento() {
        assertEquals(Optional.of(Departamento.QUINDIO), Departamento.buscar("quindio"));
        assertEquals(Optional.of(Departamento.QUINDIO), Departamento.buscar("QUINDIO"));
        assertEquals(Optional.of(Departamento.BOGOTA_DC), Departamento.buscar("Bogotá"));
        assertTrue(Departamento.buscar("Narnia").isEmpty());
        assertTrue(Departamento.buscar(null).isEmpty());
    }

    @Test
    @DisplayName("Un municipio solo es válido en su departamento y se devuelve con el nombre oficial")
    void municipioPerteneceAlDepartamento() {
        assertEquals(Optional.of("La Tebaida"), Departamento.QUINDIO.buscarMunicipio("la tebaida"));
        assertEquals(Optional.of("Calarcá"), Departamento.QUINDIO.buscarMunicipio("CALARCA"));
        assertFalse(Departamento.QUINDIO.contieneMunicipio("Pereira"));
        assertTrue(Departamento.RISARALDA.contieneMunicipio("Pereira"));
        // Mismo nombre en dos departamentos: cada uno tiene el suyo
        assertTrue(Departamento.QUINDIO.contieneMunicipio("Córdoba"));
        assertTrue(Departamento.BOLIVAR.contieneMunicipio("Córdoba"));
    }

    @Test
    @DisplayName("buscarMunicipioEnCualquiera sirve para el municipio de un programa")
    void municipioEnCualquiera() {
        assertEquals(Optional.of("Medellín"), Departamento.buscarMunicipioEnCualquiera("medellin"));
        assertEquals(Optional.of("Bogotá D.C."), Departamento.BOGOTA_DC.buscarMunicipio("Bogotá"));
        assertTrue(Departamento.buscarMunicipioEnCualquiera("Gotham").isEmpty());
    }
}
