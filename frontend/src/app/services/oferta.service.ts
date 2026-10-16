import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import {
  Beneficio,
  BeneficioRequest,
  Institucion,
  Oportunidad,
  OportunidadRequest,
  Programa,
  ProgramaRequest,
  Requisito,
} from '../models/oferta.model';

export interface FiltrosPrograma {
  nivel?: string;
  modalidad?: string;
  municipio?: string;
  area?: string;
  texto?: string;
}

/** Oferta educativa (HU-12 a HU-16): lectura para todos, escritura en /admin. */
@Injectable({ providedIn: 'root' })
export class OfertaService {
  private http = inject(HttpClient);

  // ---------- Programas
  programas(filtros: FiltrosPrograma = {}): Observable<Programa[]> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([k, v]) => {
      if (v) params = params.set(k, v);
    });
    return this.http.get<Programa[]>(`${API_URL}/programas`, { params });
  }
  programa(id: number): Observable<Programa> {
    return this.http.get<Programa>(`${API_URL}/programas/${id}`);
  }
  crearPrograma(p: ProgramaRequest): Observable<Programa> {
    return this.http.post<Programa>(`${API_URL}/admin/programas`, p);
  }
  actualizarPrograma(id: number, p: ProgramaRequest): Observable<Programa> {
    return this.http.put<Programa>(`${API_URL}/admin/programas/${id}`, p);
  }
  eliminarPrograma(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/programas/${id}`);
  }

  // ---------- Instituciones
  instituciones(): Observable<Institucion[]> {
    return this.http.get<Institucion[]>(`${API_URL}/instituciones`);
  }
  crearInstitucion(i: Institucion): Observable<Institucion> {
    return this.http.post<Institucion>(`${API_URL}/admin/instituciones`, i);
  }
  actualizarInstitucion(id: number, i: Institucion): Observable<Institucion> {
    return this.http.put<Institucion>(`${API_URL}/admin/instituciones/${id}`, i);
  }
  eliminarInstitucion(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/instituciones/${id}`);
  }

  // ---------- Requisitos
  requisitos(): Observable<Requisito[]> {
    return this.http.get<Requisito[]>(`${API_URL}/requisitos`);
  }
  crearRequisito(r: Requisito): Observable<Requisito> {
    return this.http.post<Requisito>(`${API_URL}/admin/requisitos`, r);
  }
  actualizarRequisito(id: number, r: Requisito): Observable<Requisito> {
    return this.http.put<Requisito>(`${API_URL}/admin/requisitos/${id}`, r);
  }
  eliminarRequisito(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/requisitos/${id}`);
  }

  // ---------- Beneficios
  beneficios(): Observable<Beneficio[]> {
    return this.http.get<Beneficio[]>(`${API_URL}/beneficios`);
  }
  crearBeneficio(b: BeneficioRequest): Observable<Beneficio> {
    return this.http.post<Beneficio>(`${API_URL}/admin/beneficios`, b);
  }
  actualizarBeneficio(id: number, b: BeneficioRequest): Observable<Beneficio> {
    return this.http.put<Beneficio>(`${API_URL}/admin/beneficios/${id}`, b);
  }
  eliminarBeneficio(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/beneficios/${id}`);
  }

  // ---------- Oportunidades (convocatorias)
  oportunidades(vigentes = true, programaId?: number): Observable<Oportunidad[]> {
    let params = new HttpParams().set('vigentes', vigentes);
    if (programaId) params = params.set('programaId', programaId);
    return this.http.get<Oportunidad[]>(`${API_URL}/oportunidades`, { params });
  }
  crearOportunidad(o: OportunidadRequest): Observable<Oportunidad> {
    return this.http.post<Oportunidad>(`${API_URL}/admin/oportunidades`, o);
  }
  actualizarOportunidad(id: number, o: OportunidadRequest): Observable<Oportunidad> {
    return this.http.put<Oportunidad>(`${API_URL}/admin/oportunidades/${id}`, o);
  }
  eliminarOportunidad(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/oportunidades/${id}`);
  }
}
