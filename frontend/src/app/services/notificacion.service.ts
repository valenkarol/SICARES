import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import { Notificacion } from '../models/postulacion.model';

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private http = inject(HttpClient);
  private url = `${API_URL}/notificaciones`;

  listar(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(this.url);
  }

  noLeidas(): Observable<{ total: number }> {
    return this.http.get<{ total: number }>(`${this.url}/no-leidas`);
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.patch<Notificacion>(`${this.url}/${id}/leida`, {});
  }
}
