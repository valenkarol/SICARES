import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AlertaComponent } from '../../../components/alerta.component';
import { Requisito } from '../../../models/oferta.model';
import { DocumentoPostulacion, Postulacion } from '../../../models/postulacion.model';
import { PostulacionService } from '../../../services/postulacion.service';
import { EtiquetaPipe, abrirBlob } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/**
 * Seguimiento de una postulación:
 * HU-21 requisitos y fechas, HU-22 documentos, HU-23 registrar, HU-24 estado e historial, HU-26 recordatorios.
 */
@Component({
  selector: 'app-seguimiento',
  standalone: true,
  imports: [RouterLink, DatePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <a routerLink="/beneficiario/postulaciones" class="suave">← Mis postulaciones</a>
    @if (p) {
      <div class="fila entre">
        <h1>{{ p.oportunidad.programaNombre }}</h1>
        <span class="badge">{{ p.estado | etiqueta }}</span>
      </div>
      <p class="suave">{{ p.oportunidad.institucionNombre }} · Periodo {{ p.oportunidad.periodoAcademico }}</p>

      <app-alerta [mensaje]="error" />
      <app-alerta tipo="ok" [mensaje]="exito" />

      <div class="grid">
        <!-- HU-21 fechas -->
        <div class="card">
          <h3>Fechas importantes</h3>
          <p>Cierre de inscripciones: <strong>{{ p.oportunidad.fechaCierreInscripcion | date: 'dd/MM/yyyy' }}</strong>
            ({{ p.oportunidad.diasParaCierre }} días)</p>
          <p>Admisión: {{ p.oportunidad.fechaAdmision | date: 'dd/MM/yyyy' }}</p>
          <p>Matrícula: {{ p.oportunidad.fechaMatricula | date: 'dd/MM/yyyy' }}</p>
        </div>

        <!-- HU-26 recordatorios -->
        <div class="card">
          <h3>Recordatorios</h3>
          @for (r of p.recordatorios; track r.id) {
            <p>
              <span class="badge" [class.badge-ok]="r.enviado">{{ r.enviado ? 'Enviado' : 'Programado' }}</span>
              {{ r.titulo }} · {{ r.fechaProgramada | date: 'dd/MM/yyyy' }}
            </p>
          } @empty { <p class="suave">Sin recordatorios.</p> }
        </div>
      </div>

      <!-- HU-21 / HU-22 requisitos y documentos -->
      <div class="card">
        <h2>Requisitos y documentos</h2>
        <div class="tabla-scroll">
          <table class="tabla">
            <thead><tr><th>Requisito</th><th>Tipo</th><th>Documento</th><th></th></tr></thead>
            <tbody>
              @for (r of p.requisitos; track r.id) {
                <tr>
                  <td>{{ r.nombre }} @if (r.obligatorio) { <span class="suave">(obligatorio)</span> }
                    @if (r.descripcion) { <div class="suave">{{ r.descripcion }}</div> }</td>
                  <td>{{ r.tipo | etiqueta }}</td>
                  <td>
                    @if (documentoDe(r); as d) {
                      <span class="badge" [class.badge-ok]="d.estado === 'VALIDADO'" [class.badge-error]="d.estado === 'RECHAZADO'">
                        {{ d.estado | etiqueta }}</span>
                      <a href="" (click)="$event.preventDefault(); ver(d)">{{ d.nombreArchivo }}</a>
                    } @else if (r.requiereDocumento) {
                      <span class="badge badge-aviso">Pendiente</span>
                    } @else {
                      <span class="suave">No requiere archivo</span>
                    }
                  </td>
                  <td>
                    @if (r.requiereDocumento && puedeEditar) {
                      <label class="btn btn-sec btn-chico">
                        {{ documentoDe(r) ? 'Reemplazar' : 'Cargar' }}
                        <input type="file" hidden accept=".pdf,.jpg,.jpeg,.png" (change)="cargar(r, $event)" />
                      </label>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="suave">Formatos permitidos: PDF, JPG o PNG de máximo 5 MB.</p>

        <div class="fila">
          @if (puedeEditar) {
            <button class="btn" (click)="registrar()" [disabled]="trabajando">Registrar postulación</button>
          }
          @if (!p.finalizada) {
            <button class="btn btn-peligro" (click)="cancelar()" [disabled]="trabajando">Desistir</button>
          }
        </div>
      </div>

      <!-- HU-24 historial -->
      <div class="card">
        <h2>Historial del proceso</h2>
        @for (h of p.historial; track h.id) {
          <div class="hito">
            <strong>{{ h.estadoNuevo | etiqueta }}</strong>
            <span class="suave"> · {{ h.fechaCambio | date: 'dd/MM/yyyy HH:mm' }} · {{ h.responsable }}</span>
            @if (h.observacion) { <div>{{ h.observacion }}</div> }
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .hito { border-left: 3px solid var(--primario); padding: 4px 0 8px 12px; margin-bottom: 8px; }
  `],
})
export class SeguimientoComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private postulacionService = inject(PostulacionService);

  p: Postulacion | null = null;
  error: string | null = null;
  exito: string | null = null;
  trabajando = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.postulacionService.obtener(id).subscribe({
      next: (p) => (this.p = p),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  get puedeEditar(): boolean {
    return !!this.p && (this.p.estado === 'SELECCIONADA' || this.p.estado === 'DOCUMENTOS_PENDIENTES');
  }

  documentoDe(r: Requisito): DocumentoPostulacion | undefined {
    return this.p?.documentos.find((d) => d.requisitoId === r.id);
  }

  cargar(r: Requisito, evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo || !this.p || r.id == null) return;
    if (archivo.size > 5 * 1024 * 1024) {
      this.error = 'El archivo supera los 5 MB';
      return;
    }
    this.ejecutar(this.postulacionService.cargarDocumento(this.p.id, r.id, archivo), 'Documento cargado');
    input.value = '';
  }

  registrar(): void {
    if (!this.p) return;
    this.ejecutar(this.postulacionService.registrar(this.p.id), '');
  }

  cancelar(): void {
    if (!this.p || !confirm('¿Seguro que deseas desistir de esta postulación?')) return;
    this.ejecutar(this.postulacionService.cancelar(this.p.id), 'Postulación cerrada');
  }

  ver(d: DocumentoPostulacion): void {
    if (!this.p) return;
    this.postulacionService.descargarDocumento(this.p.id, d.id).subscribe(abrirBlob);
  }

  private ejecutar(peticion: ReturnType<PostulacionService['registrar']>, mensajeOk: string): void {
    this.error = null;
    this.exito = null;
    this.trabajando = true;
    peticion.subscribe({
      next: (p) => {
        this.p = p;
        this.trabajando = false;
        if (mensajeOk) {
          this.exito = mensajeOk;
        } else if (p.estado === 'POSTULADA') {
          this.exito = '¡Tu postulación fue registrada! Te avisaremos cuando cambie su estado.';
        } else {
          this.error = `Aún tienes ${p.documentosPendientes.length} documento(s) pendiente(s) por cargar.`;
        }
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.trabajando = false;
      },
    });
  }
}
