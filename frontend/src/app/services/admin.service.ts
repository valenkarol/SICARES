import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import { Estadisticas, Usuario } from '../models/admin.model';

/** gestión de usuarios/roles y KPIs. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private url = `${API_URL}/admin`;

  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.url}/usuarios`);
  }

  cambiarActivo(id: number, valor: boolean): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.url}/usuarios/${id}/activo`, null, {
      params: new HttpParams().set('valor', valor),
    });
  }

  crearAdministrador(correo: string, contrasena: string): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.url}/administradores`, { correo, contrasena });
  }

  estadisticas(): Observable<Estadisticas> {
    return this.http.get<Estadisticas>(`${this.url}/estadisticas`);
  }
}
