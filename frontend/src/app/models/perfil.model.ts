export interface InformacionPersonal {
  nombres: string;
  apellidos: string;
  tipoDocumento: string;
  numeroDocumento: string;
  fechaNacimiento: string; // yyyy-MM-dd
  genero?: string;
  etnia?: string;
  telefono?: string;
  departamento: string;
  municipio: string;
  zona: string;
  edad?: number;
  /** Obligatorio cuando el beneficiario es menor de 18 años. */
  responsable?: ResponsableMenor | null;
}

/** Adulto responsable de un beneficiario menor de edad (HU-04). */
export interface ResponsableMenor {
  nombres: string;
  apellidos: string;
  tipoDocumento: string; // siempre CC (debe ser mayor de edad)
  numeroDocumento: string;
  fechaNacimiento: string; // yyyy-MM-dd
  parentesco: string;
  telefono: string;
  correo?: string;
}

export interface SituacionVictima {
  esVictima: boolean;
  inscritoRUV: boolean;
  hechoVictimizante?: string;
  fechaHecho?: string;
  municipioExpulsor?: string;
}

export interface SituacionSocioeconomica {
  grupoSisben?: string;
  estrato: number;
  ingresoMensualHogar?: number;
  situacionLaboral?: string;
  integrantesHogar: number;
  personasACargo: number;
}

export interface CondicionAccesibilidad {
  tieneInternet: boolean;
  tieneDispositivos: boolean;
  medioTransporte?: string;
  distanciaMaximaKm: number;
  modalidadPreferida?: string;
}

export interface EstudioRealizado {
  id?: number;
  nivel: string;
  institucion?: string;
  titulo?: string;
  estado: string;
  anioInicio?: number;
  anioFin?: number;
}

export interface TrayectoriaEducativa {
  nivelActual: string;
  ultimoGradoAprobado: number;
  anioUltimoEstudio: number;
  estudios: EstudioRealizado[];
}

export interface InteresProfesional {
  areaConocimiento: string;
  descripcion?: string;
}

export interface Perfil {
  id: number | null;
  correo: string;
  completo: boolean;
  porcentajeCompletitud: number;
  fechaActualizacion: string | null;
  autorizacionAceptada: boolean;
  informacionPersonal: InformacionPersonal | null;
  situacionVictima: SituacionVictima | null;
  situacionSocioeconomica: SituacionSocioeconomica | null;
  condicionAccesibilidad: CondicionAccesibilidad | null;
  trayectoriaEducativa: TrayectoriaEducativa | null;
  intereses: InteresProfesional[];
}

/** Respuesta de GET /api/catalogos */
export interface Catalogos {
  areasConocimiento: string[];
  hechosVictimizantes: string[];
  gruposSisben: string[];
  mediosTransporte: string[];
  situacionesLaborales: string[];
  nivelesEducativos: string[];
  modalidades: string[];
  tiposDocumento: string[];
  zonas: string[];
  estadosEstudio: string[];
  tiposInstitucion: string[];
  tiposBeneficio: string[];
  tiposRequisito: string[];
}
