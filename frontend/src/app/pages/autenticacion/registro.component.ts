import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AlertaComponent } from '../../components/alerta.component';
import { AuthService } from '../../services/auth.service';
import { mensajeError } from '../../utils/errores';

/** HU-01 Registro + "Solicitar autorización e informar del tratamiento de datos". */
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, RouterLink, AlertaComponent],
  template: `
    <div class="auth">
      <div class="card caja">
        <div class="cabecera">
          <h1>SICARES</h1>
          <p>Crear cuenta · Regístrate para construir tu perfil y recibir rutas educativas.</p>
        </div>
        <div class="cuerpo">
          <app-alerta [mensaje]="error" />
          <form (ngSubmit)="registrar()" #f="ngForm">
            <div class="campo">
              <label for="correo">Correo</label>
              <input id="correo" name="correo" type="email" [(ngModel)]="correo" required email />
            </div>
            <div class="campo">
              <label for="contrasena">Contraseña</label>
              <input id="contrasena" name="contrasena" type="password" [(ngModel)]="contrasena" required minlength="8" />
              <span class="suave">Mínimo 8 caracteres, con mayúscula, minúscula y número.</span>
            </div>
            <div class="campo">
              <label for="confirmar">Confirmar contraseña</label>
              <input id="confirmar" name="confirmar" type="password" [(ngModel)]="confirmar" required />
            </div>

            <div class="politica">
              <strong>Tratamiento de datos personales</strong>
              <p>
                SICARES recopila información personal, educativa, socioeconómica y de tu condición de víctima con el
                único fin de orientarte hacia oportunidades educativas. Tus datos se tratan conforme a la Ley 1581 de 2012,
                el Decreto 1377 de 2013 y las reglas de reserva de la Ley 1448 de 2011. Puedes consultar, actualizar o
                revocar esta autorización en cualquier momento.
              </p>
              <label class="check">
                <input type="checkbox" name="acepta" [(ngModel)]="acepta" required />
                Autorizo el tratamiento de mis datos personales
              </label>
            </div>

            <button class="btn ancho" type="submit" [disabled]="f.invalid || !acepta || cargando">
              {{ cargando ? 'Creando cuenta...' : 'Registrarme' }}
            </button>
          </form>
          <p class="suave centro">¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth { min-height: 100vh; display: grid; place-items: center; padding: 16px; background: var(--azul-palido); }
    .caja { width: 100%; max-width: 480px; padding: 0; overflow: hidden; border: 3px solid var(--marino); border-radius: 28px; }
    .cabecera { background: var(--marino); padding: 24px 28px 20px; }
    .cabecera h1 { color: var(--rosa); margin: 0 0 6px; font-size: 1.8rem; }
    .cabecera p { color: #fff; margin: 0; font-size: 0.9rem; }
    .cuerpo { padding: 22px 28px 26px; }
    .ancho { width: 100%; }
    .centro { text-align: center; margin-top: 16px; }
    .politica { background: var(--rosa-claro); border: 1px solid var(--rosa-medio); color: var(--marino); border-radius: 8px; padding: 12px; margin-bottom: 12px; font-size: 0.85rem; }
    .politica p { margin: 6px 0 10px; }
  `],
})
export class RegistroComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  correo = '';
  contrasena = '';
  confirmar = '';
  acepta = false;
  error: string | null = null;
  cargando = false;

  registrar(): void {
    this.error = null;
    if (this.contrasena !== this.confirmar) {
      this.error = 'Las contraseñas no coinciden';
      return;
    }
    this.cargando = true;
    this.auth
      .registrar({ correo: this.correo, contrasena: this.contrasena, aceptaTratamientoDatos: this.acepta })
      .subscribe({
        next: () => this.router.navigate(['/beneficiario/caracterizacion']),
        error: (e) => {
          this.error = mensajeError(e);
          this.cargando = false;
        },
      });
  }
}
