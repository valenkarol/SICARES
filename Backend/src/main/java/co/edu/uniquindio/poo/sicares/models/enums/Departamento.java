package co.edu.uniquindio.poo.sicares.models.enums;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Departamentos de Colombia con sus municipios (DIVIPOLA - DANE), incluido Bogotá D.C. y las áreas no
 * municipalizadas de Amazonas, Guainía y Vaupés.
 * <p>
 * Relación: un Departamento 1 ── 1..* municipios. En la base de datos se sigue guardando el nombre oficial
 * ({@link #getNombre()} y el nombre del municipio), así los registros existentes ("Quindío", "Armenia") siguen
 * siendo válidos y el motor de reglas puede comparar el municipio del beneficiario con el del programa.
 * La búsqueda ignora mayúsculas, tildes y espacios repetidos ("quindio" = "Quindío").
 */
public enum Departamento {
    AMAZONAS("Amazonas", List.of(
            "Leticia", "Puerto Nariño", "El Encanto", "La Chorrera", "La Pedrera", "La Victoria", "Mirití-Paraná",
            "Puerto Alegría", "Puerto Arica", "Puerto Santander", "Tarapacá")),
    ANTIOQUIA("Antioquia", List.of(
            "Medellín", "Abejorral", "Abriaquí", "Alejandría", "Amagá", "Amalfi", "Andes", "Angelópolis",
            "Angostura", "Anorí", "Anzá", "Apartadó", "Arboletes", "Argelia", "Armenia", "Barbosa", "Bello",
            "Belmira", "Betania", "Betulia", "Briceño", "Buriticá", "Cáceres", "Caicedo", "Caldas", "Campamento",
            "Cañasgordas", "Caracolí", "Caramanta", "Carepa", "Carolina", "Caucasia", "Chigorodó", "Cisneros",
            "Ciudad Bolívar", "Cocorná", "Concepción", "Concordia", "Copacabana", "Dabeiba", "Donmatías", "Ebéjico",
            "El Bagre", "El Carmen de Viboral", "El Peñol", "El Retiro", "El Santuario", "Entrerríos", "Envigado",
            "Fredonia", "Frontino", "Giraldo", "Girardota", "Gómez Plata", "Granada", "Guadalupe", "Guarne",
            "Guatapé", "Heliconia", "Hispania", "Itagüí", "Ituango", "Jardín", "Jericó", "La Ceja", "La Estrella",
            "La Pintada", "La Unión", "Liborina", "Maceo", "Marinilla", "Montebello", "Murindó", "Mutatá", "Nariño",
            "Nechí", "Necoclí", "Olaya", "Peque", "Pueblorrico", "Puerto Berrío", "Puerto Nare", "Puerto Triunfo",
            "Remedios", "Rionegro", "Sabanalarga", "Sabaneta", "Salgar", "San Andrés de Cuerquia", "San Carlos",
            "San Francisco", "San Jerónimo", "San José de la Montaña", "San Juan de Urabá", "San Luis",
            "San Pedro de los Milagros", "San Pedro de Urabá", "San Rafael", "San Roque", "San Vicente Ferrer",
            "Santa Bárbara", "Santa Fe de Antioquia", "Santa Rosa de Osos", "Santo Domingo", "Segovia", "Sonsón",
            "Sopetrán", "Támesis", "Tarazá", "Tarso", "Titiribí", "Toledo", "Turbo", "Uramita", "Urrao", "Valdivia",
            "Valparaíso", "Vegachí", "Venecia", "Vigía del Fuerte", "Yalí", "Yarumal", "Yolombó", "Yondó",
            "Zaragoza")),
    ARAUCA("Arauca", List.of(
            "Arauca", "Arauquita", "Cravo Norte", "Fortul", "Puerto Rondón", "Saravena", "Tame")),
    ATLANTICO("Atlántico", List.of(
            "Barranquilla", "Baranoa", "Campo de la Cruz", "Candelaria", "Galapa", "Juan de Acosta", "Luruaco",
            "Malambo", "Manatí", "Palmar de Varela", "Piojó", "Polonuevo", "Ponedera", "Puerto Colombia", "Repelón",
            "Sabanagrande", "Sabanalarga", "Santa Lucía", "Santo Tomás", "Soledad", "Suan", "Tubará", "Usiacurí")),
    BOGOTA_DC("Bogotá D.C.", List.of(
            "Bogotá D.C.")),
    BOLIVAR("Bolívar", List.of(
            "Cartagena de Indias", "Achí", "Altos del Rosario", "Arenal", "Arjona", "Arroyohondo",
            "Barranco de Loba", "Calamar", "Cantagallo", "Cicuco", "Clemencia", "Córdoba", "El Carmen de Bolívar",
            "El Guamo", "El Peñón", "Hatillo de Loba", "Magangué", "Mahates", "Margarita", "María La Baja",
            "Mompós", "Montecristo", "Morales", "Norosí", "Pinillos", "Regidor", "Río Viejo", "San Cristóbal",
            "San Estanislao", "San Fernando", "San Jacinto", "San Jacinto del Cauca", "San Juan Nepomuceno",
            "San Martín de Loba", "San Pablo", "Santa Catalina", "Santa Rosa", "Santa Rosa del Sur", "Simití",
            "Soplaviento", "Talaigua Nuevo", "Tiquisio", "Turbaco", "Turbaná", "Villanueva", "Zambrano")),
    BOYACA("Boyacá", List.of(
            "Tunja", "Almeida", "Aquitania", "Arcabuco", "Belén", "Berbeo", "Betéitiva", "Boavita", "Boyacá",
            "Briceño", "Buenavista", "Busbanzá", "Caldas", "Campohermoso", "Cerinza", "Chinavita", "Chiquinquirá",
            "Chíquiza", "Chiscas", "Chita", "Chitaraque", "Chivatá", "Chivor", "Ciénega", "Cómbita", "Coper",
            "Corrales", "Covarachía", "Cubará", "Cucaita", "Cuítiva", "Duitama", "El Cocuy", "El Espino",
            "Firavitoba", "Floresta", "Gachantivá", "Gámeza", "Garagoa", "Guacamayas", "Guateque", "Guayatá",
            "Güicán", "Iza", "Jenesano", "Jericó", "La Capilla", "La Uvita", "La Victoria", "Labranzagrande",
            "Macanal", "Maripí", "Miraflores", "Mongua", "Monguí", "Moniquirá", "Motavita", "Muzo", "Nobsa",
            "Nuevo Colón", "Oicatá", "Otanche", "Pachavita", "Páez", "Paipa", "Pajarito", "Panqueba", "Pauna",
            "Paya", "Paz de Río", "Pesca", "Pisba", "Puerto Boyacá", "Quípama", "Ramiriquí", "Ráquira", "Rondón",
            "Saboyá", "Sáchica", "Samacá", "San Eduardo", "San José de Pare", "San Luis de Gaceno", "San Mateo",
            "San Miguel de Sema", "San Pablo de Borbur", "Santa María", "Santa Rosa de Viterbo", "Santa Sofía",
            "Santana", "Sativanorte", "Sativasur", "Siachoque", "Soatá", "Socha", "Socotá", "Sogamoso", "Somondoco",
            "Sora", "Soracá", "Sotaquirá", "Susacón", "Sutamarchán", "Sutatenza", "Tasco", "Tenza", "Tibaná",
            "Tibasosa", "Tinjacá", "Tipacoque", "Toca", "Togüí", "Tópaga", "Tota", "Tununguá", "Turmequé", "Tuta",
            "Tutazá", "Úmbita", "Ventaquemada", "Villa de Leyva", "Viracachá", "Zetaquira")),
    CALDAS("Caldas", List.of(
            "Manizales", "Aguadas", "Anserma", "Aranzazu", "Belalcázar", "Chinchiná", "Filadelfia", "La Dorada",
            "La Merced", "Manzanares", "Marmato", "Marquetalia", "Marulanda", "Neira", "Norcasia", "Pácora",
            "Palestina", "Pensilvania", "Riosucio", "Risaralda", "Salamina", "Samaná", "San José", "Supía",
            "Victoria", "Villamaría", "Viterbo")),
    CAQUETA("Caquetá", List.of(
            "Florencia", "Albania", "Belén de los Andaquíes", "Cartagena del Chairá", "Curillo", "El Doncello",
            "El Paujil", "La Montañita", "Milán", "Morelia", "Puerto Rico", "San José del Fragua",
            "San Vicente del Caguán", "Solano", "Solita", "Valparaíso")),
    CASANARE("Casanare", List.of(
            "Yopal", "Aguazul", "Chámeza", "Hato Corozal", "La Salina", "Maní", "Monterrey", "Nunchía", "Orocué",
            "Paz de Ariporo", "Pore", "Recetor", "Sabanalarga", "Sácama", "San Luis de Palenque", "Támara",
            "Tauramena", "Trinidad", "Villanueva")),
    CAUCA("Cauca", List.of(
            "Popayán", "Almaguer", "Argelia", "Balboa", "Bolívar", "Buenos Aires", "Cajibío", "Caldono", "Caloto",
            "Corinto", "El Tambo", "Florencia", "Guachené", "Guapí", "Inzá", "Jambaló", "La Sierra", "La Vega",
            "López de Micay", "Mercaderes", "Miranda", "Morales", "Padilla", "Páez", "Patía", "Piamonte",
            "Piendamó - Tunía", "Puerto Tejada", "Puracé", "Rosas", "San Sebastián", "Santa Rosa",
            "Santander de Quilichao", "Silvia", "Sotará", "Suárez", "Sucre", "Timbío", "Timbiquí", "Toribío",
            "Totoró", "Villa Rica")),
    CESAR("Cesar", List.of(
            "Valledupar", "Aguachica", "Agustín Codazzi", "Astrea", "Becerril", "Bosconia", "Chimichagua",
            "Chiriguaná", "Curumaní", "El Copey", "El Paso", "Gamarra", "González", "La Gloria",
            "La Jagua de Ibirico", "La Paz", "Manaure Balcón del Cesar", "Pailitas", "Pelaya", "Pueblo Bello",
            "Río de Oro", "San Alberto", "San Diego", "San Martín", "Tamalameque")),
    CHOCO("Chocó", List.of(
            "Quibdó", "Acandí", "Alto Baudó", "Atrato", "Bagadó", "Bahía Solano", "Bajo Baudó", "Bojayá",
            "Carmen del Darién", "Cértegui", "Condoto", "El Cantón del San Pablo", "El Carmen de Atrato",
            "El Litoral del San Juan", "Istmina", "Juradó", "Lloró", "Medio Atrato", "Medio Baudó",
            "Medio San Juan", "Nóvita", "Nuquí", "Río Iró", "Río Quito", "Riosucio", "San José del Palmar", "Sipí",
            "Tadó", "Unguía", "Unión Panamericana")),
    CORDOBA("Córdoba", List.of(
            "Montería", "Ayapel", "Buenavista", "Canalete", "Cereté", "Chimá", "Chinú", "Ciénaga de Oro", "Cotorra",
            "La Apartada", "Lorica", "Los Córdobas", "Momil", "Montelíbano", "Moñitos", "Planeta Rica",
            "Pueblo Nuevo", "Puerto Escondido", "Puerto Libertador", "Purísima", "Sahagún",
            "San Andrés de Sotavento", "San Antero", "San Bernardo del Viento", "San Carlos", "San José de Uré",
            "San Pelayo", "Tierralta", "Tuchín", "Valencia")),
    CUNDINAMARCA("Cundinamarca", List.of(
            "Agua de Dios", "Albán", "Anapoima", "Anolaima", "Apulo", "Arbeláez", "Beltrán", "Bituima", "Bojacá",
            "Cabrera", "Cachipay", "Cajicá", "Caparrapí", "Cáqueza", "Carmen de Carupa", "Chaguaní", "Chía",
            "Chipaque", "Choachí", "Chocontá", "Cogua", "Cota", "Cucunubá", "El Colegio", "El Peñón", "El Rosal",
            "Facatativá", "Fómeque", "Fosca", "Funza", "Fúquene", "Fusagasugá", "Gachalá", "Gachancipá", "Gachetá",
            "Gama", "Girardot", "Granada", "Guachetá", "Guaduas", "Guasca", "Guataquí", "Guatavita",
            "Guayabal de Síquima", "Guayabetal", "Gutiérrez", "Jerusalén", "Junín", "La Calera", "La Mesa",
            "La Palma", "La Peña", "La Vega", "Lenguazaque", "Machetá", "Madrid", "Manta", "Medina", "Mosquera",
            "Nariño", "Nemocón", "Nilo", "Nimaima", "Nocaima", "Pacho", "Paime", "Pandi", "Paratebueno", "Pasca",
            "Puerto Salgar", "Pulí", "Quebradanegra", "Quetame", "Quipile", "Ricaurte",
            "San Antonio del Tequendama", "San Bernardo", "San Cayetano", "San Francisco", "San Juan de Rioseco",
            "Sasaima", "Sesquilé", "Sibaté", "Silvania", "Simijaca", "Soacha", "Sopó", "Subachoque", "Suesca",
            "Supatá", "Susa", "Sutatausa", "Tabio", "Tausa", "Tena", "Tenjo", "Tibacuy", "Tibirita", "Tocaima",
            "Tocancipá", "Topaipí", "Ubalá", "Ubaque", "Une", "Útica", "Venecia", "Vergara", "Vianí",
            "Villa de San Diego de Ubaté", "Villagómez", "Villapinzón", "Villeta", "Viotá", "Yacopí", "Zipacón",
            "Zipaquirá")),
    GUAINIA("Guainía", List.of(
            "Inírida", "Barrancominas", "Cacahual", "La Guadalupe", "Pana Pana", "Puerto Colombia", "San Felipe")),
    GUAVIARE("Guaviare", List.of(
            "San José del Guaviare", "Calamar", "El Retorno", "Miraflores")),
    HUILA("Huila", List.of(
            "Neiva", "Acevedo", "Agrado", "Aipe", "Algeciras", "Altamira", "Baraya", "Campoalegre", "Colombia",
            "Elías", "Garzón", "Gigante", "Guadalupe", "Hobo", "Íquira", "Isnos", "La Argentina", "La Plata",
            "Nátaga", "Oporapa", "Paicol", "Palermo", "Palestina", "Pital", "Pitalito", "Rivera", "Saladoblanco",
            "San Agustín", "Santa María", "Suaza", "Tarqui", "Tello", "Teruel", "Tesalia", "Timaná", "Villavieja",
            "Yaguará")),
    LA_GUAJIRA("La Guajira", List.of(
            "Riohacha", "Albania", "Barrancas", "Dibulla", "Distracción", "El Molino", "Fonseca", "Hatonuevo",
            "La Jagua del Pilar", "Maicao", "Manaure", "San Juan del Cesar", "Uribia", "Urumita", "Villanueva")),
    MAGDALENA("Magdalena", List.of(
            "Santa Marta", "Algarrobo", "Aracataca", "Ariguaní", "Cerro de San Antonio", "Chivolo", "Ciénaga",
            "Concordia", "El Banco", "El Piñón", "El Retén", "Fundación", "Guamal", "Nueva Granada", "Pedraza",
            "Pijiño del Carmen", "Pivijay", "Plato", "Puebloviejo", "Remolino", "Sabanas de San Ángel", "Salamina",
            "San Sebastián de Buenavista", "San Zenón", "Santa Ana", "Santa Bárbara de Pinto", "Sitionuevo",
            "Tenerife", "Zapayán", "Zona Bananera")),
    META("Meta", List.of(
            "Villavicencio", "Acacías", "Barranca de Upía", "Cabuyaro", "Castilla la Nueva", "Cubarral", "Cumaral",
            "El Calvario", "El Castillo", "El Dorado", "Fuente de Oro", "Granada", "Guamal", "La Macarena",
            "Lejanías", "Mapiripán", "Mesetas", "Puerto Concordia", "Puerto Gaitán", "Puerto Lleras",
            "Puerto López", "Puerto Rico", "Restrepo", "San Carlos de Guaroa", "San Juan de Arama", "San Juanito",
            "San Martín", "Uribe", "Vistahermosa")),
    NARINO("Nariño", List.of(
            "Pasto", "Albán", "Aldana", "Ancuya", "Arboleda", "Barbacoas", "Belén", "Buesaco", "Chachagüí", "Colón",
            "Consacá", "Contadero", "Córdoba", "Cuaspud", "Cumbal", "Cumbitara", "El Charco", "El Peñol",
            "El Rosario", "El Tablón de Gómez", "El Tambo", "Francisco Pizarro", "Funes", "Guachucal",
            "Guaitarilla", "Gualmatán", "Iles", "Imués", "Ipiales", "La Cruz", "La Florida", "La Llanada",
            "La Tola", "La Unión", "Leiva", "Linares", "Los Andes", "Magüí", "Mallama", "Mosquera", "Nariño",
            "Olaya Herrera", "Ospina", "Policarpa", "Potosí", "Providencia", "Puerres", "Pupiales", "Ricaurte",
            "Roberto Payán", "Samaniego", "San Andrés de Tumaco", "San Bernardo", "San Lorenzo", "San Pablo",
            "San Pedro de Cartago", "Sandoná", "Santa Bárbara", "Santacruz", "Sapuyes", "Taminango", "Tangua",
            "Túquerres", "Yacuanquer")),
    NORTE_DE_SANTANDER("Norte de Santander", List.of(
            "Cúcuta", "Ábrego", "Arboledas", "Bochalema", "Bucarasica", "Cáchira", "Cácota", "Chinácota", "Chitagá",
            "Convención", "Cucutilla", "Durania", "El Carmen", "El Tarra", "El Zulia", "Gramalote", "Hacarí",
            "Herrán", "La Esperanza", "La Playa", "Labateca", "Los Patios", "Lourdes", "Mutiscua", "Ocaña",
            "Pamplona", "Pamplonita", "Puerto Santander", "Ragonvalia", "Salazar", "San Calixto", "San Cayetano",
            "Santiago", "Sardinata", "Silos", "Teorama", "Tibú", "Toledo", "Villa Caro", "Villa del Rosario")),
    PUTUMAYO("Putumayo", List.of(
            "Mocoa", "Colón", "Orito", "Puerto Asís", "Puerto Caicedo", "Puerto Guzmán", "Puerto Leguízamo",
            "San Francisco", "San Miguel", "Santiago", "Sibundoy", "Valle del Guamuez", "Villagarzón")),
    QUINDIO("Quindío", List.of(
            "Armenia", "Buenavista", "Calarcá", "Circasia", "Córdoba", "Filandia", "Génova", "La Tebaida",
            "Montenegro", "Pijao", "Quimbaya", "Salento")),
    RISARALDA("Risaralda", List.of(
            "Pereira", "Apía", "Balboa", "Belén de Umbría", "Dosquebradas", "Guática", "La Celia", "La Virginia",
            "Marsella", "Mistrató", "Pueblo Rico", "Quinchía", "Santa Rosa de Cabal", "Santuario")),
    SAN_ANDRES("San Andrés y Providencia", List.of(
            "San Andrés", "Providencia")),
    SANTANDER("Santander", List.of(
            "Bucaramanga", "Aguada", "Albania", "Aratoca", "Barbosa", "Barichara", "Barrancabermeja", "Betulia",
            "Bolívar", "Cabrera", "California", "Capitanejo", "Carcasí", "Cepitá", "Cerrito", "Charalá", "Charta",
            "Chima", "Chipatá", "Cimitarra", "Concepción", "Confines", "Contratación", "Coromoro", "Curití",
            "El Carmen de Chucurí", "El Guacamayo", "El Peñón", "El Playón", "Encino", "Enciso", "Florián",
            "Floridablanca", "Galán", "Gámbita", "Girón", "Guaca", "Guadalupe", "Guapotá", "Guavatá", "Güepsa",
            "Hato", "Jesús María", "Jordán", "La Belleza", "La Paz", "Landázuri", "Lebrija", "Los Santos",
            "Macaravita", "Málaga", "Matanza", "Mogotes", "Molagavita", "Ocamonte", "Oiba", "Onzaga", "Palmar",
            "Palmas del Socorro", "Páramo", "Piedecuesta", "Pinchote", "Puente Nacional", "Puerto Parra",
            "Puerto Wilches", "Rionegro", "Sabana de Torres", "San Andrés", "San Benito", "San Gil", "San Joaquín",
            "San José de Miranda", "San Miguel", "San Vicente de Chucurí", "Santa Bárbara", "Santa Helena del Opón",
            "Simacota", "Socorro", "Suaita", "Sucre", "Suratá", "Tona", "Valle de San José", "Vélez", "Vetas",
            "Villanueva", "Zapatoca")),
    SUCRE("Sucre", List.of(
            "Sincelejo", "Buenavista", "Caimito", "Chalán", "Colosó", "Corozal", "Coveñas", "El Roble", "Galeras",
            "Guaranda", "La Unión", "Los Palmitos", "Majagual", "Morroa", "Ovejas", "Palmito", "Sampués",
            "San Benito Abad", "San Juan de Betulia", "San Luis de Sincé", "San Marcos", "San Onofre", "San Pedro",
            "Santiago de Tolú", "Sucre", "Tolú Viejo")),
    TOLIMA("Tolima", List.of(
            "Ibagué", "Alpujarra", "Alvarado", "Ambalema", "Anzoátegui", "Armero", "Ataco", "Cajamarca",
            "Carmen de Apicalá", "Casabianca", "Chaparral", "Coello", "Coyaima", "Cunday", "Dolores", "Espinal",
            "Falan", "Flandes", "Fresno", "Guamo", "Herveo", "Honda", "Icononzo", "Lérida", "Líbano", "Melgar",
            "Murillo", "Natagaima", "Ortega", "Palocabildo", "Piedras", "Planadas", "Prado", "Purificación",
            "Rioblanco", "Roncesvalles", "Rovira", "Saldaña", "San Antonio", "San Luis",
            "San Sebastián de Mariquita", "Santa Isabel", "Suárez", "Valle de San Juan", "Venadillo",
            "Villahermosa", "Villarrica")),
    VALLE_DEL_CAUCA("Valle del Cauca", List.of(
            "Cali", "Alcalá", "Andalucía", "Ansermanuevo", "Argelia", "Bolívar", "Buenaventura", "Bugalagrande",
            "Caicedonia", "Calima", "Candelaria", "Cartago", "Dagua", "El Águila", "El Cairo", "El Cerrito",
            "El Dovio", "Florida", "Ginebra", "Guacarí", "Guadalajara de Buga", "Jamundí", "La Cumbre", "La Unión",
            "La Victoria", "Obando", "Palmira", "Pradera", "Restrepo", "Riofrío", "Roldanillo", "San Pedro",
            "Sevilla", "Toro", "Trujillo", "Tuluá", "Ulloa", "Versalles", "Vijes", "Yotoco", "Yumbo", "Zarzal")),
    VAUPES("Vaupés", List.of(
            "Mitú", "Carurú", "Taraira", "Pacoa", "Papunahua", "Yavaraté")),
    VICHADA("Vichada", List.of(
            "Puerto Carreño", "Cumaribo", "La Primavera", "Santa Rosalía"));

    private final String nombre;
    private final List<String> municipios;

    Departamento(String nombre, List<String> municipios) {
        this.nombre = nombre;
        this.municipios = municipios;
    }

    /** Nombre oficial con tildes, el que se muestra y se guarda (ej. "Quindío"). */
    public String getNombre() {
        return nombre;
    }

    /** Municipios del departamento, la capital primero. */
    public List<String> getMunicipios() {
        return municipios;
    }

    /** Busca un departamento por su nombre ("Quindío", "quindio") o por la constante ("QUINDIO"). */
    public static Optional<Departamento> buscar(String texto) {
        if (texto == null || texto.isBlank()) return Optional.empty();
        String buscado = normalizar(texto);
        return Arrays.stream(values())
                .filter(d -> normalizar(d.nombre).equals(buscado) || normalizar(d.name()).equals(buscado)
                        || normalizar(d.nombre).replace(" dc", "").equals(buscado))   // "Bogotá" = "Bogotá D.C."
                .findFirst();
    }

    /** Devuelve el nombre oficial del municipio si pertenece a este departamento. */
    public Optional<String> buscarMunicipio(String texto) {
        if (texto == null || texto.isBlank()) return Optional.empty();
        String buscado = normalizar(texto);
        return municipios.stream()
                .filter(m -> normalizar(m).equals(buscado) || normalizar(m).replace(" dc", "").equals(buscado))
                .findFirst();
    }

    public boolean contieneMunicipio(String texto) {
        return buscarMunicipio(texto).isPresent();
    }

    /** Nombre oficial del municipio en cualquier departamento (para campos sin departamento, ej. el programa). */
    public static Optional<String> buscarMunicipioEnCualquiera(String texto) {
        return Arrays.stream(values())
                .map(d -> d.buscarMunicipio(texto))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** Minúsculas, sin tildes, guiones ni espacios repetidos: "Piendamó - Tunía" -> "piendamo tunia". */
    static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replace('_', ' ').replace('-', ' ').replaceAll("[.]", "")
                .replaceAll("\\s+", " ").trim();
    }
}
