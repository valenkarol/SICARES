import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertaComponent } from '../../../components/alerta.component';
import { Usuario } from '../../../models/admin.model';
import { AdminService } from '../../../services/admin.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/** HU-03 Gestión de roles: listar usuarios, activar/desactivar y crear administradores. */
@Component({
  selector: 'app-beneficiarios',
  standalone: true,
  imports: [FormsModule, DatePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Usuarios</h1>
    <app-alerta [mensaje]="error" />
    <app-alerta tipo="ok" [mensaje]="exito" />

    <div class="card">
      <h3>Nuevo administrador</h3>
      <form #f="ngForm" (ngSubmit)="crearAdmin()" class="fila">
        <input class="entrada" type="email" name="correo" [(ngModel)]="correo" placeholder="correo@dominio.co" required email />
        <input class="entrada" type="password" name="pass" [(ngModel)]="contrasena" placeholder="Contraseña" required minlength="8" />
        <button class="btn" [disabled]="f.invalid">Crear</button>
      </form>
    </div>

    <div class="card tabla-scroll">
      <table class="tabla">
        <thead><tr><th>Usuario</th><th>Rol</th><th>Perfil</th><th>Registro</th><th>Último acceso</th><th>Estado</th></tr></thead>
        <tbody>
          @for (u of usuarios; track u.id) {
            <tr>
              <td>{{ u.nombreCompleto ?? '—' }}<div class="suave">{{ u.correo }}</div></td>
              <td>{{ u.rol | etiqueta }}</td>
              <td>{{ u.porcentajePerfil != null ? u.porcentajePerfil + '%' : '' }}</td>
              <td>{{ u.fechaRegistro | date: 'dd/MM/yyyy' }}</td>
              <td>{{ u.ultimoAcceso | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <button class="btn btn-chico" [class.btn-peligro]="u.activo" [class.btn-sec]="!u.activo" (click)="alternar(u)">
                  {{ u.activo ? 'Desactivar' : 'Activar' }}
                </button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`.entrada { font: inherit; padding: 9px 11px; border: 1px solid var(--borde); border-radius: 8px; }`],
})
export class BeneficiariosComponent implements OnInit {
  private admin = inject(AdminService);
  usuarios: Usuario[] = [];
  correo = '';
  contrasena = '';
  error: string | null = null;
  exito: string | null = null;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.admin.usuarios().subscribe((u) => (this.usuarios = u));
  }

  alternar(u: Usuario): void {
    this.error = null;
    this.admin.cambiarActivo(u.id, !u.activo).subscribe({
      next: (r) => (u.activo = r.activo),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  crearAdmin(): void {
    this.error = null;
    this.admin.crearAdministrador(this.correo, this.contrasena).subscribe({
      next: () => {
        this.exito = 'Administrador creado';
        this.correo = '';
        this.contrasena = '';
        this.cargar();
      },
      error: (e) => (this.error = mensajeError(e)),
    });
  }
}
