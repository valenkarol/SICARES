import { environment } from '../../environments/environment';

/** URL base del backend Spring Boot. */
export const API_URL = environment.apiUrl;

/** Claves usadas en localStorage para la sesión. */
export const STORAGE_TOKEN = 'sicares_token';
export const STORAGE_USUARIO = 'sicares_usuario';
