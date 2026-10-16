import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Oportunidad, Programa } from '../../../models/oferta.model';
import { Catalogos } from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { FiltrosPrograma, OfertaService } from '../../../services/oferta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-12 Consultar programas educativos con filtros. */
@Component({
  selector: 'app-oportunidades',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Oferta educativa</h1>
    <p class="suave">Consulta los programas disponibles. Para recibir recomendaciones personalizadas genera tu ruta.</p>

    <div class="card">
      <div class="grid-2">
        <div class="campo"><label>Buscar</label>
          <input [(ngModel)]="filtros.texto" placeholder="Nombre del programa" (keyup.enter)="buscar()" /></div>
        <div class="campo"><label>Nivel</label>
          <select [(ngModel)]="filtros.nivel">
            <option value="">Todos</option>
            @for (n of catalogos?.nivelesEducativos ?? []; track n) { <option [value]="n">{{ n | etiqueta }}</option> }
          </select></div>
        <div class="campo"><label>Modalidad</label>
          <select [(ngModel)]="filtros.modalidad">
            <option value="">Todas</option>
            @for (m of catalogos?.modalidades ?? []; track m) { <option [value]="m">{{ m | etiqueta }}</option> }
          </select></div>
        <div class="campo"><label>Área</label>
          <select [(ngModel)]="filtros.area">
            <option value="">Todas</option>
            @for (a of catalogos?.areasConocimiento ?? []; track a) { <option [value]="a">{{ a }}</option> }
          </select></div>
        <div class="campo"><label>Municipio</label>
          <input [(ngModel)]="filtros.municipio" placeholder="Ej. Armenia" /></div>
      </div>
      <div class="fila">
        <button class="btn" (click)="buscar()">Buscar</button>
        <button class="btn btn-sec" (click)="limpiar()">Limpiar</button>
      </div>
    </div>

    <app-alerta [mensaje]="error" />

    @for (p of programas; track p.id) {
      <div class="card">
        <div class="fila entre">
          <h3>{{ p.nombre }}</h3>
          @if (p.gratuito) { <span class="badge badge-ok">Gratuito</span> }
          @else { <span class="badge">{{ p.costoPeriodo | currency: 'COP' : 'symbol-narrow' : '1.0-0' }} / periodo</span> }
        </div>
        <p class="suave">
          {{ p.institucionNombre }} ({{ p.institucionTipo | etiqueta }}) · {{ p.nivel | etiqueta }} ·
          {{ p.modalidad | etiqueta }} · {{ p.municipio }} · {{ p.duracionPeriodos }} periodos · {{ p.areaConocimiento }}
        </p>
        @if (p.requisitos.length) {
          <p><strong>Requisitos:</strong>
            @for (r of p.requisitos; track r.id; let ultimo = $last) { {{ r.nombre }}{{ ultimo ? '' : ', ' }} }
          </p>
        }
        @if (p.beneficios.length) {
          <p><strong>Beneficios:</strong>
            @for (b of p.beneficios; track b.id) {
              <span class="badge badge-aviso">{{ b.nombre }} ({{ b.porcentajeCobertura }}%)</span>&nbsp;
            }
          </p>
        }
        @for (o of convocatoriasDe(p.id); track o.id) {
          <p class="suave">Convocatoria {{ o.periodoAcademico }}: inscripciones hasta
            <strong>{{ o.fechaCierreInscripcion | date: 'dd/MM/yyyy' }}</strong> ({{ o.diasParaCierre }} días) · {{ o.cupos }} cupos</p>
        }
      </div>
    } @empty {
      <p class="vacio">No se encontraron programas con esos filtros.</p>
    }
  `,
})
export class OportunidadesComponent implements OnInit {
  private oferta = inject(OfertaService);
  private catalogoService = inject(CatalogoService);

  catalogos: Catalogos | null = null;
  programas: Programa[] = [];
  oportunidades: Oportunidad[] = [];
  filtros: FiltrosPrograma = { texto: '', nivel: '', modalidad: '', area: '', municipio: '' };
  error: string | null = null;

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.oferta.oportunidades(true).subscribe((o) => (this.oportunidades = o));
    this.buscar();
  }

  buscar(): void {
    this.error = null;
    this.oferta.programas(this.filtros).subscribe({
      next: (p) => (this.programas = p),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  limpiar(): void {
    this.filtros = { texto: '', nivel: '', modalidad: '', area: '', municipio: '' };
    this.buscar();
  }

  convocatoriasDe(programaId: number): Oportunidad[] {
    return this.oportunidades.filter((o) => o.programaId === programaId);
  }
}
