import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../services/notificacion.service';

/** Barra de navegación + contenido. Muestra menús distintos según el rol (HU-03). */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra">
      <div class="barra-in">
        <a class="marca" [routerLink]="auth.rutaInicio()">SICARES</a>
        <nav>
          @if (auth.rol === 'BENEFICIARIO') {
            <a routerLink="/beneficiario" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Inicio</a>
            <a routerLink="/beneficiario/caracterizacion" routerLinkActive="activo">Mi perfil</a>
            <a routerLink="/beneficiario/oportunidades" routerLinkActive="activo">Oferta</a>
            <a routerLink="/beneficiario/rutas" routerLinkActive="activo">Mi ruta</a>
            <a routerLink="/beneficiario/postulaciones" routerLinkActive="activo">Postulaciones</a>
          } @else {
            <a routerLink="/admin" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Panel</a>
            <a routerLink="/admin/instituciones" routerLinkActive="activo">Instituciones</a>
            <a routerLink="/admin/programas" routerLinkActive="activo">Programas</a>
            <a routerLink="/admin/oportunidades" routerLinkActive="activo">Convocatorias</a>
            <a routerLink="/admin/requisitos" routerLinkActive="activo">Requisitos</a>
            <a routerLink="/admin/beneficios" routerLinkActive="activo">Beneficios</a>
            <a routerLink="/admin/postulaciones" routerLinkActive="activo">Postulaciones</a>
            <a routerLink="/admin/beneficiarios" routerLinkActive="activo">Usuarios</a>
          }
        </nav>
        <div class="usuario">
          @if (noLeidas > 0) { <span class="badge badge-aviso" title="Notificaciones sin leer">{{ noLeidas }}</span> }
          <span class="suave">{{ auth.usuario?.correo }}</span>
          <button class="btn btn-sec btn-chico" (click)="auth.logout()">Salir</button>
        </div>
      </div>
    </header>
    <main class="contenedor">
      <router-outlet />
    </main>
  `,
  styles: [`
    /* Colores del documento UI: barra azul marino, marca y menú en rosa/lila */
    .barra { background: var(--marino); position: sticky; top: 0; z-index: 10;
      box-shadow: 0 2px 8px rgba(1, 32, 96, 0.25); }
    .barra-in { max-width: 1120px; margin: 0 auto; padding: 12px 16px; display: flex; gap: 16px;
      align-items: center; flex-wrap: wrap; }
    .marca { font-weight: 700; font-size: 1.35rem; color: var(--rosa); text-decoration: none; letter-spacing: 0.5px; }
    nav { display: flex; gap: 4px; flex-wrap: wrap; flex: 1; }
    nav a { padding: 6px 12px; border-radius: 8px; text-decoration: none; color: var(--rosa); font-weight: 500; }
    nav a:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
    nav a.activo { background: var(--acero); color: #fff; }
    .usuario { display: flex; gap: 8px; align-items: center; }
    .usuario .suave { color: var(--azul-claro); }
    .usuario .btn-sec { background: transparent; color: #fff; border-color: var(--rosa); }
    .usuario .btn-sec:hover { background: var(--rosa); color: var(--marino); }
  `],
})
export class LayoutComponent implements OnInit {
  auth = inject(AuthService);
  private notificaciones = inject(NotificacionService);
  noLeidas = 0;

  ngOnInit(): void {
    this.notificaciones.noLeidas().subscribe({ next: (r) => (this.noLeidas = r.total), error: () => {} });
  }
}
