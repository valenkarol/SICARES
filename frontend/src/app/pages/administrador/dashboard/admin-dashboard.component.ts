import { KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AlertaComponent } from '../../../components/alerta.component';
import { Estadisticas } from '../../../models/admin.model';
import { AdminService } from '../../../services/admin.service';
import { PostulacionService } from '../../../services/postulacion.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/**
 * Panel del administrador con los KPIs del documento del proyecto
 * (caracterización, oferta, rutas y postulaciones) y envío manual de recordatorios (HU-26).
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [KeyValuePipe, EtiquetaPipe, AlertaComponent],
  template: `
    <h1>Panel de administración</h1>
    <app-alerta [mensaje]="error" />
    <app-alerta tipo="ok" [mensaje]="exito" />

    @if (e) {
      <div class="grid">
        <div class="card"><div class="kpi">{{ e.totalBeneficiarios }}</div><div class="suave">Beneficiarios registrados</div></div>
        <div class="card"><div class="kpi">{{ e.porcentajePerfilesCompletos }}%</div>
          <div class="suave">Usuarios con perfil completo ({{ e.perfilesCompletos }})</div></div>
        <div class="card"><div class="kpi">{{ e.beneficiariosConRuta }}</div><div class="suave">Usuarios con ruta generada</div></div>
        <div class="card"><div class="kpi">{{ e.totalPostulaciones }}</div><div class="suave">Postulaciones</div></div>
        <div class="card"><div class="kpi">{{ e.totalProgramas }}</div><div class="suave">Programas registrados</div></div>
        <div class="card"><div class="kpi">{{ e.oportunidadesVigentes }}</div><div class="suave">Convocatorias abiertas</div></div>
      </div>

      <!-- KPIs del Documento de Definición, agrupados por proceso de negocio -->
      <div class="card">
        <h2>Proceso 1 · Caracterización del usuario</h2>
        <div class="grid">
          <div><div class="kpi">{{ e.porcentajePerfilesCompletos }}%</div><div class="suave">Caracterizaciones completadas</div></div>
          <div><div class="kpi">{{ e.porcentajeFormulariosIncompletos }}%</div><div class="suave">Perfiles iniciados con información incompleta</div></div>
          <div><div class="kpi">{{ e.minutosPromedioCaracterizacion }} min</div><div class="suave">Tiempo promedio para completar la caracterización</div></div>
        </div>
      </div>

      <div class="card">
        <h2>Proceso 2 · Planeación de rutas educativas</h2>
        <div class="grid">
          <div><div class="kpi">{{ e.porcentajeUsuariosConRuta }}%</div><div class="suave">Usuarios con ruta educativa generada</div></div>
          <div><div class="kpi">{{ e.msPromedioGeneracionRuta }} ms</div><div class="suave">Tiempo promedio de generación de una ruta</div></div>
          <div><div class="kpi">{{ e.porcentajeRutasSeleccionadas }}%</div><div class="suave">Usuarios con ruta que avanzan a postulación</div></div>
        </div>
      </div>

      <div class="card">
        <h2>Proceso 3 · Postulación y seguimiento</h2>
        <div class="grid">
          <div><div class="kpi">{{ e.porcentajePostulacionesCompletadas }}%</div><div class="suave">Postulaciones completadas frente a iniciadas</div></div>
          <div><div class="kpi">{{ e.horasPromedioSeleccionAPostulacion }} h</div><div class="suave">Tiempo promedio entre la selección y la postulación formal</div></div>
          <div><div class="kpi">{{ e.porcentajeRecordatoriosAtendidos }}%</div><div class="suave">Recordatorios de cierre atendidos a tiempo</div></div>
        </div>
      </div>

      <div class="card">
        <h2>Postulaciones por estado</h2>
        <table class="tabla">
          @for (item of e.postulacionesPorEstado | keyvalue; track item.key) {
            <tr><td>{{ item.key | etiqueta }}</td><td><strong>{{ item.value }}</strong></td></tr>
          }
        </table>
      </div>
    }

    <div class="card">
      <h2>Recordatorios</h2>
      <p class="suave">Se envían automáticamente todos los días a las 7:00 a.m. Puedes ejecutar el envío ahora para pruebas.</p>
      <button class="btn btn-sec" (click)="procesarRecordatorios()">Enviar recordatorios pendientes</button>
    </div>
  `,
})
export class AdminDashboardComponent implements OnInit {
  private admin = inject(AdminService);
  private postulaciones = inject(PostulacionService);
  e: Estadisticas | null = null;
  error: string | null = null;
  exito: string | null = null;

  ngOnInit(): void {
    this.admin.estadisticas().subscribe({ next: (e) => (this.e = e), error: (err) => (this.error = mensajeError(err)) });
  }

  procesarRecordatorios(): void {
    this.postulaciones.procesarRecordatorios().subscribe({
      next: (r) => (this.exito = `Recordatorios enviados: ${r.enviados}`),
      error: (err) => (this.error = mensajeError(err)),
    });
  }
}
