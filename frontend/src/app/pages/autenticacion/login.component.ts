import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AlertaComponent } from '../../components/alerta.component';
import { AuthService } from '../../services/auth.service';
import { mensajeError } from '../../utils/errores';

/** HU-02 Inicio de sesión. */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, AlertaComponent],
  template: `
    <div class="auth">
      <div class="card caja">
        <div class="cabecera">
          <h1>SICARES</h1>
          <p>Sistema de Caracterización<br />y Seguimiento Educativo</p>
        </div>
        <div class="cuerpo">
          <div class="titulo-rosa">Iniciar sesión</div>
          <p class="suave centro-sup">Accede a tu cuenta para continuar</p>
          <app-alerta [mensaje]="error" />
          <form (ngSubmit)="ingresar()" #f="ngForm">
            <div class="campo">
              <label for="correo">Correo</label>
              <input id="correo" name="correo" type="email" [(ngModel)]="correo" required email autocomplete="username" />
            </div>
            <div class="campo">
              <label for="contrasena">Contraseña</label>
              <input id="contrasena" name="contrasena" type="password" [(ngModel)]="contrasena" required
                     autocomplete="current-password" />
            </div>
            <button class="btn ancho" type="submit" [disabled]="f.invalid || cargando">
              {{ cargando ? 'Ingresando...' : 'Iniciar sesión' }}
            </button>
          </form>
          <a class="btn btn-sec ancho centro-btn" routerLink="/registro">Crear cuenta</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth { min-height: 100vh; display: grid; place-items: center; padding: 16px; background: var(--azul-palido); }
    .caja { width: 100%; max-width: 420px; padding: 0; overflow: hidden; border: 3px solid var(--marino); border-radius: 28px; }
    .cabecera { background: var(--marino); padding: 28px 28px 22px; }
    .cabecera h1 { color: var(--rosa); margin: 0 0 6px; font-size: 1.8rem; }
    .cabecera p { color: #fff; margin: 0; font-size: 0.9rem; font-weight: 500; }
    .cuerpo { padding: 22px 28px 26px; }
    .titulo-rosa { background: var(--rosa); color: var(--marino); text-align: center; font-weight: 700;
      padding: 8px; border-radius: 6px; border: 1px solid var(--marino); }
    .centro-sup { text-align: center; color: var(--marino); margin: 10px 0 16px; }
    .ancho { width: 100%; margin-top: 4px; }
    .centro-btn { text-align: center; margin-top: 10px; }
  `],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  correo = '';
  contrasena = '';
  error: string | null = null;
  cargando = false;

  ingresar(): void {
    this.error = null;
    this.cargando = true;
    this.auth.login({ correo: this.correo, contrasena: this.contrasena }).subscribe({
      next: () => this.router.navigate([this.auth.rutaInicio()]),
      error: (e) => {
        this.error = mensajeError(e);
        this.cargando = false;
      },
    });
  }
}
