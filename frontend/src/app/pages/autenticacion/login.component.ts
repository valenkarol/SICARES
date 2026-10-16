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
        <h1>SICARES</h1>
        <p class="suave">Sistema de Caracterización y Seguimiento Educativo</p>
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
        <p class="suave centro">¿No tienes cuenta? <a routerLink="/registro">Regístrate</a></p>
      </div>
    </div>
  `,
  styles: [`
    .auth { min-height: 100vh; display: grid; place-items: center; padding: 16px; }
    .caja { width: 100%; max-width: 400px; }
    .ancho { width: 100%; margin-top: 4px; }
    .centro { text-align: center; margin-top: 16px; }
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
