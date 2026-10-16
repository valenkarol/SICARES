export interface Institucion {
  id?: number;
  nombre: string;
  nit: string;
  tipo: string;
  departamento: string;
  municipio: string;
  correoContacto?: string;
  totalProgramas?: number;
}

export interface Requisito {
  id?: number;
  nombre: string;
  descripcion?: string;
  tipo: string;
  obligatorio: boolean;
  valorEsperado?: string;
  requiereDocumento: boolean;
}

export interface BeneficioResumen {
  id: number;
  nombre: string;
  tipo: string;
  porcentajeCobertura: number;
  exclusivoVictimas: boolean;
  fechaLimite?: string;
}

export interface ProgramaRequest {
  nombre: string;
  nivel: string;
  modalidad: string;
  areaConocimiento: string;
  duracionPeriodos: number;
  costoPeriodo: number;
  municipio: string;
  activo: boolean;
  institucionId: number | null;
  requisitoIds: number[];
}

export interface Programa {
  id: number;
  nombre: string;
  nivel: string;
  modalidad: string;
  areaConocimiento: string;
  duracionPeriodos: number;
  costoPeriodo: number;
  municipio: string;
  activo: boolean;
  gratuito: boolean;
  institucionId: number;
  institucionNombre: string;
  institucionTipo: string;
  requisitos: Requisito[];
  beneficios: BeneficioResumen[];
}

export interface BeneficioRequest {
  nombre: string;
  tipo: string;
  descripcion?: string;
  porcentajeCobertura: number;
  exclusivoVictimas: boolean;
  fechaLimite?: string;
  institucionId: number | null;
  programaIds: number[];
  requisitoIds: number[];
}

export interface Beneficio {
  id: number;
  nombre: string;
  tipo: string;
  descripcion?: string;
  porcentajeCobertura: number;
  exclusivoVictimas: boolean;
  fechaLimite?: string;
  vigente: boolean;
  institucionId: number;
  institucionNombre: string;
  programaIds: number[];
  requisitos: Requisito[];
}

export interface OportunidadRequest {
  periodoAcademico: string;
  fechaAperturaInscripcion: string;
  fechaCierreInscripcion: string;
  fechaAdmision?: string;
  fechaMatricula?: string;
  cupos: number;
  programaId: number | null;
}

export interface Oportunidad {
  id: number;
  periodoAcademico: string;
  fechaAperturaInscripcion: string;
  fechaCierreInscripcion: string;
  fechaAdmision?: string;
  fechaMatricula?: string;
  cupos: number;
  vigente: boolean;
  diasParaCierre: number;
  programaId: number;
  programaNombre: string;
  institucionNombre: string;
  nivel: string;
  modalidad: string;
  municipio: string;
}
