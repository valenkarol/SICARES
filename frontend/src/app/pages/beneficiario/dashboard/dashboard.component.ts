import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Perfil } from '../../../models/perfil.model';
import { Notificacion, Recordatorio } from '../../../models/postulacion.model';
import { RutaEducativa } from '../../../models/ruta.model';
import { NotificacionService } from '../../../services/notificacion.service';
import { PerfilService } from '../../../services/perfil.service';
import { PostulacionService } from '../../../services/postulacion.service';
import { RutaService } from '../../../services/ruta.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';

/**
 * Inicio del beneficiario: estado del perfil (HU-10), de la ruta (HU-19),
 * próximas fechas (HU-26) y notificaciones.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, EtiquetaPipe],
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
        <h3>Próximas fechas</h3>
        @for (r of proximos; track r.id) {
          <p><strong>{{ r.fechaEvento | date: 'dd MMM' }}</strong> · {{ r.titulo }}</p>
        } @empty {
          <p class="suave">No tienes fechas próximas.</p>
        }
        <a class="btn btn-sec" routerLink="/beneficiario/postulaciones">Mis postulaciones</a>
      </div>
    </div>

    <div class="card">
      <h2>Notificaciones</h2>
      @for (n of notificaciones; track n.id) {
        <div class="notif" [class.no-leida]="!n.leida">
          <div class="fila entre">
            <strong>{{ n.asunto }}</strong>
            <span class="suave">{{ n.fechaEnvio | date: 'dd/MM/yyyy HH:mm' }}</span>
          </div>
          <p>{{ n.mensaje }}</p>
          @if (!n.leida) {
            <button class="btn btn-sec btn-chico" (click)="marcarLeida(n)">Marcar como leída</button>
          }
        </div>
      } @empty {
        <p class="vacio">No tienes notificaciones.</p>
      }
    </div>
  `,
  styles: [`
    .notif { border-bottom: 1px solid var(--borde); padding: 10px 0; }
    .notif p { margin: 4px 0 8px; }
    .no-leida { border-left: 3px solid var(--acento); padding-left: 10px; }
  `],
})
export class DashboardComponent implements OnInit {
  private perfilService = inject(PerfilService);
  private rutaService = inject(RutaService);
  private postulacionService = inject(PostulacionService);
  private notificacionService = inject(NotificacionService);

  perfil: Perfil | null = null;
  ruta: RutaEducativa | null = null;
  proximos: Recordatorio[] = [];
  notificaciones: Notificacion[] = [];

  ngOnInit(): void {
    this.perfilService.consultar().subscribe((p) => (this.perfil = p));
    this.rutaService.actual().subscribe((r) => (this.ruta = r));
    this.postulacionService.recordatorios().subscribe((lista) => {
      const hoy = new Date().toISOString().slice(0, 10);
      this.proximos = lista.filter((r) => r.fechaEvento >= hoy).slice(0, 5);
    });
    this.notificacionService.listar().subscribe((n) => (this.notificaciones = n));
  }

  marcarLeida(n: Notificacion): void {
    this.notificacionService.marcarLeida(n.id).subscribe(() => (n.leida = true));
  }
}
