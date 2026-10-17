import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Institucion } from '../../../models/oferta.model';
import { OfertaService } from '../../../services/oferta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-13 Gestionar instituciones educativas. */
@Component({
  selector: 'app-instituciones',
  standalone: true,
  imports: [FormsModule, AlertaComponent, EtiquetaPipe],
  template: `
    <h1>Instituciones educativas</h1>
    <app-alerta [mensaje]="error" />

    <div class="card">
      <h3>{{ editandoId ? 'Editar institución' : 'Nueva institución' }}</h3>
      <form #f="ngForm" (ngSubmit)="guardar()">
        <div class="grid-2">
          <div class="campo"><label>Nombre *</label><input name="nombre" [(ngModel)]="form.nombre" required /></div>
          <div class="campo"><label>NIT *</label><input name="nit" [(ngModel)]="form.nit" required /></div>
          <div class="campo"><label>Tipo *</label>
            <select name="tipo" [(ngModel)]="form.tipo" required>
              <option value="PUBLICA">Pública</option><option value="PRIVADA">Privada</option>
            </select></div>
          <div class="campo"><label>Departamento *</label><input name="depto" [(ngModel)]="form.departamento" required /></div>
          <div class="campo"><label>Municipio *</label><input name="mpio" [(ngModel)]="form.municipio" required /></div>
          <div class="campo"><label>Correo de contacto</label><input type="email" name="correo" [(ngModel)]="form.correoContacto" /></div>
        </div>
        <div class="fila">
          <button class="btn" [disabled]="f.invalid">Guardar</button>
          @if (editandoId) { <button type="button" class="btn btn-sec" (click)="nuevo()">Cancelar</button> }
        </div>
      </form>
    </div>

    <div class="card tabla-scroll">
      <table class="tabla">
        <thead><tr><th>Nombre</th><th>NIT</th><th>Tipo</th><th>Ubicación</th><th>Programas</th><th></th></tr></thead>
        <tbody>
          @for (i of lista; track i.id) {
            <tr>
              <td>{{ i.nombre }}<div class="suave">{{ i.correoContacto }}</div></td>
              <td>{{ i.nit }}</td>
              <td>{{ i.tipo | etiqueta }}</td>
              <td>{{ i.municipio }}, {{ i.departamento }}</td>
              <td>{{ i.totalProgramas }}</td>
              <td class="fila">
                <button class="btn btn-sec btn-chico" (click)="editar(i)">Editar</button>
                <button class="btn btn-peligro btn-chico" (click)="eliminar(i)">Eliminar</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class InstitucionesComponent implements OnInit {
  private oferta = inject(OfertaService);
  lista: Institucion[] = [];
  form: Institucion = this.vacio();
  editandoId: number | null = null;
  error: string | null = null;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.oferta.instituciones().subscribe((l) => (this.lista = l));
  }

  nuevo(): void {
    this.form = this.vacio();
    this.editandoId = null;
  }

  editar(i: Institucion): void {
    this.form = { ...i };
    this.editandoId = i.id ?? null;
  }

  guardar(): void {
    this.error = null;
    const peticion = this.editandoId
      ? this.oferta.actualizarInstitucion(this.editandoId, this.form)
      : this.oferta.crearInstitucion(this.form);
    peticion.subscribe({
      next: () => {
        this.nuevo();
        this.cargar();
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  eliminar(i: Institucion): void {
    if (!i.id || !confirm(`¿Eliminar ${i.nombre}?`)) return;
    this.oferta.eliminarInstitucion(i.id).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  private vacio(): Institucion {
    return { nombre: '', nit: '', tipo: 'PUBLICA', departamento: 'Quindío', municipio: '', correoContacto: '' };
  }
}
