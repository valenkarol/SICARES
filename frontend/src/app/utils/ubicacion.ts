import { DepartamentoCatalogo } from '../models/perfil.model';

/**
 * Ayudas para los selects dependientes Departamento → Municipio (catálogo `departamentos` de GET /api/catalogos).
 * Comparan sin tildes ni mayúsculas, así un dato viejo como "Quindio" se reconoce como "Quindío".
 */
export function normalizarTexto(s: string | null | undefined): string {
  return (s ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[-_.]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export function buscarDepartamento(
  departamentos: DepartamentoCatalogo[] | null | undefined,
  nombre: string | null | undefined,
): DepartamentoCatalogo | undefined {
  const n = normalizarTexto(nombre);
  if (!n) return undefined;
  return (departamentos ?? []).find(
    (d) => normalizarTexto(d.nombre) === n || normalizarTexto(d.codigo) === n
      || normalizarTexto(d.nombre).replace(' dc', '') === n,
  );
}

/** Municipios del departamento elegido (vacío si no hay departamento). */
export function municipiosDe(
  departamentos: DepartamentoCatalogo[] | null | undefined,
  nombreDepartamento: string | null | undefined,
): string[] {
  return buscarDepartamento(departamentos, nombreDepartamento)?.municipios ?? [];
}

/** Nombre oficial del municipio dentro de la lista, o '' si no pertenece. */
export function municipioOficial(municipios: string[], nombre: string | null | undefined): string {
  const n = normalizarTexto(nombre);
  return municipios.find((m) => normalizarTexto(m) === n || normalizarTexto(m).replace(' dc', '') === n) ?? '';
}
