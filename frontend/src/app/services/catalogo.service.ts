import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_URL } from '../config/api.config';
import { Catalogos } from '../models/perfil.model';

/** Listas para los selects (se piden una sola vez y se reutilizan). */
@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private http = inject(HttpClient);
  private cache$?: Observable<Catalogos>;

  obtener(): Observable<Catalogos> {
    if (!this.cache$) {
      this.cache$ = this.http.get<Catalogos>(`${API_URL}/catalogos`).pipe(shareReplay(1));
    }
    return this.cache$;
  }
}
