import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AlertaComponent } from '../../../components/alerta.component';
import {
  Catalogos,
  CondicionAccesibilidad,
  InformacionPersonal,
  Perfil,
  ResponsableMenor,
  SituacionSocioeconomica,
  SituacionVictima,
  TrayectoriaEducativa,
} from '../../../models/perfil.model';
import { CatalogoService } from '../../../services/catalogo.service';
import { PerfilService } from '../../../services/perfil.service';
import { EtiquetaPipe } from '../../../utils/etiqueta.pipe';
import { mensajeError } from '../../../utils/errores';
import { buscarDepartamento, municipioOficial, municipiosDe } from '../../../utils/ubicacion';

type Seccion = 'personal' | 'victima' | 'socio' | 'trayectoria' | 'intereses' | 'accesibilidad';

/**
 * Proceso 1: Caracterización del usuario (HU-04 a HU-09) y actualización del perfil (HU-11).
 * Cada pestaña guarda su sección por separado en el backend.
 */
@Component({
  selector: 'app-caracterizacion',
  standalone: true,
  imports: [FormsModule, RouterLink, AlertaComponent, EtiquetaPipe],
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

    @if (perfil?.completo) {
      <div class="card card-azul completo">
        <div>
          <strong>✓ Tu caracterización está completa.</strong>
          <span class="suave">Tu ruta educativa se actualiza sola cada vez que cambias tu perfil.</span>
        </div>
        <div class="fila">
          <a class="btn" routerLink="/beneficiario/oportunidades">Explorar ofertas</a>
          <a class="btn btn-sec" routerLink="/beneficiario/rutas">Ver mi ruta</a>
        </div>
      </div>
    }

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
          <form #f="ngForm" (ngSubmit)="guardarPersonal(f)" novalidate>
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
              <div class="campo"><label for="numeroDocumento">Número de documento *</label>
                <input id="numeroDocumento" name="numeroDocumento" [(ngModel)]="personal.numeroDocumento" required
                       inputmode="numeric" autocomplete="off" #doc="ngModel"
                       [class.invalido]="errorDocumento && (doc.dirty || doc.touched || f.submitted)"
                       [attr.aria-invalid]="!!errorDocumento" aria-describedby="ayudaDocumento" />
                @if (errorDocumento && (doc.dirty || doc.touched || f.submitted)) {
                  <small id="ayudaDocumento" class="error-campo" role="alert">{{ errorDocumento }}</small>
                } @else {
                  <small id="ayudaDocumento" class="suave">{{ personal.tipoDocumento === 'TI'
                    ? 'Solo números, 10 u 11 dígitos' : 'Solo números, de 6 a 10 dígitos' }}</small>
                }
              </div>
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
              <div class="campo"><label for="telefono">Teléfono</label>
                <input id="telefono" name="telefono" [(ngModel)]="personal.telefono" inputmode="tel" #tel="ngModel"
                       [class.invalido]="errorTelefono && (tel.dirty || tel.touched || f.submitted)"
                       [attr.aria-invalid]="!!errorTelefono" />
                @if (errorTelefono && (tel.dirty || tel.touched || f.submitted)) {
                  <small class="error-campo" role="alert">{{ errorTelefono }}</small>
                } @else {
                  <small class="suave">Opcional. Solo números, de 7 a 10 dígitos</small>
                }
              </div>
              <!-- Departamento 1 ── 1..* municipios: el municipio depende del departamento elegido -->
              <div class="campo"><label for="departamento">Departamento *</label>
                <select id="departamento" name="departamento" [(ngModel)]="personal.departamento" required
                        (ngModelChange)="cambiarDepartamento()">
                  <option value="" disabled>Selecciona el departamento</option>
                  @for (d of catalogos?.departamentos ?? []; track d.codigo) { <option [value]="d.nombre">{{ d.nombre }}</option> }
                </select></div>
              <div class="campo"><label for="municipio">Municipio *</label>
                <select id="municipio" name="municipio" [(ngModel)]="personal.municipio" required
                        [disabled]="!municipios.length">
                  <option value="" disabled>{{ municipios.length ? 'Selecciona el municipio' : 'Primero elige el departamento' }}</option>
                  @for (m of municipios; track m) { <option [value]="m">{{ m }}</option> }
                </select></div>
              <div class="campo"><label>Zona *</label>
                <select name="zona" [(ngModel)]="personal.zona" required>
                  <option value="URBANA">Urbana</option><option value="RURAL">Rural</option>
                </select></div>
            </div>

            <!-- Responsable obligatorio: se despliega solo si la fecha de nacimiento indica menor de 18 años -->
            @if (esMenorDeEdad) {
              <fieldset class="responsable">
                <legend>Responsable mayor de edad *</legend>
                <p class="suave">
                  Según la fecha de nacimiento tienes {{ edadBeneficiario }} años. Como eres menor de edad, debes
                  registrar los datos de un adulto responsable (padre, madre, acudiente o tutor legal).
                </p>
                <div class="grid-2">
                  <div class="campo"><label>Nombres del responsable *</label>
                    <input name="respNombres" [(ngModel)]="responsable.nombres" required maxlength="80" /></div>
                  <div class="campo"><label>Apellidos del responsable *</label>
                    <input name="respApellidos" [(ngModel)]="responsable.apellidos" required maxlength="80" /></div>
                  <div class="campo"><label>Parentesco *</label>
                    <select name="respParentesco" [(ngModel)]="responsable.parentesco" required>
                      <option value="" disabled>Selecciona una opción</option>
                      @for (par of parentescos; track par) { <option>{{ par }}</option> }
                    </select></div>
                  <div class="campo"><label>Tipo de documento *</label>
                    <input name="respTipoDocumento" value="Cédula de ciudadanía" readonly /></div>
                  <div class="campo"><label>Número de documento *</label>
                    <input name="respNumeroDocumento" [(ngModel)]="responsable.numeroDocumento" required
                           inputmode="numeric" #respDoc="ngModel"
                           [class.invalido]="errorDocumentoResponsable && (respDoc.dirty || respDoc.touched || f.submitted)" />
                    @if (errorDocumentoResponsable && (respDoc.dirty || respDoc.touched || f.submitted)) {
                      <small class="error-campo" role="alert">{{ errorDocumentoResponsable }}</small>
                    }
                  </div>
                  <div class="campo"><label>Fecha de nacimiento * (mayor de 18 años)</label>
                    <input type="date" name="respFechaNacimiento" [(ngModel)]="responsable.fechaNacimiento" required
                           [max]="fechaMaximaResponsable" /></div>
                  <div class="campo"><label>Teléfono *</label>
                    <input name="respTelefono" [(ngModel)]="responsable.telefono" required inputmode="tel"
                           #respTel="ngModel"
                           [class.invalido]="errorTelefonoResponsable && (respTel.dirty || respTel.touched || f.submitted)" />
                    @if (errorTelefonoResponsable && (respTel.dirty || respTel.touched || f.submitted)) {
                      <small class="error-campo" role="alert">{{ errorTelefonoResponsable }}</small>
                    }
                  </div>
                  <div class="campo"><label>Correo electrónico</label>
                    <input type="email" name="respCorreo" [(ngModel)]="responsable.correo" email maxlength="120" /></div>
                </div>
                @if (responsable.fechaNacimiento && !responsableEsMayor) {
                  <div class="alerta alerta-error">El responsable debe ser mayor de edad (18 años o más).</div>
                }
                @if (responsable.numeroDocumento && responsable.numeroDocumento === personal.numeroDocumento) {
                  <div class="alerta alerta-error">El documento del responsable no puede ser el mismo del beneficiario.</div>
                }
              </fieldset>
            }

            <!-- El botón ya no se bloquea en silencio: al pulsarlo con errores se explica qué corregir -->
            @if (f.submitted && erroresPersonal(f).length) {
              <div class="alerta alerta-error" role="alert">
                <strong>No se guardó. Corrige lo siguiente:</strong>
                <ul>@for (m of erroresPersonal(f); track m) { <li>{{ m }}</li> }</ul>
              </div>
            }
            <button class="btn" [disabled]="guardando">Guardar</button>
          </form>
        }

        <!-- ================= HU-05 Situación de víctima ================= -->
        @case ('victima') {
          <form #f="ngForm" (ngSubmit)="guardarVictima()">
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
                <!-- Departamento 1 ── 1..* municipios, igual que en la información personal -->
                <div class="campo"><label for="departamentoExpulsor">Departamento expulsor</label>
                  <select id="departamentoExpulsor" name="departamentoExpulsor" [(ngModel)]="victima.departamentoExpulsor"
                          (ngModelChange)="cambiarDepartamentoExpulsor()">
                    <option value="">Sin especificar</option>
                    @for (d of catalogos?.departamentos ?? []; track d.codigo) { <option [value]="d.nombre">{{ d.nombre }}</option> }
                  </select></div>
                <div class="campo"><label for="municipioExpulsor">Municipio expulsor</label>
                  <select id="municipioExpulsor" name="municipioExpulsor" [(ngModel)]="victima.municipioExpulsor"
                          [disabled]="!municipiosExpulsor.length">
                    <option value="">{{ municipiosExpulsor.length ? 'Selecciona el municipio' : 'Primero elige el departamento' }}</option>
                    @for (m of municipiosExpulsor; track m) { <option [value]="m">{{ m }}</option> }
                  </select></div>
              </div>
            }
            <p class="suave">Esta información está protegida por la Ley 1448 de 2011.</p>
            @for (m of erroresVictima; track m) { <div class="alerta alerta-error">{{ m }}</div> }
            <button class="btn" [disabled]="f.invalid || guardando || erroresVictima.length > 0">Guardar</button>
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
            @for (m of erroresSocio; track m) { <div class="alerta alerta-error">{{ m }}</div> }
            <button class="btn" [disabled]="f.invalid || guardando || erroresSocio.length > 0">Guardar</button>
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
            @for (m of erroresTrayectoria; track m) { <div class="alerta alerta-error">{{ m }}</div> }
            <div class="fila">
              <button type="button" class="btn btn-sec" (click)="agregarEstudio()">+ Agregar estudio</button>
              <button class="btn" [disabled]="f.invalid || guardando || erroresTrayectoria.length > 0">Guardar</button>
            </div>
          </form>
        }

        <!-- ================= HU-08 Intereses profesionales ================= -->
        @case ('intereses') {
          <div class="fila entre">
            <p class="suave">Selecciona entre 1 y {{ maxIntereses }} áreas que te interesen.</p>
            <span class="badge" [class.badge-ok]="interesesSeleccionados.size > 0"
                  [class.badge-aviso]="interesesSeleccionados.size >= maxIntereses">
              {{ interesesSeleccionados.size }} de {{ maxIntereses }} seleccionadas
            </span>
          </div>
          <div class="grid-2">
            @for (a of catalogos?.areasConocimiento ?? []; track a) {
              <label class="check" [class.bloqueada]="!interesesSeleccionados.has(a) && interesesSeleccionados.size >= maxIntereses">
                <input type="checkbox" [checked]="interesesSeleccionados.has(a)"
                       [disabled]="!interesesSeleccionados.has(a) && interesesSeleccionados.size >= maxIntereses"
                       (change)="alternarInteres(a, $event)" />
                {{ a }}
              </label>
            }
          </div>
          @if (avisoIntereses) {
            <div class="alerta alerta-aviso" role="alert">{{ avisoIntereses }}</div>
          }
          <button class="btn" [disabled]="guardando" (click)="guardarIntereses()">
            Guardar
          </button>
        }

        <!-- ================= HU-09 Accesibilidad ================= -->
        @case ('accesibilidad') {
          <form #f="ngForm" (ngSubmit)="guardar(perfilService.guardarAccesibilidad(accesibilidad))">
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
            @for (m of erroresAccesibilidad; track m) { <div class="alerta alerta-error">{{ m }}</div> }
            <button class="btn" [disabled]="f.invalid || guardando || erroresAccesibilidad.length > 0">Guardar</button>
          </form>
        }
      }
    </div>

    <!-- Aviso flotante: siempre visible aunque el formulario sea largo (antes el error quedaba arriba, fuera de vista) -->
    @if (toast) {
      <div class="toast" [class.toast-error]="toast.tipo === 'error'" role="status" aria-live="polite">
        <span>{{ toast.texto }}</span>
        <button type="button" class="cerrar" aria-label="Cerrar aviso" (click)="toast = null">×</button>
      </div>
    }

    <!-- Notificación: caracterización completada → invitar a navegar las ofertas -->
    @if (mostrarAvisoCompleto) {
      <div class="velo" (click)="mostrarAvisoCompleto = false"></div>
      <div class="modal card" role="dialog" aria-modal="true" aria-labelledby="tituloCompleto">
        <div class="icono">🎉</div>
        <h2 id="tituloCompleto">¡Completaste tu caracterización!</h2>
        <p>Tu perfil está al 100 %. Con esta información ya generamos tu ruta educativa recomendada y puedes
          explorar toda la oferta educativa disponible.</p>
        <div class="fila centro">
          <a class="btn" routerLink="/beneficiario/oportunidades" (click)="mostrarAvisoCompleto = false">Explorar ofertas</a>
          <a class="btn btn-sec" routerLink="/beneficiario/rutas" (click)="mostrarAvisoCompleto = false">Ver mi ruta</a>
          <button type="button" class="btn btn-sec" (click)="mostrarAvisoCompleto = false">Seguir en mi perfil</button>
        </div>
      </div>
    }
  `,
  styles: [`
    .error-campo { color: var(--error); font-weight: 600; }
    .campo input.invalido, .campo select.invalido { border-color: var(--error); background: var(--error-claro); }
    .alerta ul { margin: 6px 0 0; padding-left: 20px; }
    .check.bloqueada { opacity: 0.5; cursor: not-allowed; }
    .completo { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
    .completo .suave { display: block; }
    .toast { position: fixed; right: 20px; bottom: 20px; z-index: 50; max-width: 420px; display: flex; gap: 12px;
      align-items: flex-start; padding: 12px 14px; border-radius: 12px; background: var(--exito-claro);
      color: var(--exito); border: 1px solid var(--exito); box-shadow: 0 6px 20px rgba(1, 32, 96, 0.2);
      font-weight: 600; }
    .toast-error { background: var(--error-claro); color: var(--error); border-color: var(--error); }
    .toast .cerrar { background: none; border: none; font-size: 1.2rem; line-height: 1; cursor: pointer; color: inherit; }
    .velo { position: fixed; inset: 0; background: rgba(1, 32, 96, 0.45); z-index: 60; }
    .modal { position: fixed; z-index: 61; left: 50%; top: 50%; transform: translate(-50%, -50%);
      width: min(480px, calc(100% - 32px)); text-align: center; border: 3px solid var(--marino); border-radius: 24px; }
    .modal .icono { font-size: 2.4rem; }
    .modal .centro { justify-content: center; }
    .estudio { border: 1px dashed var(--azul-claro); border-radius: 10px; padding: 12px; margin-bottom: 10px;
      background: var(--azul-palido); }
    .responsable { border: 2px solid var(--rosa); border-radius: 14px; background: var(--rosa-claro);
      padding: 14px 16px 6px; margin: 4px 0 16px; }
    .responsable legend { padding: 2px 12px; border-radius: 999px; background: var(--morado); color: #fff;
      font-weight: 600; font-size: 0.9rem; }
    .responsable p.suave { margin: 4px 0 12px; }
    .responsable input[readonly] { background: var(--azul-palido); color: var(--texto-suave); }
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

  /** Aviso flotante (éxito o error) que se ve aunque el usuario esté abajo en el formulario. */
  toast: { tipo: 'ok' | 'error'; texto: string } | null = null;
  private toastTimer?: ReturnType<typeof setTimeout>;
  /** Ventana "¡Completaste tu caracterización!" con el enlace a las ofertas. */
  mostrarAvisoCompleto = false;

  /** HU-08: máximo de áreas de interés (igual que el backend). */
  readonly maxIntereses = 5;
  avisoIntereses: string | null = null;

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

  /** Adulto responsable: solo se envía si el beneficiario es menor de 18 años. */
  responsable: ResponsableMenor = this.responsableVacio();
  parentescos = ['Madre', 'Padre', 'Abuelo(a)', 'Tío(a)', 'Hermano(a)', 'Acudiente', 'Tutor legal', 'Otro'];
  accesibilidad: CondicionAccesibilidad = {
    tieneInternet: false, tieneDispositivos: false, medioTransporte: 'PUBLICO', distanciaMaximaKm: 5,
  };

  ngOnInit(): void {
    this.catalogoService.obtener().subscribe((c) => {
      this.catalogos = c;
      this.ajustarUbicacion();
    });
    this.perfilService.consultar().subscribe({
      next: (p) => this.cargarPerfil(p),
      error: (e) => (this.error = mensajeError(e)),
    });
  }

  cambiar(s: Seccion): void {
    this.seccion = s;
    this.error = null;
    this.exito = null;
    this.avisoIntereses = null;
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
    const estabaCompleto = !!this.perfil?.completo;
    peticion.subscribe({
      next: (p) => {
        this.cargarPerfil(p);
        if (p.completo && !estabaCompleto) {
          // Notificación de caracterización completada (también llega a la bandeja y al correo)
          this.exito = '¡Perfil completo! Tu ruta educativa ya se generó automáticamente.';
          this.mostrarAvisoCompleto = true;
        } else {
          this.exito = p.completo
            ? 'Información guardada. Tu ruta educativa se actualizó automáticamente.'
            : 'Información guardada correctamente.';
        }
        this.mostrarToast('ok', this.exito);
        this.guardando = false;
        this.irASiguientePendiente();
      },
      error: (e) => {
        this.error = mensajeError(e);
        this.mostrarToast('error', 'No se guardó: ' + this.error);
        this.guardando = false;
      },
    });
  }

  mostrarToast(tipo: 'ok' | 'error', texto: string): void {
    clearTimeout(this.toastTimer);
    this.toast = { tipo, texto };
    this.toastTimer = setTimeout(() => (this.toast = null), tipo === 'error' ? 9000 : 5000);
  }

  // ---------------------------------------------------------------- Validaciones con mensaje (HU-04)
  // Antes el campo solo se ponía rojo y el botón Guardar se bloqueaba sin decir por qué.

  /** Cédula: 6 a 10 dígitos. Tarjeta de identidad: 10 u 11 dígitos. Solo números. */
  private static errorNumeroDocumento(valor: string | undefined, tipo: string, nombre: string): string | null {
    const v = (valor ?? '').trim();
    if (!v) return `${nombre} es obligatorio`;
    if (/\D/.test(v)) return `${nombre} solo puede tener números (sin letras, puntos ni espacios)`;
    const [min, max] = tipo === 'TI' ? [10, 11] : [6, 10];
    const tipoTexto = tipo === 'TI' ? 'La tarjeta de identidad' : 'La cédula';
    if (v.length < min || v.length > max) {
      return `${tipoTexto} debe tener ${min === 10 && max === 11 ? '10 u 11' : `entre ${min} y ${max}`} dígitos; escribiste ${v.length}`;
    }
    return null;
  }

  private static errorNumeroTelefono(valor: string | undefined, obligatorio: boolean, nombre: string): string | null {
    const v = (valor ?? '').trim();
    if (!v) return obligatorio ? `${nombre} es obligatorio` : null;
    if (/\D/.test(v)) return `${nombre} solo puede tener números (sin espacios, guiones ni +57)`;
    if (v.length < 7 || v.length > 10) return `${nombre} debe tener entre 7 y 10 dígitos; escribiste ${v.length}`;
    return null;
  }

  get errorDocumento(): string | null {
    return CaracterizacionComponent.errorNumeroDocumento(this.personal.numeroDocumento, this.personal.tipoDocumento,
      'El número de documento');
  }

  get errorTelefono(): string | null {
    return CaracterizacionComponent.errorNumeroTelefono(this.personal.telefono, false, 'El teléfono');
  }

  get errorDocumentoResponsable(): string | null {
    return CaracterizacionComponent.errorNumeroDocumento(this.responsable.numeroDocumento, 'CC',
      'La cédula del responsable');
  }

  get errorTelefonoResponsable(): string | null {
    return CaracterizacionComponent.errorNumeroTelefono(this.responsable.telefono, true, 'El teléfono del responsable');
  }

  /** Todos los problemas de la pestaña "Información personal", en palabras. */
  erroresPersonal(f: NgForm): string[] {
    const e: string[] = [];
    const c = f.controls;
    if (c['nombres']?.invalid) e.push('Escribe tus nombres');
    if (c['apellidos']?.invalid) e.push('Escribe tus apellidos');
    if (this.errorDocumento) e.push(this.errorDocumento);
    if (!this.personal.fechaNacimiento) e.push('Indica tu fecha de nacimiento');
    if (this.errorTelefono) e.push(this.errorTelefono);
    if (!this.personal.departamento) e.push('Selecciona el departamento');
    if (!this.personal.municipio) e.push('Selecciona el municipio');
    if (this.esMenorDeEdad) {
      const r = this.responsable;
      if (!r.nombres?.trim() || !r.apellidos?.trim()) e.push('Escribe los nombres y apellidos del responsable');
      if (!r.parentesco) e.push('Selecciona el parentesco del responsable');
      if (this.errorDocumentoResponsable) e.push(this.errorDocumentoResponsable);
      if (!r.fechaNacimiento) e.push('Indica la fecha de nacimiento del responsable');
      else if (!this.responsableEsMayor) e.push('El responsable debe ser mayor de edad (18 años o más)');
      if (this.errorTelefonoResponsable) e.push(this.errorTelefonoResponsable);
      if (c['respCorreo']?.invalid) e.push('El correo del responsable no es válido');
      if (r.numeroDocumento && r.numeroDocumento === this.personal.numeroDocumento) {
        e.push('El documento del responsable no puede ser el mismo del beneficiario');
      }
    }
    return e;
  }

  // ---------------------------------------------------------------- Departamento → Municipio

  /** Municipios del departamento expulsor (HU-05). */
  get municipiosExpulsor(): string[] {
    return municipiosDe(this.catalogos?.departamentos, this.victima.departamentoExpulsor);
  }

  cambiarDepartamentoExpulsor(): void {
    this.victima.municipioExpulsor = municipioOficial(this.municipiosExpulsor, this.victima.municipioExpulsor);
  }

  get municipios(): string[] {
    return municipiosDe(this.catalogos?.departamentos, this.personal.departamento);
  }

  /** Al cambiar de departamento se borra el municipio si no pertenece al nuevo. */
  cambiarDepartamento(): void {
    this.personal.municipio = municipioOficial(this.municipios, this.personal.municipio);
  }

  /** Deja el departamento y el municipio guardados con el nombre exacto de la lista (ej. "Quindio" → "Quindío"). */
  private ajustarUbicacion(): void {
    const deps = this.catalogos?.departamentos;
    if (!deps) return;
    const d = buscarDepartamento(deps, this.personal.departamento);
    this.personal.departamento = d?.nombre ?? '';
    this.personal.municipio = d ? municipioOficial(d.municipios, this.personal.municipio) : '';

    // Situación de víctima: los registros viejos solo tienen el municipio escrito a mano; se busca su departamento
    const v = this.victima;
    const dv = buscarDepartamento(deps, v.departamentoExpulsor)
      ?? (v.municipioExpulsor ? deps.find((x) => municipioOficial(x.municipios, v.municipioExpulsor)) : undefined);
    v.departamentoExpulsor = dv?.nombre ?? '';
    v.municipioExpulsor = dv ? municipioOficial(dv.municipios, v.municipioExpulsor) : '';
  }

  // ---------------------------------------------------------------- Responsable de menores (HU-04)

  /** Edad calculada a partir de una fecha yyyy-MM-dd (null si la fecha no es válida). */
  private calcularEdad(fecha: string | undefined | null): number | null {
    if (!fecha) return null;
    const [a, m, d] = fecha.split('-').map(Number);
    if (!a || !m || !d) return null;
    const hoy = new Date();
    let edad = hoy.getFullYear() - a;
    if (hoy.getMonth() + 1 < m || (hoy.getMonth() + 1 === m && hoy.getDate() < d)) edad--;
    return edad;
  }

  get edadBeneficiario(): number | null {
    return this.calcularEdad(this.personal.fechaNacimiento);
  }

  get esMenorDeEdad(): boolean {
    const edad = this.edadBeneficiario;
    return edad !== null && edad >= 0 && edad < 18;
  }

  get responsableEsMayor(): boolean {
    const edad = this.calcularEdad(this.responsable.fechaNacimiento);
    return edad !== null && edad >= 18;
  }

  /** Si el beneficiario es menor, el responsable debe ser adulto y tener un documento distinto. */
  get responsableValido(): boolean {
    if (!this.esMenorDeEdad) return true;
    return this.responsableEsMayor && this.responsable.numeroDocumento !== this.personal.numeroDocumento;
  }

  /** Fecha máxima permitida para el responsable: hoy menos 18 años. */
  get fechaMaximaResponsable(): string {
    const f = new Date();
    f.setFullYear(f.getFullYear() - 18);
    return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`;
  }

  guardarPersonal(f: NgForm): void {
    f.control.markAllAsTouched();
    const errores = this.erroresPersonal(f);
    if (errores.length) {
      this.mostrarToast('error', `No se guardó: ${errores.length === 1 ? errores[0] : 'revisa los campos marcados en rojo'}`);
      return;
    }
    const datos: InformacionPersonal = {
      ...this.personal,
      responsable: this.esMenorDeEdad ? { ...this.responsable, tipoDocumento: 'CC' } : null,
    };
    this.guardar(this.perfilService.guardarInformacionPersonal(datos));
  }

  private responsableVacio(): ResponsableMenor {
    return {
      nombres: '', apellidos: '', tipoDocumento: 'CC', numeroDocumento: '', fechaNacimiento: '',
      parentesco: '', telefono: '', correo: '',
    };
  }

  guardarTrayectoria(): void {
    this.guardar(this.perfilService.guardarTrayectoria(this.trayectoria));
  }

  // ---------------------------------------------------------------- RNF-07: guardar en un solo intento
  // Las mismas reglas que valida el backend (CaracterizacionService y PerfilEducativo.inconsistencias()),
  // para que el botón Guardar no se habilite con datos que el servidor va a rechazar.

  /** Orden de los niveles educativos (igual al enum NivelEducativo del backend). */
  private static readonly NIVELES = ['NINGUNO', 'BASICA_PRIMARIA', 'BASICA_SECUNDARIA', 'MEDIA', 'TECNICO',
    'TECNOLOGICO', 'UNIVERSITARIO'];

  private get anioNacimiento(): number | null {
    const f = this.perfil?.informacionPersonal?.fechaNacimiento ?? this.personal.fechaNacimiento;
    const anio = f ? Number(f.substring(0, 4)) : NaN;
    return Number.isNaN(anio) ? null : anio;
  }

  /** HU-05: si no es víctima no puede estar en el RUV (se limpia al guardar), y el hecho no puede ser antes de nacer. */
  guardarVictima(): void {
    const datos: SituacionVictima = {
      ...this.victima,
      inscritoRUV: this.victima.esVictima && this.victima.inscritoRUV,
      departamentoExpulsor: this.victima.departamentoExpulsor || undefined,
      municipioExpulsor: this.victima.municipioExpulsor || undefined,
    };
    this.guardar(this.perfilService.guardarSituacionVictima(datos));
  }

  get erroresVictima(): string[] {
    const e: string[] = [];
    const v = this.victima;
    if (v.esVictima && !v.hechoVictimizante) e.push('Selecciona el hecho victimizante');
    if (v.esVictima && v.municipioExpulsor && !v.departamentoExpulsor) {
      e.push('Selecciona el departamento del municipio expulsor');
    }
    const nacimiento = this.perfil?.informacionPersonal?.fechaNacimiento;
    if (v.esVictima && v.fechaHecho && nacimiento && v.fechaHecho < nacimiento) {
      e.push('La fecha del hecho victimizante no puede ser anterior a tu fecha de nacimiento');
    }
    return e;
  }

  get erroresSocio(): string[] {
    const e: string[] = [];
    if (this.socio.personasACargo > this.socio.integrantesHogar) {
      e.push('Las personas a cargo no pueden superar a los integrantes del hogar');
    }
    return e;
  }

  get erroresTrayectoria(): string[] {
    const e: string[] = [];
    const t = this.trayectoria;
    const g = Number(t.ultimoGradoAprobado ?? 0);
    const nivel = CaracterizacionComponent.NIVELES.indexOf(t.nivelActual);
    switch (t.nivelActual) {
      case 'NINGUNO':
        if (g > 4) e.push(`Con grado ${g} aprobado tu nivel no puede ser "Ninguno"`);
        break;
      case 'BASICA_PRIMARIA':
        if (g < 5 || g > 8) e.push('Básica primaria terminada corresponde a un último grado entre 5 y 8');
        break;
      case 'BASICA_SECUNDARIA':
        if (g < 9 || g > 10) e.push('Básica secundaria terminada corresponde a un último grado de 9 o 10');
        break;
      default:
        if (g !== 11) e.push('Para este nivel el último grado aprobado debe ser 11');
    }
    if (t.anioUltimoEstudio > new Date().getFullYear()) e.push('El año del último estudio no puede ser futuro');
    const nacimiento = this.anioNacimiento;
    if (nacimiento !== null && t.anioUltimoEstudio < nacimiento + 5) {
      e.push('El año del último estudio no es coherente con tu fecha de nacimiento');
    }
    t.estudios.forEach((x, i) => {
      if (x.anioInicio && x.anioFin && x.anioFin < x.anioInicio) {
        e.push(`Estudio ${i + 1}: el año de fin es menor que el de inicio`);
      }
      if (x.estado === 'COMPLETO' && CaracterizacionComponent.NIVELES.indexOf(x.nivel) > nivel) {
        e.push(`Estudio ${i + 1}: si lo terminaste, tu último nivel aprobado debe ser al menos ese nivel`);
      }
    });
    return e;
  }

  get erroresAccesibilidad(): string[] {
    const e: string[] = [];
    if (this.accesibilidad.modalidadPreferida === 'VIRTUAL' && !this.accesibilidad.tieneInternet) {
      e.push('Elegiste la modalidad virtual como preferida pero no marcaste que tienes internet');
    }
    return e;
  }

  agregarEstudio(): void {
    this.trayectoria.estudios.push({ nivel: 'BASICA_PRIMARIA', estado: 'COMPLETO' });
  }

  quitarEstudio(i: number): void {
    this.trayectoria.estudios.splice(i, 1);
  }

  /**
   * HU-08: máximo 5 áreas. Antes la casilla número 6 se veía marcada aunque no se guardaba; ahora las demás
   * casillas se bloquean al llegar a 5 y, si alguna llega a marcarse, se desmarca y se avisa.
   */
  alternarInteres(area: string, evento?: Event): void {
    const casilla = evento?.target as HTMLInputElement | undefined;
    if (this.interesesSeleccionados.has(area)) {
      this.interesesSeleccionados.delete(area);
    } else if (this.interesesSeleccionados.size < this.maxIntereses) {
      this.interesesSeleccionados.add(area);
    } else {
      if (casilla) casilla.checked = false;
      this.avisoIntereses = `Solo puedes elegir hasta ${this.maxIntereses} áreas. Desmarca una para elegir "${area}".`;
      return;
    }
    this.avisoIntereses = this.interesesSeleccionados.size >= this.maxIntereses
      ? `Llegaste al máximo de ${this.maxIntereses} áreas. Para elegir otra, primero desmarca una.`
      : null;
  }

  guardarIntereses(): void {
    const total = this.interesesSeleccionados.size;
    if (total === 0 || total > this.maxIntereses) {
      this.avisoIntereses = total === 0
        ? 'Selecciona al menos 1 área de interés.'
        : `Seleccionaste ${total} áreas; el máximo es ${this.maxIntereses}. Desmarca ${total - this.maxIntereses}.`;
      this.mostrarToast('error', 'No se guardó: ' + this.avisoIntereses);
      return;
    }
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
    if (p.informacionPersonal) {
      this.personal = { ...p.informacionPersonal };
      this.responsable = p.informacionPersonal.responsable
        ? { ...p.informacionPersonal.responsable, correo: p.informacionPersonal.responsable.correo ?? '' }
        : this.responsableVacio();
    }
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
    this.ajustarUbicacion();
  }
}
