import { AfterViewInit, Component, DestroyRef, ElementRef, HostListener, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../services/notificacion.service';

/**
 * Barra de navegación + contenido. Muestra menús distintos según el rol (HU-03).
 * El menú queda en una sola fila: si las opciones no caben (panel del administrador), se desplaza de lado a lado
 * con las flechas ‹ ›, con la rueda del mouse, arrastrando en pantallas táctiles o con el teclado (Tab).
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra">
      <div class="barra-in">
        <a class="marca" [routerLink]="auth.rutaInicio()">SICARES</a>

        <div class="menu">
          <button type="button" class="flecha" aria-label="Ver opciones anteriores del menú"
                  [class.oculta]="!puedeIzquierda" (click)="desplazar(-1)">‹</button>
          <nav #nav (scroll)="actualizarFlechas()" aria-label="Menú principal">
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
          <button type="button" class="flecha" aria-label="Ver más opciones del menú"
                  [class.oculta]="!puedeDerecha" (click)="desplazar(1)">›</button>
        </div>

        <div class="usuario">
          @if (noLeidas > 0) { <span class="badge badge-aviso" title="Notificaciones sin leer">{{ noLeidas }}</span> }
          <span class="suave correo" [title]="auth.usuario?.correo ?? ''">{{ auth.usuario?.correo }}</span>
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
    .barra-in { max-width: 1120px; margin: 0 auto; padding: 12px 16px; display: flex; gap: 12px;
      align-items: center; }
    .marca { font-weight: 700; font-size: 1.35rem; color: var(--rosa); text-decoration: none; letter-spacing: 0.5px;
      flex-shrink: 0; }

    /* Menú en una sola fila con desplazamiento horizontal */
    .menu { flex: 1; min-width: 0; display: flex; align-items: center; gap: 2px; }
    nav { display: flex; gap: 4px; flex-wrap: nowrap; overflow-x: auto; scroll-behavior: smooth;
      scrollbar-width: none; -webkit-overflow-scrolling: touch; flex: 1; min-width: 0;
      mask-image: linear-gradient(to right, transparent 0, #000 12px, #000 calc(100% - 12px), transparent 100%); }
    nav::-webkit-scrollbar { display: none; }
    nav a { padding: 6px 12px; border-radius: 8px; text-decoration: none; color: var(--rosa); font-weight: 500;
      white-space: nowrap; flex-shrink: 0; }
    nav a:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }
    nav a.activo { background: var(--acero); color: #fff; }
    nav a:focus-visible { outline: 2px solid var(--rosa); outline-offset: -2px; }
    .flecha { flex-shrink: 0; width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--rosa);
      background: transparent; color: var(--rosa); font-size: 1.2rem; line-height: 1; cursor: pointer;
      display: grid; place-items: center; padding: 0 0 2px; }
    .flecha:hover { background: var(--rosa); color: var(--marino); }
    .flecha.oculta { visibility: hidden; }

    .usuario { display: flex; gap: 8px; align-items: center; flex-shrink: 0; }
    .usuario .suave { color: var(--azul-claro); }
    .correo { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .usuario .btn-sec { background: transparent; color: #fff; border-color: var(--rosa); }
    .usuario .btn-sec:hover { background: var(--rosa); color: var(--marino); }
    @media (max-width: 720px) { .correo { display: none; } }
  `],
})
export class LayoutComponent implements OnInit, AfterViewInit {
  auth = inject(AuthService);
  private notificaciones = inject(NotificacionService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  @ViewChild('nav') private nav?: ElementRef<HTMLElement>;

  noLeidas = 0;
  puedeIzquierda = false;
  puedeDerecha = false;

  ngOnInit(): void {
    this.notificaciones.noLeidas().subscribe({ next: (r) => (this.noLeidas = r.total), error: () => {} });
    // Al cambiar de página, la opción activa se desplaza a la vista
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => setTimeout(() => this.mostrarActivo()));
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.mostrarActivo();
      this.actualizarFlechas();
    });
  }

  @HostListener('window:resize')
  actualizarFlechas(): void {
    const el = this.nav?.nativeElement;
    if (!el) return;
    this.puedeIzquierda = el.scrollLeft > 2;
    this.puedeDerecha = el.scrollLeft + el.clientWidth < el.scrollWidth - 2;
  }

  /** Desplaza el menú casi un "ancho" hacia la izquierda (-1) o la derecha (1). */
  desplazar(direccion: -1 | 1): void {
    const el = this.nav?.nativeElement;
    if (!el) return;
    el.scrollBy({ left: direccion * Math.max(160, el.clientWidth * 0.7), behavior: 'smooth' });
  }

  /** Permite mover el menú con la rueda del mouse (sin Shift). */
  @HostListener('wheel', ['$event'])
  rueda(ev: WheelEvent): void {
    const el = this.nav?.nativeElement;
    if (!el || !el.contains(ev.target as Node) || el.scrollWidth <= el.clientWidth) return;
    if (Math.abs(ev.deltaY) > Math.abs(ev.deltaX)) {
      el.scrollLeft += ev.deltaY;
      ev.preventDefault();
    }
  }

  private mostrarActivo(): void {
    const activo = this.nav?.nativeElement.querySelector<HTMLElement>('a.activo');
    activo?.scrollIntoView({ block: 'nearest', inline: 'center', behavior: 'smooth' });
    this.actualizarFlechas();
  }
}
