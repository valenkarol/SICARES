import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL, STORAGE_TOKEN, STORAGE_USUARIO } from '../config/api.config';
import { AuthResponse, LoginRequest, RegistroRequest, Rol } from '../models/auth.model';

/** HU-01 / HU-02: registro, inicio y cierre de sesión. Guarda el token JWT en localStorage. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private url = `${API_URL}/auth`;

  registrar(datos: RegistroRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/registro`, datos).pipe(tap((r) => this.guardarSesion(r)));
  }

  login(datos: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/login`, datos).pipe(tap((r) => this.guardarSesion(r)));
  }

  cambiarContrasena(actual: string, nueva: string): Observable<void> {
    return this.http.put<void>(`${this.url}/contrasena`, { actual, nueva });
  }

  logout(): void {
    localStorage.removeItem(STORAGE_TOKEN);
    localStorage.removeItem(STORAGE_USUARIO);
    this.router.navigate(['/login']);
  }

  get token(): string | null {
    return localStorage.getItem(STORAGE_TOKEN);
  }

  get usuario(): AuthResponse | null {
    const raw = localStorage.getItem(STORAGE_USUARIO);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  }

  get rol(): Rol | null {
    return this.usuario?.rol ?? null;
  }

  estaAutenticado(): boolean {
    return !!this.token;
  }

  /** Ruta de inicio según el rol. */
  rutaInicio(): string {
    return this.rol === 'ADMINISTRADOR' ? '/admin' : '/beneficiario';
  }

  private guardarSesion(r: AuthResponse): void {
    localStorage.setItem(STORAGE_TOKEN, r.token);
    localStorage.setItem(STORAGE_USUARIO, JSON.stringify({ ...r, token: undefined }));
  }
}
