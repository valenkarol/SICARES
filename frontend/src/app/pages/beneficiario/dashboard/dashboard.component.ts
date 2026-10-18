import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Perfil } from '../../../models/perfil.model';
import { RutaEducativa } from '../../../models/ruta.model';
import { PerfilService } from '../../../services/perfil.service';
import { RutaService } from '../../../services/ruta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';

/**
 * Inicio del beneficiario: estado del perfil (HU-10) y de la ruta (HU-19).
 * Sprint 3: agregar "Próximas fechas" (HU-26) y "Notificaciones".
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, EtiquetaPipe],
  template: `
    <h1>Hola{{ perfil?.informacionPersonal ? ', ' + perfil?.informacionPersonal?.nombres : '' }}</h1>
    <p class="suave">Este es el resumen de tu proceso en SICARES.</p>

    <div class="grid">
      <div class="card card-azul">
        <h3>Mi caracterización</h3>
        <div class="progreso"><div [style.width.%]="perfil?.porcentajeCompletitud ?? 0"></div></div>
        <p>{{ perfil?.porcentajeCompletitud ?? 0 }}% completado</p>
        @if (perfil?.completo) {
          <span class="badge badge-ok">Perfil completo</span>
        } @else {
          <a class="btn" routerLink="/beneficiario/caracterizacion">Completar perfil</a>
        }
      </div>

      <div class="card card-rosa">
        <h3>Mi ruta educativa</h3>
        @if (ruta) {
          <p>
            Versión {{ ruta.version }} ·
            <span class="badge" [class.badge-aviso]="ruta.estado === 'DESACTUALIZADA'">{{ ruta.estado | etiqueta }}</span>
          </p>
          <p class="suave">{{ ruta.recomendaciones.length }} oportunidades recomendadas</p>
        } @else {
          <p class="suave">Aún no has generado una ruta.</p>
        }
        <a class="btn btn-acento" routerLink="/beneficiario/rutas">Ver mi ruta</a>
      </div>

      <div class="card">
        <h3>Oferta educativa</h3>
        <p class="suave">Consulta programas por nivel, modalidad, municipio o área.</p>
        <a class="btn btn-sec" routerLink="/beneficiario/oportunidades">Ver oferta</a>
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  private perfilService = inject(PerfilService);
  private rutaService = inject(RutaService);

  perfil: Perfil | null = null;
  ruta: RutaEducativa | null = null;

  ngOnInit(): void {
    this.perfilService.consultar().subscribe((p) => (this.perfil = p));
    this.rutaService.actual().subscribe((r) => (this.ruta = r));
  }
}
