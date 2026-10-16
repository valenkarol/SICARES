import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Institucion, Programa, ProgramaRequest, Requisito } from '../../../models/oferta.model';
import { Catalogos } from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { OfertaService } from '../../../services/oferta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-14 Gestionar programas educativos (relación institución-programa y programa-requisito). */
@Component({
  selector: 'app-programas',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Programas educativos</h1>
    <app-alerta [mensaje]="error" />

    <div class="card">
      <h3>{{ editandoId ? 'Editar programa' : 'Nuevo programa' }}</h3>
      <form #f="ngForm" (ngSubmit)="guardar()">
        <div class="grid-2">
          <div class="campo"><label>Nombre *</label><input name="nombre" [(ngModel)]="form.nombre" required /></div>
          <div class="campo"><label>Institución *</label>
            <select name="inst" [(ngModel)]="form.institucionId" required>
              @for (i of instituciones; track i.id) { <option [ngValue]="i.id">{{ i.nombre }}</option> }
            </select></div>
          <div class="campo"><label>Nivel *</label>
            <select name="nivel" [(ngModel)]="form.nivel" required>
              @for (n of catalogos?.nivelesEducativos ?? []; track n) { <option [value]="n">{{ n | etiqueta }}</option> }
            </select></div>
          <div class="campo"><label>Modalidad *</label>
            <select name="modalidad" [(ngModel)]="form.modalidad" required>
              @for (m of catalogos?.modalidades ?? []; track m) { <option [value]="m">{{ m | etiqueta }}</option> }
            </select></div>
          <div class="campo"><label>Área de conocimiento *</label>
            <select name="area" [(ngModel)]="form.areaConocimiento" required>
              @for (a of catalogos?.areasConocimiento ?? []; track a) { <option [value]="a">{{ a }}</option> }
            </select></div>
          <div class="campo"><label>Duración (periodos) *</label>
            <input type="number" min="1" name="dur" [(ngModel)]="form.duracionPeriodos" required /></div>
          <div class="campo"><label>Costo por periodo (0 = gratuito) *</label>
            <input type="number" min="0" name="costo" [(ngModel)]="form.costoPeriodo" required /></div>
          <div class="campo"><label>Municipio *</label><input name="mpio" [(ngModel)]="form.municipio" required /></div>
        </div>
        <label class="check"><input type="checkbox" name="activo" [(ngModel)]="form.activo" /> Programa activo</label>

        <h3>Requisitos que exige</h3>
        <div class="grid-2">
          @for (r of requisitos; track r.id) {
            <label class="check">
              <input type="checkbox" [checked]="form.requisitoIds.includes(r.id!)" (change)="alternar(r.id!)" />
              {{ r.nombre }} <span class="suave">({{ r.tipo | etiqueta }})</span>
            </label>
          }
        </div>
        <div class="fila">
          <button class="btn" [disabled]="f.invalid">Guardar</button>
          @if (editandoId) { <button type="button" class="btn btn-sec" (click)="nuevo()">Cancelar</button> }
        </div>
      </form>
    </div>

    <div class="card tabla-scroll">
      <table class="tabla">
        <thead><tr><th>Programa</th><th>Institución</th><th>Nivel / modalidad</th><th>Costo</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (p of lista; track p.id) {
            <tr>
              <td>{{ p.nombre }}<div class="suave">{{ p.areaConocimiento }} · {{ p.municipio }}</div></td>
              <td>{{ p.institucionNombre }}</td>
              <td>{{ p.nivel | etiqueta }}<div class="suave">{{ p.modalidad | etiqueta }}</div></td>
              <td>{{ p.gratuito ? 'Gratuito' : (p.costoPeriodo | currency: 'COP' : 'symbol-narrow' : '1.0-0') }}</td>
              <td><span class="badge" [class.badge-ok]="p.activo" [class.badge-error]="!p.activo">{{ p.activo ? 'Activo' : 'Inactivo' }}</span></td>
              <td class="fila">
                <button class="btn btn-sec btn-chico" (click)="editar(p)">Editar</button>
                <button class="btn btn-peligro btn-chico" (click)="eliminar(p)">Eliminar</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class ProgramasComponent implements OnInit {
  private oferta = inject(OfertaService);
  private catalogoService = inject(CatalogoService);

  lista: Programa[] = [];
  instituciones: Institucion[] = [];
  requisitos: Requisito[] = [];
  catalogos: Catalogos | null = null;
  form: ProgramaRequest = this.vacio();
  editandoId: number | null = null;
  error: string | null = null;

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.oferta.instituciones().subscribe((i) => (this.instituciones = i));
    this.oferta.requisitos().subscribe((r) => (this.requisitos = r));
    this.cargar();
  }

  cargar(): void {
    this.oferta.programas().subscribe((l) => (this.lista = l));
  }

  alternar(id: number): void {
    const ids = this.form.requisitoIds;
    this.form.requisitoIds = ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
  }

  nuevo(): void {
    this.form = this.vacio();
    this.editandoId = null;
  }

  editar(p: Programa): void {
    this.editandoId = p.id;
    this.form = {
      nombre: p.nombre, nivel: p.nivel, modalidad: p.modalidad, areaConocimiento: p.areaConocimiento,
      duracionPeriodos: p.duracionPeriodos, costoPeriodo: p.costoPeriodo, municipio: p.municipio, activo: p.activo,
      institucionId: p.institucionId, requisitoIds: p.requisitos.map((r) => r.id!),
    };
  }

  guardar(): void {
    this.error = null;
    const peticion = this.editandoId
      ? this.oferta.actualizarPrograma(this.editandoId, this.form)
      : this.oferta.crearPrograma(this.form);
    peticion.subscribe({
      next: () => {
        this.nuevo();
        this.cargar();
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  eliminar(p: Programa): void {
    if (!confirm(`¿Eliminar ${p.nombre}? Si ya tiene postulaciones, mejor desactívalo.`)) return;
    this.oferta.eliminarPrograma(p.id).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  private vacio(): ProgramaRequest {
    return {
      nombre: '', nivel: 'TECNICO', modalidad: 'PRESENCIAL', areaConocimiento: '', duracionPeriodos: 1,
      costoPeriodo: 0, municipio: '', activo: true, institucionId: null, requisitoIds: [],
    };
  }
}
