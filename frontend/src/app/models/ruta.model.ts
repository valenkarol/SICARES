import { BeneficioResumen, Oportunidad, Programa } from './oferta.model';

export interface Recomendacion {
  id: number;
  orden: number;
  puntajeCompatibilidad: number;
  justificacion: string;
  oportunidad: Oportunidad;
  programa: Programa;
  beneficiosAplicables: BeneficioResumen[];
}

export interface RutaEducativa {
  id: number;
  fechaGeneracion: string;
  estado: 'GENERADA' | 'VIGENTE' | 'DESACTUALIZADA';
  version: number;
  recomendaciones: Recomendacion[];
}
