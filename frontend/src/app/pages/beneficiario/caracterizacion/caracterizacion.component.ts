import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { AlertaComponent } from '../../../components/alerta.component';
import {
  Catalogos,
  CondicionAccesibilidad,
  InformacionPersonal,
  Perfil,
  SituacionSocioeconomica,
  SituacionVictima,
  TrayectoriaEducativa,
} from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { PerfilService } from '../../../services/perfil.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';

type Seccion = 'personal' | 'victima' | 'socio' | 'trayectoria' | 'intereses' | 'accesibilidad';

/**
 * Proceso 1: Caracterización del usuario (HU-04 a HU-09) y actualización del perfil (HU-11).
 * Cada pestaña guarda su sección por separado en el backend.
 */
@Component({
  selector: 'app-caracterizacion',
  standalone: true,
  imports: [FormsModule, AlertaComponent, EtiquetaPipe],
  template: `
    <div class="fila entre">
      <h1>Mi caracterización</h1>
      <span class="badge" [class.badge-ok]="perfil?.completo">{{ perfil?.porcentajeCompletitud ?? 0 }}% completo</span>
    </div>
    <div class="progreso" style="margin-bottom:16px"><div [style.width.%]="perfil?.porcentajeCompletitud ?? 0"></div></div>

    @if (perfil && !perfil.autorizacionAceptada) {
      <div class="alerta alerta-aviso">
        Para guardar tu información debes autorizar el tratamiento de datos personales (Ley 1581 de 2012).
        <button class="btn btn-chico" (click)="aceptarAutorizacion()">Autorizo</button>
      </div>
    }

    <app-alerta [mensaje]="error" />
    <app-alerta tipo="ok" [mensaje]="exito" />

    <div class="tabs">
      @for (t of pestanas; track t.id) {
        <button class="tab" [class.activa]="seccion === t.id" (click)="cambiar(t.id)">
          {{ t.titulo }} @if (seccionLista(t.id)) { ✓ }
        </button>
      }
    </div>

    <div class="card">
      @switch (seccion) {
        <!-- ================= HU-04 Información personal ================= -->
        @case ('personal') {
          <form #f="ngForm" (ngSubmit)="guardar(perfilService.guardarInformacionPersonal(personal))">
            <div class="grid-2">
              <div class="campo"><label>Nombres *</label>
                <input name="nombres" [(ngModel)]="personal.nombres" required /></div>
              <div class="campo"><label>Apellidos *</label>
                <input name="apellidos" [(ngModel)]="personal.apellidos" required /></div>
              <div class="campo"><label>Tipo de documento *</label>
                <select name="tipoDocumento" [(ngModel)]="personal.tipoDocumento" required>
                  <option value="TI">Tarjeta de identidad</option>
                  <option value="CC">Cédula de ciudadanía</option>
                </select></div>
              <div class="campo"><label>Número de documento *</label>
                <input name="numeroDocumento" [(ngModel)]="personal.numeroDocumento" required pattern="\\d{6,12}" /></div>
              <div class="campo"><label>Fecha de nacimiento * (12 a 59 años)</label>
                <input type="date" name="fechaNacimiento" [(ngModel)]="personal.fechaNacimiento" required /></div>
              <div class="campo"><label>Género</label>
                <select name="genero" [(ngModel)]="personal.genero">
                  <option value="">Prefiero no decirlo</option>
                  <option>Femenino</option><option>Masculino</option><option>No binario</option><option>Otro</option>
                </select></div>
              <div class="campo"><label>Etnia</label>
                <select name="etnia" [(ngModel)]="personal.etnia">
                  <option value="">Ninguna</option>
                  <option>Indígena</option><option>Afrocolombiano</option><option>Raizal</option>
                  <option>Palenquero</option><option>Rrom</option>
                </select></div>
              <div class="campo"><label>Teléfono</label>
                <input name="telefono" [(ngModel)]="personal.telefono" pattern="\\d{7,10}" /></div>
              <div class="campo"><label>Departamento *</label>
                <input name="departamento" [(ngModel)]="personal.departamento" required /></div>
              <div class="campo"><label>Municipio *</label>
                <input name="municipio" [(ngModel)]="personal.municipio" required /></div>
              <div class="campo"><label>Zona *</label>
                <select name="zona" [(ngModel)]="personal.zona" required>
                  <option value="URBANA">Urbana</option><option value="RURAL">Rural</option>
                </select></div>
            </div>
            <button class="btn" [disabled]="f.invalid || guardando">Guardar</button>
          </form>
        }

        <!-- ================= HU-05 Situación de víctima ================= -->
        @case ('victima') {
          <form (ngSubmit)="guardar(perfilService.guardarSituacionVictima(victima))">
            <label class="check"><input type="checkbox" name="esVictima" [(ngModel)]="victima.esVictima" />
              Soy víctima del conflicto armado</label>
            @if (victima.esVictima) {
              <label class="check"><input type="checkbox" name="inscritoRUV" [(ngModel)]="victima.inscritoRUV" />
                Estoy inscrito(a) en el Registro Único de Víctimas (RUV)</label>
              <div class="grid-2">
                <div class="campo"><label>Hecho victimizante *</label>
                  <select name="hecho" [(ngModel)]="victima.hechoVictimizante" required>
                    @for (h of catalogos?.hechosVictimizantes ?? []; track h) { <option>{{ h }}</option> }
                  </select></div>
                <div class="campo"><label>Fecha del hecho</label>
                  <input type="date" name="fechaHecho" [(ngModel)]="victima.fechaHecho" /></div>
                <div class="campo"><label>Municipio expulsor</label>
                  <input name="municipioExpulsor" [(ngModel)]="victima.municipioExpulsor" /></div>
              </div>
            }
            <p class="suave">Esta información está protegida por la Ley 1448 de 2011.</p>
            <button class="btn" [disabled]="guardando">Guardar</button>
          </form>
        }

        <!-- ================= HU-06 Situación socioeconómica ================= -->
        @case ('socio') {
          <form #f="ngForm" (ngSubmit)="guardar(perfilService.guardarSituacionSocioeconomica(socio))">
            <div class="grid-2">
              <div class="campo"><label>Grupo SISBÉN</label>
                <select name="sisben" [(ngModel)]="socio.grupoSisben">
                  @for (g of catalogos?.gruposSisben ?? []; track g) { <option [value]="g">{{ g | etiqueta }}</option> }
                </select></div>
              <div class="campo"><label>Estrato *</label>
                <input type="number" min="1" max="6" name="estrato" [(ngModel)]="socio.estrato" required /></div>
              <div class="campo"><label>Ingreso mensual del hogar (COP)</label>
                <input type="number" min="0" name="ingreso" [(ngModel)]="socio.ingresoMensualHogar" /></div>
              <div class="campo"><label>Situación laboral</label>
                <select name="laboral" [(ngModel)]="socio.situacionLaboral">
                  @for (s of catalogos?.situacionesLaborales ?? []; track s) { <option>{{ s }}</option> }
                </select></div>
              <div class="campo"><label>Integrantes del hogar *</label>
                <input type="number" min="1" name="integrantes" [(ngModel)]="socio.integrantesHogar" required /></div>
              <div class="campo"><label>Personas a cargo *</label>
                <input type="number" min="0" name="aCargo" [(ngModel)]="socio.personasACargo" required /></div>
            </div>
            <button class="btn" [disabled]="f.invalid || guardando">Guardar</button>
          </form>
        }

        <!-- ================= HU-07 Trayectoria educativa ================= -->
        @case ('trayectoria') {
          <form #f="ngForm" (ngSubmit)="guardarTrayectoria()">
            <div class="grid-2">
              <div class="campo"><label>Último nivel educativo aprobado *</label>
                <select name="nivelActual" [(ngModel)]="trayectoria.nivelActual" required>
                  @for (n of catalogos?.nivelesEducativos ?? []; track n) { <option [value]="n">{{ n | etiqueta }}</option> }
                </select></div>
              <div class="campo"><label>Último grado aprobado (0 a 11)</label>
                <input type="number" min="0" max="11" name="grado" [(ngModel)]="trayectoria.ultimoGradoAprobado" /></div>
              <div class="campo"><label>Año en que estudiaste por última vez *</label>
                <input type="number" min="1950" name="anio" [(ngModel)]="trayectoria.anioUltimoEstudio" required /></div>
            </div>

            <h3>Estudios realizados</h3>
            @for (e of trayectoria.estudios; track $index; let i = $index) {
              <div class="estudio grid-2">
                <div class="campo"><label>Nivel</label>
                  <select [name]="'nivel' + i" [(ngModel)]="e.nivel" required>
                    @for (n of catalogos?.nivelesEducativos ?? []; track n) { <option [value]="n">{{ n | etiqueta }}</option> }
                  </select></div>
                <div class="campo"><label>Institución</label>
                  <input [name]="'inst' + i" [(ngModel)]="e.institucion" /></div>
                <div class="campo"><label>Título / programa</label>
                  <input [name]="'titulo' + i" [(ngModel)]="e.titulo" /></div>
                <div class="campo"><label>Estado</label>
                  <select [name]="'estado' + i" [(ngModel)]="e.estado" required>
                    @for (s of catalogos?.estadosEstudio ?? []; track s) { <option [value]="s">{{ s | etiqueta }}</option> }
                  </select></div>
                <div class="campo"><label>Año inicio</label>
                  <input type="number" [name]="'ini' + i" [(ngModel)]="e.anioInicio" /></div>
                <div class="campo"><label>Año fin</label>
                  <input type="number" [name]="'fin' + i" [(ngModel)]="e.anioFin" /></div>
                <div><button type="button" class="btn btn-peligro btn-chico" (click)="quitarEstudio(i)">Quitar</button></div>
              </div>
            }
            <div class="fila">
              <button type="button" class="btn btn-sec" (click)="agregarEstudio()">+ Agregar estudio</button>
              <button class="btn" [disabled]="f.invalid || guardando">Guardar</button>
            </div>
          </form>
        }

        <!-- ================= HU-08 Intereses profesionales ================= -->
        @case ('intereses') {
          <p class="suave">Selecciona entre 1 y 5 áreas que te interesen.</p>
          <div class="grid-2">
            @for (a of catalogos?.areasConocimiento ?? []; track a) {
              <label class="check">
                <input type="checkbox" [checked]="interesesSeleccionados.has(a)" (change)="alternarInteres(a)" />
                {{ a }}
              </label>
            }
          </div>
          <button class="btn" [disabled]="interesesSeleccionados.size === 0 || guardando" (click)="guardarIntereses()">
            Guardar
          </button>
        }

        <!-- ================= HU-09 Accesibilidad ================= -->
        @case ('accesibilidad') {
          <form (ngSubmit)="guardar(perfilService.guardarAccesibilidad(accesibilidad))">
            <label class="check"><input type="checkbox" name="internet" [(ngModel)]="accesibilidad.tieneInternet" />
              Tengo acceso a internet</label>
            <label class="check"><input type="checkbox" name="disp" [(ngModel)]="accesibilidad.tieneDispositivos" />
              Tengo computador, tablet o celular para estudiar</label>
            <div class="grid-2">
              <div class="campo"><label>Medio de transporte</label>
                <select name="transporte" [(ngModel)]="accesibilidad.medioTransporte">
                  @for (m of catalogos?.mediosTransporte ?? []; track m) { <option [value]="m">{{ m | etiqueta }}</option> }
                </select></div>
              <div class="campo"><label>Distancia máxima que puedes recorrer (km)</label>
                <input type="number" min="0" name="distancia" [(ngModel)]="accesibilidad.distanciaMaximaKm" /></div>
              <div class="campo"><label>Modalidad preferida</label>
                <select name="modalidad" [(ngModel)]="accesibilidad.modalidadPreferida">
                  <option [ngValue]="undefined">Sin preferencia</option>
                  @for (m of catalogos?.modalidades ?? []; track m) { <option [value]="m">{{ m | etiqueta }}</option> }
                </select></div>
            </div>
            <button class="btn" [disabled]="guardando">Guardar</button>
          </form>
        }
      }
    </div>
  `,
  styles: [`
    .estudio { border: 1px dashed var(--borde); border-radius: 8px; padding: 12px; margin-bottom: 10px; }
  `],
})
export class CaracterizacionComponent implements OnInit {
  perfilService = inject(PerfilService);
  private catalogoService = inject(CatalogoService);

