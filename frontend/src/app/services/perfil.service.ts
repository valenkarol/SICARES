import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import {
  CondicionAccesibilidad,
  InformacionPersonal,
  InteresProfesional,
  Perfil,
  SituacionSocioeconomica,
  SituacionVictima,
  TrayectoriaEducativa,
} from '../models/perfil.model';

/** Proceso 1: Caracterización (HU-04 a HU-11). */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private http = inject(HttpClient);
  private url = `${API_URL}/beneficiario/perfil`;

  consultar(): Observable<Perfil> {
    return this.http.get<Perfil>(this.url);
  }

  aceptarAutorizacion(): Observable<Perfil> {
    return this.http.post<Perfil>(`${this.url}/autorizacion`, {});
  }

  guardarInformacionPersonal(d: InformacionPersonal): Observable<Perfil> {
    return this.http.put<Perfil>(`${this.url}/informacion-personal`, d);
  }

  guardarSituacionVictima(d: SituacionVictima): Observable<Perfil> {
    return this.http.put<Perfil>(`${this.url}/situacion-victima`, d);
  }

  guardarSituacionSocioeconomica(d: SituacionSocioeconomica): Observable<Perfil> {
    return this.http.put<Perfil>(`${this.url}/situacion-socioeconomica`, d);
  }

  guardarTrayectoria(d: TrayectoriaEducativa): Observable<Perfil> {
    return this.http.put<Perfil>(`${this.url}/trayectoria`, d);
  }

  guardarIntereses(intereses: InteresProfesional[]): Observable<Perfil> {
    return this.http.put<Perfil>(`${this.url}/intereses`, { intereses });
  }

  guardarAccesibilidad(d: CondicionAccesibilidad): Observable<Perfil> {
    return this.http.put<Perfil>(`${this.url}/accesibilidad`, d);
  }
}
