import { Component, OnInit, inject } from '@angular/core';
import { AlertaComponent } from '../../../components/alerta.component';
import { Estadisticas } from '../../../models/admin.model';
import { AdminService } from '../../../services/admin.service';
import { mensajeError } from '../../../utils/errores';

/**
 * Panel del administrador con los KPIs del Sprint 2 (caracterización, oferta y rutas).
 * Sprint 3: agregar "Postulaciones" y "Postulaciones por estado".
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [AlertaComponent],
  template: `
    <h1>Panel de administración</h1>
    <app-alerta [mensaje]="error" />

    @if (e) {
      <div class="grid">
        <div class="card"><div class="kpi">{{ e.totalBeneficiarios }}</div><div class="suave">Beneficiarios registrados</div></div>
        <div class="card"><div class="kpi">{{ e.porcentajePerfilesCompletos }}%</div>
          <div class="suave">Usuarios con perfil completo ({{ e.perfilesCompletos }})</div></div>
        <div class="card"><div class="kpi">{{ e.beneficiariosConRuta }}</div><div class="suave">Usuarios con ruta generada</div></div>
        <div class="card"><div class="kpi">{{ e.totalProgramas }}</div><div class="suave">Programas registrados</div></div>
        <div class="card"><div class="kpi">{{ e.oportunidadesVigentes }}</div><div class="suave">Convocatorias abiertas</div></div>
      </div>
    }
  `,
})
export class AdminDashboardComponent implements OnInit {
  private admin = inject(AdminService);
  e: Estadisticas | null = null;
  error: string | null = null;

  ngOnInit(): void {
    this.admin.estadisticas().subscribe({ next: (e) => (this.e = e), error: (err) => (this.error = mensajeError(err)) });
  }
}
