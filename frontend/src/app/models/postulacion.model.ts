import { Oportunidad, Requisito } from './oferta.model';

export type EstadoPostulacion =
  | 'SELECCIONADA'
  | 'DOCUMENTOS_PENDIENTES'
  | 'POSTULADA'
  | 'ADMITIDA'
  | 'NO_ADMITIDA'
  | 'MATRICULADA'
  | 'CERRADA';

export interface DocumentoPostulacion {
  id: number;
  nombreArchivo: string;
  tipoArchivo: string;
  tamanoBytes: number;
  fechaCarga: string;
  estado: 'PENDIENTE' | 'CARGADO' | 'VALIDADO' | 'RECHAZADO';
  requisitoId: number | null;
  requisitoNombre: string | null;
}

export interface HistorialEstado {
  id: number;
  estadoAnterior: EstadoPostulacion | null;
  estadoNuevo: EstadoPostulacion;
  fechaCambio: string;
  observacion?: string;
  responsable: string;
}

export interface Recordatorio {
  id: number;
  titulo: string;
  mensaje: string;
  fechaEvento: string;
  fechaProgramada: string;
  enviado: boolean;
  postulacionId: number;
}

export interface Postulacion {
  id: number;
  fechaSeleccion: string;
  fechaPostulacion: string | null;
  estado: EstadoPostulacion;
  finalizada: boolean;
  beneficiarioId: number;
  beneficiarioCorreo: string;
  oportunidad: Oportunidad;
  requisitos: Requisito[];
  documentosPendientes: Requisito[];
  documentos: DocumentoPostulacion[];
  historial: HistorialEstado[];
  recordatorios: Recordatorio[];
  siguientesEstados: EstadoPostulacion[];
}

export interface Notificacion {
  id: number;
  asunto: string;
  mensaje: string;
  canal: string;
  fechaEnvio: string;
  leida: boolean;
  postulacionId: number | null;
}
