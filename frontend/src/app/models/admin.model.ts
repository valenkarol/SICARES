import { Rol } from './auth.model';

export interface Usuario {
  id: number;
  correo: string;
  rol: Rol;
  activo: boolean;
  fechaRegistro: string;
  ultimoAcceso: string | null;
  nombreCompleto: string | null;
  porcentajePerfil: number | null;
}

export interface Estadisticas {
  totalBeneficiarios: number;
  perfilesCompletos: number;
  porcentajePerfilesCompletos: number;
  beneficiariosConRuta: number;
  totalPostulaciones: number;
  postulacionesPorEstado: Record<string, number>;
  totalProgramas: number;
  oportunidadesVigentes: number;
  // KPIs del Documento de Definición (procesos 1, 2 y 3)
  porcentajeFormulariosIncompletos: number;
  minutosPromedioCaracterizacion: number;
  porcentajeUsuariosConRuta: number;
  msPromedioGeneracionRuta: number;
  porcentajeRutasSeleccionadas: number;
  porcentajePostulacionesCompletadas: number;
  horasPromedioSeleccionAPostulacion: number;
  porcentajeRecordatoriosAtendidos: number;
}

/** Resultado del correo de prueba (POST /api/admin/correo/prueba). */
export interface ResultadoCorreo {
  enviado: boolean;
  detalle: string;
}
