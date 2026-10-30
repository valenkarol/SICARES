import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Catalogos } from '../../../models/perfil.model';
import { DocumentoPostulacion, EstadoPostulacion, Postulacion } from '../../../models/postulacion.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { PostulacionService } from '../../../services/postulacion.service';
import { EtiquetaPipe, abrirBlob } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-25 Actualizar estado de una postulación (con historial) y validar documentos. */
@Component({
  selector: 'app-admin-postulaciones',
  standalone: true,
  imports: [FormsModule, DatePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Postulaciones</h1>
    <app-alerta [mensaje]="error" />
    <app-alerta tipo="ok" [mensaje]="exito" />

    <div class="card fila">
      <div class="campo" style="margin:0">
        <label>Filtrar por estado</label>
        <select [(ngModel)]="filtro" (change)="cargar()">
          <option value="">Todos</option>
          @for (e of catalogos?.estadosPostulacion ?? []; track e) { <option [value]="e">{{ e | etiqueta }}</option> }
        </select>
      </div>
    </div>

    <div class="card tabla-scroll">
      <table class="tabla">
        <thead><tr><th>#</th><th>Beneficiario</th><th>Programa</th><th>Seleccionada</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (p of lista; track p.id) {
            <tr>
              <td>{{ p.id }}</td>
              <td>{{ p.beneficiarioCorreo }}</td>
              <td>{{ p.oportunidad.programaNombre }}<div class="suave">{{ p.oportunidad.institucionNombre }}</div></td>
              <td>{{ p.fechaSeleccion | date: 'dd/MM/yyyy' }}</td>
              <td><span class="badge">{{ p.estado | etiqueta }}</span></td>
              <td><button class="btn btn-sec btn-chico" (click)="abrir(p)">Gestionar</button></td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="vacio">No hay postulaciones.</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (sel) {
      <div class="card">
        <div class="fila entre">
          <h2>Postulación #{{ sel.id }} · {{ sel.oportunidad.programaNombre }}</h2>
          <button class="btn btn-sec btn-chico" (click)="sel = null">Cerrar</button>
        </div>
        <p class="suave">{{ sel.beneficiarioCorreo }} · Estado actual: <strong>{{ sel.estado | etiqueta }}</strong></p>

        <h3>Documentos</h3>
        @for (d of sel.documentos; track d.id) {
          <div class="fila">
            <a href="" (click)="$event.preventDefault(); ver(d)">{{ d.requisitoNombre }} — {{ d.nombreArchivo }}</a>
            <span class="badge">{{ d.estado | etiqueta }}</span>
            <button class="btn btn-sec btn-chico" (click)="estadoDocumento(d, 'VALIDADO')">Validar</button>
            <button class="btn btn-peligro btn-chico" (click)="estadoDocumento(d, 'RECHAZADO')">Rechazar</button>
          </div>
        } @empty { <p class="suave">Sin documentos cargados.</p> }
        @if (sel.documentosPendientes.length) {
          <p class="alerta alerta-aviso">Pendientes: @for (r of sel.documentosPendientes; track r.id) { {{ r.nombre }}; }</p>
        }

        <h3>Cambiar estado</h3>
        @if (sel.siguientesEstados.length) {
          <div class="grid-2">
            <div class="campo"><label>Nuevo estado</label>
              <select [(ngModel)]="nuevoEstado">
                @for (e of sel.siguientesEstados; track e) { <option [value]="e">{{ e | etiqueta }}</option> }
              </select></div>
            <div class="campo"><label>Observación</label><input [(ngModel)]="observacion" maxlength="500" /></div>
          </div>
          <button class="btn" [disabled]="!nuevoEstado" (click)="cambiarEstado()">Actualizar estado</button>
        } @else {
          <p class="suave">La postulación está finalizada.</p>
        }

        <h3>Historial</h3>
        <table class="tabla">
          @for (h of sel.historial; track h.id) {
            <tr>
              <td>{{ h.fechaCambio | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>{{ (h.estadoAnterior | etiqueta) || '—' }} → <strong>{{ h.estadoNuevo | etiqueta }}</strong></td>
              <td>{{ h.responsable }}</td>
              <td>{{ h.observacion }}</td>
            </tr>
          }
        </table>
      </div>
    }
  `,
})
export class AdminPostulacionesComponent implements OnInit {
  private postulacionService = inject(PostulacionService);
  private catalogoService = inject(CatalogoService);

  lista: Postulacion[] = [];
  catalogos: Catalogos | null = null;
  filtro = '';
  sel: Postulacion | null = null;
  nuevoEstado: EstadoPostulacion | '' = '';
  observacion = '';
  error: string | null = null;
  exito: string | null = null;

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.cargar();
  }

  cargar(): void {
    this.postulacionService.listarTodas(this.filtro).subscribe((l) => (this.lista = l));
  }

  abrir(p: Postulacion): void {
    this.sel = p;
    this.nuevoEstado = '';
    this.observacion = '';
    this.error = null;
    this.exito = null;
  }

  cambiarEstado(): void {
    if (!this.sel || !this.nuevoEstado) return;
    this.postulacionService.cambiarEstado(this.sel.id, this.nuevoEstado, this.observacion).subscribe({
      next: (p) => {
        this.exito = `Estado actualizado a ${p.estado}. Se notificó al beneficiario.`;
        this.abrirActualizada(p);
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  estadoDocumento(d: DocumentoPostulacion, estado: 'VALIDADO' | 'RECHAZADO'): void {
    this.postulacionService.cambiarEstadoDocumento(d.id, estado).subscribe({
      next: (p) => this.abrirActualizada(p),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  ver(d: DocumentoPostulacion): void {
    this.postulacionService.descargarDocumentoAdmin(d.id).subscribe(abrirBlob);
  }

  private abrirActualizada(p: Postulacion): void {
    this.sel = p;
    this.nuevoEstado = '';
    this.observacion = '';
    this.cargar();
  }
}
