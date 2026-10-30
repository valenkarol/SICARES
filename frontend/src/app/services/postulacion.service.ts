import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config/api.config';
import { EstadoPostulacion, Postulacion, Recordatorio } from '../models/postulacion.model';

/** Proceso 3: Postulación y seguimiento (HU-20 a HU-26). */
@Injectable({ providedIn: 'root' })
export class PostulacionService {
  private http = inject(HttpClient);
  private url = `${API_URL}/beneficiario`;

  // ---------- Beneficiario
  seleccionar(recomendacionId: number): Observable<Postulacion> {
    return this.http.post<Postulacion>(`${this.url}/postulaciones`, { recomendacionId });
  }

  listar(): Observable<Postulacion[]> {
    return this.http.get<Postulacion[]>(`${this.url}/postulaciones`);
  }

  obtener(id: number): Observable<Postulacion> {
    return this.http.get<Postulacion>(`${this.url}/postulaciones/${id}`);
  }

  cargarDocumento(id: number, requisitoId: number, archivo: File): Observable<Postulacion> {
    const form = new FormData();
    form.append('requisitoId', String(requisitoId));
    form.append('archivo', archivo);
    return this.http.post<Postulacion>(`${this.url}/postulaciones/${id}/documentos`, form);
  }

  descargarDocumento(id: number, docId: number): Observable<Blob> {
    return this.http.get(`${this.url}/postulaciones/${id}/documentos/${docId}/archivo`, { responseType: 'blob' });
  }

  registrar(id: number): Observable<Postulacion> {
    return this.http.post<Postulacion>(`${this.url}/postulaciones/${id}/registrar`, {});
  }

  cancelar(id: number): Observable<Postulacion> {
    return this.http.post<Postulacion>(`${this.url}/postulaciones/${id}/cancelar`, {});
  }

  recordatorios(): Observable<Recordatorio[]> {
    return this.http.get<Recordatorio[]>(`${this.url}/recordatorios`);
  }

  // ---------- Administrador (HU-25)
  listarTodas(estado?: string): Observable<Postulacion[]> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    return this.http.get<Postulacion[]>(`${API_URL}/admin/postulaciones`, { params });
  }

  obtenerAdmin(id: number): Observable<Postulacion> {
    return this.http.get<Postulacion>(`${API_URL}/admin/postulaciones/${id}`);
  }

  cambiarEstado(id: number, estado: EstadoPostulacion, observacion: string): Observable<Postulacion> {
    return this.http.patch<Postulacion>(`${API_URL}/admin/postulaciones/${id}/estado`, { estado, observacion });
  }

  cambiarEstadoDocumento(docId: number, estado: string): Observable<Postulacion> {
    return this.http.patch<Postulacion>(`${API_URL}/admin/documentos/${docId}/estado`, { estado });
  }

  descargarDocumentoAdmin(docId: number): Observable<Blob> {
    return this.http.get(`${API_URL}/admin/documentos/${docId}/archivo`, { responseType: 'blob' });
  }

  procesarRecordatorios(): Observable<{ enviados: number }> {
    return this.http.post<{ enviados: number }>(`${API_URL}/admin/recordatorios/procesar`, {});
  }
}
