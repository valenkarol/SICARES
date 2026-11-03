import { KeyValuePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AlertaComponent } from '../../../components/alerta.component';
import { Estadisticas, ResultadoCorreo } from '../../../models/admin.model';
import { AdminService } from '../../../services/admin.service';
import { PostulacionService } from '../../../services/postulacion.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

/**
 * Panel del administrador con los KPIs del documento del proyecto
 * (caracterización, oferta, rutas y postulaciones), envío manual de recordatorios y prueba del correo (HU-26).
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
      <p class="suave">Se envían automáticamente: el sistema revisa cada minuto y envía cada recordatorio en cuanto
        llega su fecha (3 días antes del cierre de inscripciones, la admisión y la matrícula). Puedes forzar el envío
        ahora para pruebas.</p>
      <button class="btn btn-sec" (click)="procesarRecordatorios()">Enviar recordatorios pendientes</button>
    </div>

    <div class="card">
      <h2>Correo (Gmail)</h2>
      <p class="suave">Envía un correo de prueba para comprobar que las notificaciones llegan al correo. Si falla,
        aquí aparece el motivo exacto (correo deshabilitado, contraseña de aplicación incorrecta, sin conexión…).</p>
      <div class="fila">
        <input class="entrada" type="email" placeholder="Destino (vacío = tu correo)" [value]="correoPrueba"
               (input)="correoPrueba = $any($event.target).value" aria-label="Correo de destino de la prueba" />
        <button class="btn btn-sec" (click)="probarCorreo()" [disabled]="probandoCorreo">
          {{ probandoCorreo ? 'Enviando...' : 'Enviar correo de prueba' }}
        </button>
      </div>
      @if (resultadoCorreo) {
        <div class="alerta" [class.alerta-ok]="resultadoCorreo.enviado" [class.alerta-error]="!resultadoCorreo.enviado"
             role="status" style="margin-top:12px">{{ resultadoCorreo.detalle }}</div>
      }
    </div>
  `,
  styles: [`
    .entrada { font: inherit; padding: 8px 11px; border: 1px solid var(--marino); border-radius: 8px; min-width: 260px; }
  `],
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

  correoPrueba = '';
  probandoCorreo = false;
  resultadoCorreo: ResultadoCorreo | null = null;

  probarCorreo(): void {
    this.probandoCorreo = true;
    this.resultadoCorreo = null;
    this.admin.probarCorreo(this.correoPrueba.trim() || undefined).subscribe({
      next: (r) => {
        this.resultadoCorreo = r;
        this.probandoCorreo = false;
      },
      error: (err) => {
        this.resultadoCorreo = { enviado: false, detalle: mensajeError(err) };
        this.probandoCorreo = false;
      },
    });
  }

  procesarRecordatorios(): void {
    this.postulaciones.procesarRecordatorios().subscribe({
      next: (r) => (this.exito = `Recordatorios enviados: ${r.enviados}`),
      error: (err) => (this.error = mensajeError(err)),
    });
  }
}