  pestanas: { id: Seccion; titulo: string }[] = [
    { id: 'personal', titulo: 'Información personal' },
    { id: 'victima', titulo: 'Situación de víctima' },
    { id: 'socio', titulo: 'Socioeconómica' },
    { id: 'trayectoria', titulo: 'Trayectoria educativa' },
    { id: 'intereses', titulo: 'Intereses' },
    { id: 'accesibilidad', titulo: 'Accesibilidad' },
  ];
  seccion: Seccion = 'personal';

  perfil: Perfil | null = null;
  catalogos: Catalogos | null = null;
  error: string | null = null;
  exito: string | null = null;
  guardando = false;

  // Modelos de cada formulario (se llenan con lo que ya existe en el perfil)
  personal: InformacionPersonal = {
    nombres: '', apellidos: '', tipoDocumento: 'CC', numeroDocumento: '', fechaNacimiento: '',
    genero: '', etnia: '', telefono: '', departamento: 'Quindío', municipio: '', zona: 'URBANA',
  };
  victima: SituacionVictima = { esVictima: true, inscritoRUV: false };
  socio: SituacionSocioeconomica = { estrato: 1, integrantesHogar: 1, personasACargo: 0, grupoSisben: 'NO_TIENE' };
  trayectoria: TrayectoriaEducativa = {
    nivelActual: 'NINGUNO', ultimoGradoAprobado: 0, anioUltimoEstudio: new Date().getFullYear(), estudios: [],
  };
  interesesSeleccionados = new Set<string>();
  accesibilidad: CondicionAccesibilidad = {
    tieneInternet: false, tieneDispositivos: false, medioTransporte: 'PUBLICO', distanciaMaximaKm: 5,
  };

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => (this.catalogos = c));
    this.perfilService.consultar().subscribe({
      next: (p) => this.cargarPerfil(p),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  cambiar(s: Seccion): void {
    this.seccion = s;
    this.error = null;
    this.exito = null;
  }

  seccionLista(s: Seccion): boolean {
    const p = this.perfil;
    if (!p) return false;
    switch (s) {
      case 'personal': return !!p.informacionPersonal;
      case 'victima': return !!p.situacionVictima;
      case 'socio': return !!p.situacionSocioeconomica;
      case 'trayectoria': return !!p.trayectoriaEducativa;
      case 'intereses': return p.intereses.length > 0;
      case 'accesibilidad': return !!p.condicionAccesibilidad;
      default: return false;
    }
  }

  aceptarAutorizacion(): void {
    this.perfilService.aceptarAutorizacion().subscribe((p) => (this.perfil = p));
  }

  /** Envía cualquier sección y actualiza el perfil con la respuesta. */
  guardar(peticion: Observable<Perfil>): void {
    this.error = null;
    this.exito = null;
    this.guardando = true;
    peticion.subscribe({
      next: (p) => {
        this.cargarPerfil(p);
        this.exito = p.completo
          ? '¡Perfil completo! Ya puedes generar tu ruta educativa.'
          : 'Información guardada correctamente.';
        this.guardando = false;
        this.irASiguientePendiente();
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.guardando = false;
      },
    });
  }

  guardarTrayectoria(): void {
    this.guardar(this.perfilService.guardarTrayectoria(this.trayectoria));
  }

  agregarEstudio(): void {
    this.trayectoria.estudios.push({ nivel: 'BASICA_PRIMARIA', estado: 'COMPLETO' });
  }

  quitarEstudio(i: number): void {
    this.trayectoria.estudios.splice(i, 1);
  }

  alternarInteres(area: string): void {
    if (this.interesesSeleccionados.has(area)) {
      this.interesesSeleccionados.delete(area);
    } else if (this.interesesSeleccionados.size < 5) {
      this.interesesSeleccionados.add(area);
    }
  }

  guardarIntereses(): void {
    const intereses = [...this.interesesSeleccionados].map((a) => ({ areaConocimiento: a }));
    this.guardar(this.perfilService.guardarIntereses(intereses));
  }

  private irASiguientePendiente(): void {
    const pendiente = this.pestanas.find((t) => !this.seccionLista(t.id));
    if (pendiente) this.seccion = pendiente.id;
  }

  /** Copia los datos guardados a los formularios (HU-11: editar lo existente). */
  private cargarPerfil(p: Perfil): void {
    this.perfil = p;
    if (p.informacionPersonal) this.personal = { ...p.informacionPersonal };
    if (p.situacionVictima) this.victima = { ...p.situacionVictima };
    if (p.situacionSocioeconomica) this.socio = { ...p.situacionSocioeconomica };
    if (p.trayectoriaEducativa) {
      this.trayectoria = {
        ...p.trayectoriaEducativa,
        estudios: p.trayectoriaEducativa.estudios.map((e) => ({ ...e })),
      };
    }
    if (p.condicionAccesibilidad) this.accesibilidad = { ...p.condicionAccesibilidad };
    this.interesesSeleccionados = new Set(p.intereses.map((i) => i.areaConocimiento));
  }
}
