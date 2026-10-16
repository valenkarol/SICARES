import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Oportunidad, OportunidadRequest, Programa } from '../../../models/oferta.model';
import { OfertaService } from '../../../services/oferta.service';
import { mensajeError } from '../../../utils/errores';

/** Convocatorias (OportunidadEducativa): periodo, fechas y cupos de cada programa. */
@Component({
  selector: 'app-admin-oportunidades',
  standalone: true,
  imports: [FormsModule, DatePipe, AlertaComponent],
  template: `
    <h1>Convocatorias</h1>
    <app-alerta [mensaje]="error" />

    <div class="card">
      <h3>{{ editandoId ? 'Editar convocatoria' : 'Nueva convocatoria' }}</h3>
      <form #f="ngForm" (ngSubmit)="guardar()">
        <div class="grid-2">
          <div class="campo"><label>Programa *</label>
            <select name="prog" [(ngModel)]="form.programaId" required>
              @for (p of programas; track p.id) { <option [ngValue]="p.id">{{ p.nombre }} — {{ p.institucionNombre }}</option> }
            </select></div>
          <div class="campo"><label>Periodo académico *</label>
            <input name="periodo" [(ngModel)]="form.periodoAcademico" placeholder="2027-1" required /></div>
          <div class="campo"><label>Apertura de inscripciones *</label>
            <input type="date" name="ap" [(ngModel)]="form.fechaAperturaInscripcion" required /></div>
          <div class="campo"><label>Cierre de inscripciones *</label>
            <input type="date" name="ci" [(ngModel)]="form.fechaCierreInscripcion" required /></div>
          <div class="campo"><label>Publicación de admitidos</label>
            <input type="date" name="adm" [(ngModel)]="form.fechaAdmision" /></div>
          <div class="campo"><label>Matrícula</label>
            <input type="date" name="mat" [(ngModel)]="form.fechaMatricula" /></div>
          <div class="campo"><label>Cupos *</label>
            <input type="number" min="0" name="cupos" [(ngModel)]="form.cupos" required /></div>
        </div>
        <div class="fila">
          <button class="btn" [disabled]="f.invalid">Guardar</button>
          @if (editandoId) { <button type="button" class="btn btn-sec" (click)="nuevo()">Cancelar</button> }
        </div>
      </form>
    </div>

    <div class="card tabla-scroll">
      <label class="check"><input type="checkbox" [(ngModel)]="soloVigentes" (change)="cargar()" /> Solo vigentes</label>
      <table class="tabla">
        <thead><tr><th>Programa</th><th>Periodo</th><th>Inscripciones</th><th>Admisión / matrícula</th><th>Cupos</th><th></th></tr></thead>
        <tbody>
          @for (o of lista; track o.id) {
            <tr>
              <td>{{ o.programaNombre }}<div class="suave">{{ o.institucionNombre }}</div></td>
              <td>{{ o.periodoAcademico }}</td>
              <td>{{ o.fechaAperturaInscripcion | date: 'dd/MM/yy' }} – {{ o.fechaCierreInscripcion | date: 'dd/MM/yy' }}
                <div><span class="badge" [class.badge-ok]="o.vigente" [class.badge-error]="!o.vigente">{{ o.vigente ? 'Abierta' : 'Cerrada' }}</span></div></td>
              <td>{{ o.fechaAdmision | date: 'dd/MM/yy' }} / {{ o.fechaMatricula | date: 'dd/MM/yy' }}</td>
              <td>{{ o.cupos }}</td>
              <td class="fila">
                <button class="btn btn-sec btn-chico" (click)="editar(o)">Editar</button>
                <button class="btn btn-peligro btn-chico" (click)="eliminar(o)">Eliminar</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminOportunidadesComponent implements OnInit {
  private oferta = inject(OfertaService);
  lista: Oportunidad[] = [];
  programas: Programa[] = [];
  form: OportunidadRequest = this.vacio();
  editandoId: number | null = null;
  soloVigentes = false;
  error: string | null = null;

  ngOnInit(): void {
    this.oferta.programas().subscribe((p) => (this.programas = p));
    this.cargar();
  }

  cargar(): void {
    this.oferta.oportunidades(this.soloVigentes).subscribe((l) => (this.lista = l));
  }

  nuevo(): void {
    this.form = this.vacio();
    this.editandoId = null;
  }

  editar(o: Oportunidad): void {
    this.editandoId = o.id;
    this.form = {
      programaId: o.programaId, periodoAcademico: o.periodoAcademico,
      fechaAperturaInscripcion: o.fechaAperturaInscripcion, fechaCierreInscripcion: o.fechaCierreInscripcion,
      fechaAdmision: o.fechaAdmision, fechaMatricula: o.fechaMatricula, cupos: o.cupos,
    };
  }

  guardar(): void {
    this.error = null;
    const datos: OportunidadRequest = {
      ...this.form,
      fechaAdmision: this.form.fechaAdmision || undefined,
      fechaMatricula: this.form.fechaMatricula || undefined,
    };
    const peticion = this.editandoId
      ? this.oferta.actualizarOportunidad(this.editandoId, datos)
      : this.oferta.crearOportunidad(datos);
    peticion.subscribe({
      next: () => {
        this.nuevo();
        this.cargar();
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  eliminar(o: Oportunidad): void {
    if (!confirm('¿Eliminar esta convocatoria?')) return;
    this.oferta.eliminarOportunidad(o.id).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  private vacio(): OportunidadRequest {
    return {
      programaId: null, periodoAcademico: '', fechaAperturaInscripcion: '', fechaCierreInscripcion: '', cupos: 30,
    };
  }
}
