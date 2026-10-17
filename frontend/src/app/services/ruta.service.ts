import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import { RutaEducativa } from '../models/ruta.model';

/** Proceso 2: HU-18 Generar ruta y HU-19 Consultar rutas. */
@Injectable({ providedIn: 'root' })
export class RutaService {
  private http = inject(HttpClient);
  private url = `${API_URL}/beneficiario/rutas`;

  generar(): Observable<RutaEducativa> {
    return this.http.post<RutaEducativa>(`${this.url}/generar`, {});
  }

  /** Devuelve null si el backend responde 204 (aún no hay rutas). */
  actual(): Observable<RutaEducativa | null> {
    return this.http.get<RutaEducativa | null>(`${this.url}/actual`);
  }

  historial(): Observable<RutaEducativa[]> {
    return this.http.get<RutaEducativa[]>(this.url);
  }
}
