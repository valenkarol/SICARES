import { Pipe, PipeTransform } from '@angular/core';

/** Convierte valores de enums en texto legible: "BASICA_SECUNDARIA" -> "Basica secundaria". */
@Pipe({ name: 'etiqueta', standalone: true })
export class EtiquetaPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    if (!valor) return '';
    const texto = valor.replace(/_/g, ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}

/** Abre un Blob (PDF/imagen) en una pestaña nueva. */
export function abrirBlob(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
