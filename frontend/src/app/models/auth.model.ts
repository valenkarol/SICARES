export type Rol = 'BENEFICIARIO' | 'ADMINISTRADOR';

export interface RegistroRequest {
  correo: string;
  contrasena: string;
  aceptaTratamientoDatos: boolean;
}

export interface LoginRequest {
  correo: string;
  contrasena: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  correo: string;
  rol: Rol;
}

/** Formato de error que devuelve el backend (GlobalExceptionHandler). */
export interface ApiError {
  fecha: string;
  estado: number;
  error: string;
  mensaje: string;
  campos: Record<string, string>;
}
