import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Beneficio, BeneficioRequest, Institucion, Programa, Requisito } from '../../../models/oferta.model';
import { Catalogos } from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { OfertaService } from '../../../services/oferta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-16 Gestionar beneficios (becas, créditos condonables, subsidios, descuentos). */
@Component({
  selector: 'app-beneficios',
  standalone: true,
  imports: [FormsModule, DatePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Beneficios</h1>
    <app-alerta [mensaje]="error" />

    <div class="card">
      <h3>{{ editandoId ? 'Editar beneficio' : 'Nuevo beneficio' }}</h3>
      <form #f="ngForm" (ngSubmit)="guardar()">
        <div class="grid-2">
          <div class="campo"><label>Nombre *</label><input name="nombre" [(ngModel)]="form.nombre" required /></div>
          <div class="campo"><label>Tipo *</label>
            <select name="tipo" [(ngModel)]="form.tipo" required>
              @for (t of catalogos?.tiposBeneficio ?? []; track t) { <option [value]="t">{{ t | etiqueta }}</option> }
            </select></div>
          <div class="campo"><label>Institución que lo otorga *</label>
            <select name="inst" [(ngModel)]="form.institucionId" required>
              @for (i of instituciones; track i.id) { <option [ngValue]="i.id">{{ i.nombre }}</option> }
            </select></div>
          <div class="campo"><label>Cobertura (%) *</label>
            <input type="number" min="0" max="100" name="cob" [(ngModel)]="form.porcentajeCobertura" required /></div>
          <div class="campo"><label>Fecha límite</label>
            <input type="date" name="limite" [(ngModel)]="form.fechaLimite" /></div>
          <div class="campo"><label>Descripción</label><input name="desc" [(ngModel)]="form.descripcion" /></div>
        </div>
        <label class="check"><input type="checkbox" name="vict" [(ngModel)]="form.exclusivoVictimas" /> Exclusivo para víctimas</label>

        <h3>Programas a los que aplica</h3>
        <div class="grid-2">
          @for (p of programas; track p.id) {
            <label class="check"><input type="checkbox" [checked]="form.programaIds.includes(p.id)"
                   (change)="form.programaIds = alternar(form.programaIds, p.id)" /> {{ p.nombre }}</label>
          }
        </div>
        <h3>Requisitos que exige</h3>
        <div class="grid-2">
          @for (r of requisitos; track r.id) {
            <label class="check"><input type="checkbox" [checked]="form.requisitoIds.includes(r.id!)"
                   (change)="form.requisitoIds = alternar(form.requisitoIds, r.id!)" /> {{ r.nombre }}</label>
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
        <thead><tr><th>Beneficio</th><th>Tipo</th><th>Otorga</th><th>Cobertura</th><th>Vigencia</th><th></th></tr></thead>
        <tbody>
          @for (b of lista; track b.id) {
            <tr>
              <td>{{ b.nombre }} @if (b.exclusivoVictimas) { <span class="badge">Víctimas</span> }
                <div class="suave">{{ b.programaIds.length }} programa(s)</div></td>
              <td>{{ b.tipo | etiqueta }}</td>
              <td>{{ b.institucionNombre }}</td>
              <td>{{ b.porcentajeCobertura }}%</td>
              <td><span class="badge" [class.badge-ok]="b.vigente" [class.badge-error]="!b.vigente">
                {{ b.vigente ? 'Vigente' : 'Vencido' }}</span> {{ b.fechaLimite | date: 'dd/MM/yy' }}</td>
              <td class="fila">
                <button class="btn btn-sec btn-chico" (click)="editar(b)">Editar</button>
                <button class="btn btn-peligro btn-chico" (click)="eliminar(b)">Eliminar</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class BeneficiosComponent implements OnInit {
  private oferta = inject(OfertaService);
  private catalogoService = inject(CatalogoService);
  lista: Beneficio[] = [];
  instituciones: Institucion[] = [];
  programas: Programa[] = [];
  requisitos: Requisito[] = [];
  catalogos: Catalogos | null = null;
  form: BeneficioRequest = this.vacio();
  editandoId: number | null = null;
  error: string | null = null;

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.oferta.instituciones().subscribe((i) => (this.instituciones = i));
    this.oferta.programas().subscribe((p) => (this.programas = p));
    this.oferta.requisitos().subscribe((r) => (this.requisitos = r));
    this.cargar();
  }

  cargar(): void {
    this.oferta.beneficios().subscribe((l) => (this.lista = l));
  }

  alternar(ids: number[], id: number): number[] {
    return ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id];
  }

  nuevo(): void {
    this.form = this.vacio();
    this.editandoId = null;
  }

  editar(b: Beneficio): void {
    this.editandoId = b.id;
    this.form = {
      nombre: b.nombre, tipo: b.tipo, descripcion: b.descripcion, porcentajeCobertura: b.porcentajeCobertura,
      exclusivoVictimas: b.exclusivoVictimas, fechaLimite: b.fechaLimite, institucionId: b.institucionId,
      programaIds: [...b.programaIds], requisitoIds: b.requisitos.map((r) => r.id!),
    };
  }

  guardar(): void {
    this.error = null;
    const datos = { ...this.form, fechaLimite: this.form.fechaLimite || undefined };
    const peticion = this.editandoId
      ? this.oferta.actualizarBeneficio(this.editandoId, datos)
      : this.oferta.crearBeneficio(datos);
    peticion.subscribe({
      next: () => {
        this.nuevo();
        this.cargar();
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  eliminar(b: Beneficio): void {
    if (!confirm(`¿Eliminar ${b.nombre}?`)) return;
    this.oferta.eliminarBeneficio(b.id).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  private vacio(): BeneficioRequest {
    return {
      nombre: '', tipo: 'BECA', descripcion: '', porcentajeCobertura: 100, exclusivoVictimas: false,
      institucionId: null, programaIds: [], requisitoIds: [],
    };
  }
}
