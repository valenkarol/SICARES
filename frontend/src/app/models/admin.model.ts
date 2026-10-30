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
}
