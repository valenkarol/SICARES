import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Postulacion } from '../../../models/postulacion.model';
import { PostulacionService } from '../../../services/postulacion.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';

/** consultarPostulaciones(): listado de postulaciones del beneficiario (HU-24). */
@Component({
  selector: 'app-postulaciones',
  standalone: true,
  imports: [RouterLink, DatePipe, EtiquetaPipe],
  template: `
    <h1>Mis postulaciones</h1>
    <div class="card tabla-scroll">
      <table class="tabla">
        <thead>
          <tr><th>Programa</th><th>Institución</th><th>Periodo</th><th>Estado</th><th>Pendientes</th><th></th></tr>
        </thead>
        <tbody>
          @for (p of postulaciones; track p.id) {
            <tr>
              <td>{{ p.oportunidad.programaNombre }}</td>
              <td>{{ p.oportunidad.institucionNombre }}</td>
              <td>{{ p.oportunidad.periodoAcademico }}</td>
              <td><span class="badge" [class.badge-ok]="p.estado === 'MATRICULADA' || p.estado === 'ADMITIDA'"
                        [class.badge-error]="p.estado === 'NO_ADMITIDA' || p.estado === 'CERRADA'">{{ p.estado | etiqueta }}</span></td>
              <td>{{ p.documentosPendientes.length }} documento(s)</td>
              <td><a class="btn btn-sec btn-chico" [routerLink]="['/beneficiario/postulaciones', p.id]">Ver seguimiento</a></td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="vacio">Aún no has seleccionado oportunidades. Ve a <a routerLink="/beneficiario/rutas">Mi ruta</a>.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class PostulacionesComponent implements OnInit {
  private postulacionService = inject(PostulacionService);
  postulaciones: Postulacion[] = [];

  ngOnInit(): void {
    this.postulacionService.listar().subscribe((p) => (this.postulaciones = p));
  }
}
