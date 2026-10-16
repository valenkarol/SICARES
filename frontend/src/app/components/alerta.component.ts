import { Component, Input } from '@angular/core';

/** Mensaje de éxito/error/aviso reutilizable: <app-alerta tipo="error" [mensaje]="error" /> */
@Component({
  selector: 'app-alerta',
  standalone: true,
  template: `
    @if (mensaje) {
      <div class="alerta" [class.alerta-error]="tipo === 'error'" [class.alerta-ok]="tipo === 'ok'"
           [class.alerta-aviso]="tipo === 'aviso'" role="alert">{{ mensaje }}</div>
    }
  `,
})
export class AlertaComponent {
  @Input() mensaje: string | null = null;
  @Input() tipo: 'error' | 'ok' | 'aviso' = 'error';
}
