import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Requisito } from '../../../models/oferta.model';
import { Catalogos } from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { OfertaService } from '../../../services/oferta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-15 Gestionar requisitos. */
@Component({
  selector: 'app-requisitos',
  standalone: true,
  imports: [FormsModule, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Requisitos</h1>
    <app-alerta [mensaje]="error" />

    <div class="card">
      <h3>{{ editandoId ? 'Editar requisito' : 'Nuevo requisito' }}</h3>
      <form #f="ngForm" (ngSubmit)="guardar()">
        <div class="grid-2">
          <div class="campo"><label>Nombre *</label><input name="nombre" [(ngModel)]="form.nombre" required /></div>
          <div class="campo"><label>Tipo *</label>
            <select name="tipo" [(ngModel)]="form.tipo" required>
              @for (t of catalogos?.tiposRequisito ?? []; track t) { <option [value]="t">{{ t | etiqueta }}</option> }
            </select></div>
          <div class="campo"><label>Valor esperado</label>
            <input name="valor" [(ngModel)]="form.valorEsperado" [placeholder]="ayuda()" />
            <span class="suave">{{ ayuda() }}</span></div>
          <div class="campo"><label>Descripción</label><input name="desc" [(ngModel)]="form.descripcion" /></div>
        </div>
        <label class="check"><input type="checkbox" name="oblig" [(ngModel)]="form.obligatorio" /> Obligatorio</label>
        <label class="check"><input type="checkbox" name="doc" [(ngModel)]="form.requiereDocumento" /> Requiere cargar documento</label>
        <div class="fila">
          <button class="btn" [disabled]="f.invalid">Guardar</button>
          @if (editandoId) { <button type="button" class="btn btn-sec" (click)="nuevo()">Cancelar</button> }
        </div>
      </form>
    </div>

    <div class="card tabla-scroll">
      <table class="tabla">
        <thead><tr><th>Nombre</th><th>Tipo</th><th>Valor esperado</th><th>Obligatorio</th><th>Documento</th><th></th></tr></thead>
        <tbody>
          @for (r of lista; track r.id) {
            <tr>
              <td>{{ r.nombre }}<div class="suave">{{ r.descripcion }}</div></td>
              <td>{{ r.tipo | etiqueta }}</td>
              <td>{{ r.valorEsperado }}</td>
              <td>{{ r.obligatorio ? 'Sí' : 'No' }}</td>
              <td>{{ r.requiereDocumento ? 'Sí' : 'No' }}</td>
              <td class="fila">
                <button class="btn btn-sec btn-chico" (click)="editar(r)">Editar</button>
                <button class="btn btn-peligro btn-chico" (click)="eliminar(r)">Eliminar</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class RequisitosComponent implements OnInit {
  private oferta = inject(OfertaService);
  private catalogoService = inject(CatalogoService);
  lista: Requisito[] = [];
  catalogos: Catalogos | null = null;
  form: Requisito = this.vacio();
  editandoId: number | null = null;
  error: string | null = null;

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.cargar();
  }

  cargar(): void {
    this.oferta.requisitos().subscribe((l) => (this.lista = l));
  }

  /** Explica el formato de valorEsperado según el tipo (igual que en el backend). */
  ayuda(): string {
    switch (this.form.tipo) {
      case 'EDAD': return 'Rango de edad, ej. 15-28';
      case 'NIVEL_EDUCATIVO': return 'Nivel mínimo, ej. MEDIA';
      case 'SISBEN': return 'Grupos permitidos, ej. A,B,C';
      case 'CONDICION_VICTIMA': return 'RUV';
      default: return 'Texto libre (opcional)';
    }
  }

  nuevo(): void {
    this.form = this.vacio();
    this.editandoId = null;
  }

  editar(r: Requisito): void {
    this.form = { ...r };
    this.editandoId = r.id ?? null;
  }

  guardar(): void {
    this.error = null;
    const peticion = this.editandoId
      ? this.oferta.actualizarRequisito(this.editandoId, this.form)
      : this.oferta.crearRequisito(this.form);
    peticion.subscribe({
      next: () => {
        this.nuevo();
        this.cargar();
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  eliminar(r: Requisito): void {
    if (!r.id || !confirm(`¿Eliminar ${r.nombre}?`)) return;
    this.oferta.eliminarRequisito(r.id).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  private vacio(): Requisito {
    return { nombre: '', tipo: 'DOCUMENTO', obligatorio: true, requiereDocumento: true, valorEsperado: '', descripcion: '' };
  }
}
