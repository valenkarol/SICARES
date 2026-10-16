import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/auth.model';

/** Convierte un error HTTP en un mensaje legible para el usuario. */
export function mensajeError(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'No se pudo conectar con el servidor. ¿Está encendido el backend?';
    const api = err.error as Partial<ApiError> | null;
    if (api?.campos && Object.keys(api.campos).length > 0) {
      return Object.entries(api.campos)
        .map(([campo, msg]) => `${campo}: ${msg}`)
        .join(' · ');
    }
    if (api?.mensaje) return api.mensaje;
    return `Error ${err.status}`;
  }
  return 'Ocurrió un error inesperado';
}
