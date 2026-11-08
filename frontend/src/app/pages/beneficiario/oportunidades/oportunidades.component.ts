import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AlertaComponent } from '../../../components/alerta.component';
import { Oportunidad, Programa } from '../../../models/oferta.model';
import { Catalogos } from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { FiltrosPrograma, OfertaService } from '../../../services/oferta.service';
import { PostulacionService } from '../../../services/postulacion.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/**
 * HU-12 Consultar programas educativos con filtros.
 * Auditoría - Plan de mejoramiento #2: catálogo libre. Muestra TODA la oferta vigente (sin filtrar por los
 * intereses de la caracterización) y permite postularse a cualquier convocatoria.
 */
@Component({
  selector: 'app-oportunidades',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Oferta educativa</h1>
    <p class="suave">Consulta los programas disponibles. Para recibir recomendaciones personalizadas genera tu ruta.</p>
    <div class="aviso-catalogo">
      Aquí ves <strong>toda la oferta educativa</strong>, no solo la que coincide con tus intereses. Usa los filtros
      para explorar y, si un programa te interesa, presiona <strong>“Postularme”</strong> en su convocatoria.
      Solo podrás postularte si cumples sus <strong>requisitos</strong> (nivel educativo, edad, etc.).
      Tus recomendaciones personalizadas siguen en <a routerLink="/beneficiario/rutas">Mi ruta</a>.
    </div>

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
          <div class="fila entre convocatoria">
            <p class="suave">Convocatoria {{ o.periodoAcademico }}: inscripciones hasta
              <strong>{{ o.fechaCierreInscripcion | date: 'dd/MM/yyyy' }}</strong> ({{ o.diasParaCierre }} días) · {{ o.cupos }} cupos</p>
            @if (postulacionActiva(o.id); as post) {
              <a class="btn btn-sec" [routerLink]="['/beneficiario/postulaciones', post]">Ver mi postulación</a>
            } @else {
              @if (motivoNoElegible(o.id); as motivo) {
                <!-- No cumple los requisitos: se explica el motivo y no se permite postular -->
                <div class="no-elegible">
                  <button class="btn" disabled>Postularme</button>
                  <span>No puedes postularte: {{ motivo }}</span>
                </div>
              } @else {
                <button class="btn" (click)="postularme(o)" [disabled]="postulandoId !== null">
                  {{ postulandoId === o.id ? 'Creando postulación...' : 'Postularme' }}
                </button>
              }
            }
          </div>
        } @empty {
          <p class="suave">Este programa no tiene convocatorias abiertas en este momento.</p>
        }
      </div>
    } @empty {
      <p class="vacio">No se encontraron programas con esos filtros. Presiona “Limpiar” para ver toda la oferta.</p>
    }
  `,
  styles: [`
    .aviso-catalogo { background: var(--azul-palido); border: 1px solid var(--marino); color: var(--marino);
      border-radius: 8px; padding: 10px 14px; margin: 8px 0 16px; font-size: 0.9rem; }
    .convocatoria { border-top: 1px dashed var(--azul-claro); padding-top: 8px; margin-top: 8px; }
    .convocatoria p { margin: 0; }
    .no-elegible { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; max-width: 320px; }
    .no-elegible span { color: var(--error); font-size: 0.82rem; text-align: right; }
  `],
})
export class OportunidadesComponent implements OnInit {
  private oferta = inject(OfertaService);
  private catalogoService = inject(CatalogoService);
  private postulacionService = inject(PostulacionService);
  private router = inject(Router);

  catalogos: Catalogos | null = null;
  programas: Programa[] = [];
  oportunidades: Oportunidad[] = [];
  filtros: FiltrosPrograma = { texto: '', nivel: '', modalidad: '', area: '', municipio: '' };
  error: string | null = null;
  /** oportunidadId -> id de la postulación activa del beneficiario (para no duplicar). */
  private misPostulaciones = new Map<number, number>();
  /** oportunidadId -> motivo por el que NO cumple los requisitos (si cumple, no aparece). */
  private noElegibles = new Map<number, string>();
  postulandoId: number | null = null;

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.oferta.oportunidades(true).subscribe((o) => (this.oportunidades = o));
    this.postulacionService.listar().subscribe((lista) => {
      lista
        .filter((p) => p.estado !== 'CERRADA' && p.estado !== 'NO_ADMITIDA')
        .forEach((p) => this.misPostulaciones.set(p.oportunidad.id, p.id));
    });
    this.postulacionService.elegibilidad().subscribe((lista) => {
      lista.filter((e) => !e.puedePostularse).forEach((e) => this.noElegibles.set(e.oportunidadId, e.motivo ?? ''));
    });
    this.buscar();
  }

  motivoNoElegible(oportunidadId: number): string | null {
    return this.noElegibles.get(oportunidadId) ?? null;
  }

  postulacionActiva(oportunidadId: number): number | null {
    return this.misPostulaciones.get(oportunidadId) ?? null;
  }

  /** Auditoría - Plan de mejoramiento #2: postulación directa desde el catálogo. */
  postularme(o: Oportunidad): void {
    this.error = null;
    this.postulandoId = o.id;
    this.postulacionService.postularDesdeCatalogo(o.id).subscribe({
      next: (p) => this.router.navigate(['/beneficiario/postulaciones', p.id]),
      error: (e) => {
        this.error = mensajeError(e);
        this.postulandoId = null;
      },
    });
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
