import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AlertaComponent } from '../../../components/alerta.component';
import { Recomendacion, RutaEducativa } from '../../../models/ruta.model';
import { PostulacionService } from '../../../services/postulacion.service';
import { RutaService } from '../../../services/ruta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-18 Generar ruta recomendada, HU-19 Consultar rutas y HU-20 Seleccionar oportunidad. */
@Component({
  selector: 'app-rutas-educativas',
  standalone: true,
  imports: [RouterLink, DatePipe, DecimalPipe, CurrencyPipe, EtiquetaPipe, AlertaComponent],
  template: `
    <div class="fila entre">
      <h1>Mi ruta educativa</h1>
      <button class="btn" (click)="generar()" [disabled]="cargando">
        {{ cargando ? 'Analizando tu perfil...' : (ruta ? 'Generar nueva ruta' : 'Generar mi ruta') }}
      </button>
    </div>

    <app-alerta [mensaje]="error" />
    @if (ruta && sinCompatibles) {
      <!-- Con el perfil completo la ruta se regenera sola; si sigue desactualizada es porque ninguna oferta coincide.
           Las recomendaciones viejas ya no aplican, así que no se muestran. -->
      <div class="card vacio">
        <h3>No hay oportunidades compatibles con tu perfil actual</h3>
        <p>Ninguna oferta vigente coincide a la vez con tus <strong>áreas de interés</strong>, tu nivel educativo y
          tus condiciones. Prueba elegir otras áreas en
          <a routerLink="/beneficiario/caracterizacion">Mi perfil → Intereses</a> o revisa toda la
          <a routerLink="/beneficiario/oportunidades">oferta educativa</a>. Te avisaremos cuando haya una
          oportunidad para ti.</p>
        <!-- Auditoría - Plan de mejoramiento #2: salida directa al catálogo libre -->
        <p>También puedes <strong>postularte a cualquier programa</strong>, aunque no esté entre tus intereses.</p>
        <a class="btn" routerLink="/beneficiario/oportunidades">Explorar toda la oferta y postularme</a>
      </div>
    } @else if (ruta) {
      <p class="suave">Versión {{ ruta.version }} · generada el {{ ruta.fechaGeneracion | date: 'dd/MM/yyyy HH:mm' }}
        · se actualiza automáticamente cuando cambias tu perfil</p>
      <!-- Auditoría - Plan de mejoramiento #2 -->
      <p class="suave">¿Buscas algo distinto? <a routerLink="/beneficiario/oportunidades">Explora toda la oferta
        educativa</a> y postúlate a cualquier programa, aunque no esté entre tus intereses.</p>

      @for (r of ruta.recomendaciones; track r.id) {
        <div class="card recomendacion">
          <div class="orden">{{ r.orden }}</div>
          <div class="cuerpo">
            <div class="fila entre">
              <h3>{{ r.programa.nombre }}</h3>
              <span class="badge badge-ok">{{ r.puntajeCompatibilidad | number: '1.0-1' }}% compatible</span>
            </div>
            <p class="suave">
              {{ r.programa.institucionNombre }} · {{ r.programa.nivel | etiqueta }} · {{ r.programa.modalidad | etiqueta }}
              · {{ r.programa.municipio }} ·
              @if (r.programa.gratuito) { Gratuito } @else { {{ r.programa.costoPeriodo | currency: 'COP' : 'symbol-narrow' : '1.0-0' }}/periodo }
            </p>
            <p>{{ r.justificacion }}</p>
            <p><strong>Inscripciones:</strong> {{ r.oportunidad.fechaAperturaInscripcion | date: 'dd/MM' }} al
              {{ r.oportunidad.fechaCierreInscripcion | date: 'dd/MM/yyyy' }} · Admisión:
              {{ r.oportunidad.fechaAdmision | date: 'dd/MM/yyyy' }} · Matrícula: {{ r.oportunidad.fechaMatricula | date: 'dd/MM/yyyy' }}</p>
            @if (r.programa.requisitos.length) {
              <p><strong>Requisitos:</strong>
                @for (q of r.programa.requisitos; track q.id; let ultimo = $last) { {{ q.nombre }}{{ ultimo ? '' : ', ' }} }
              </p>
            }
            @if (r.beneficiosAplicables.length) {
              <p><strong>Beneficios:</strong>
                @for (b of r.beneficiosAplicables; track b.id) {
                  <span class="badge badge-aviso">{{ b.nombre }} · {{ b.porcentajeCobertura }}%</span>&nbsp;
                }
              </p>
            }
            @if (ruta.estado !== 'DESACTUALIZADA') {
              <button class="btn" (click)="seleccionar(r)" [disabled]="seleccionando">Seleccionar esta oportunidad</button>
            }
          </div>
        </div>
      }
    } @else if (!cargando) {
      <div class="card vacio">
        <p>Aún no tienes una ruta educativa.</p>
        <p>Completa tu <a routerLink="/beneficiario/caracterizacion">caracterización</a> al 100%: tu ruta se genera
          automáticamente (también puedes presionar “Generar mi ruta”).</p>
        <!-- Auditoría - Plan de mejoramiento #2 -->
        <p>Mientras tanto, puedes revisar y postularte a cualquier programa de la oferta educativa.</p>
        <a class="btn btn-sec" routerLink="/beneficiario/oportunidades">Explorar toda la oferta</a>
      </div>
    }
  `,
  styles: [`
    .recomendacion { display: flex; gap: 16px; }
    .orden { flex: 0 0 40px; height: 40px; border-radius: 50%; background: var(--primario); color: #fff;
      display: grid; place-items: center; font-weight: 700; }
    .cuerpo { flex: 1; min-width: 0; }
  `],
})
export class RutasEducativasComponent implements OnInit {
  private rutaService = inject(RutaService);
  private postulacionService = inject(PostulacionService);
  private router = inject(Router);

  ruta: RutaEducativa | null = null;
  error: string | null = null;

  /** La ruta quedó desactualizada (cambió el perfil) y no hay ninguna oferta que coincida: no se muestra nada. */
  get sinCompatibles(): boolean {
    return !!this.ruta && (this.ruta.estado === 'DESACTUALIZADA' || this.ruta.recomendaciones.length === 0);
  }
  cargando = false;
  seleccionando = false;

  ngOnInit(): void {
    this.cargando = true;
    this.rutaService.actual().subscribe({
      next: (r) => {
        this.ruta = r;
        this.cargando = false;
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.cargando = false;
      },
    });
  }

  generar(): void {
    this.error = null;
    this.cargando = true;
    this.rutaService.generar().subscribe({
      next: (r) => {
        this.ruta = r;
        this.cargando = false;
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.cargando = false;
      },
    });
  }

  /**crea la postulación y lleva al detalle para cargar documentos. */
  seleccionar(r: Recomendacion): void {
    this.error = null;
    this.seleccionando = true;
    this.postulacionService.seleccionar(r.id).subscribe({
      next: (p) => this.router.navigate(['/beneficiario/postulaciones', p.id]),
      error: (e) => {
        this.error = mensajeError(e);
        this.seleccionando = false;
      },
    });
  }
}
