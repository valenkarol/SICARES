import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';

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
          } @else {
            <a routerLink="/admin" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Panel</a>
            <a routerLink="/admin/instituciones" routerLinkActive="activo">Instituciones</a>
            <a routerLink="/admin/programas" routerLinkActive="activo">Programas</a>
            <a routerLink="/admin/oportunidades" routerLinkActive="activo">Convocatorias</a>
            <a routerLink="/admin/requisitos" routerLinkActive="activo">Requisitos</a>
            <a routerLink="/admin/beneficios" routerLinkActive="activo">Beneficios</a>
            <a routerLink="/admin/beneficiarios" routerLinkActive="activo">Usuarios</a>
          }
        </nav>
        <div class="usuario">
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
    .barra { background: #fff; border-bottom: 1px solid var(--borde); position: sticky; top: 0; z-index: 10; }
    .barra-in { max-width: 1120px; margin: 0 auto; padding: 10px 16px; display: flex; gap: 16px;
      align-items: center; flex-wrap: wrap; }
    .marca { font-weight: 700; font-size: 1.15rem; color: var(--primario); text-decoration: none; }
    nav { display: flex; gap: 4px; flex-wrap: wrap; flex: 1; }
    nav a { padding: 6px 10px; border-radius: 6px; text-decoration: none; color: var(--texto-suave); font-weight: 500; }
    nav a.activo, nav a:hover { background: var(--primario-claro); color: var(--primario-oscuro); }
    .usuario { display: flex; gap: 8px; align-items: center; }
  `],
})
export class LayoutComponent {
  auth = inject(AuthService);
}
