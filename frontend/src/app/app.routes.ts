import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout.component';
import { authGuard, invitadoGuard, rolGuard } from './utils/auth.guard';

/** Rutas de la aplicación. Las páginas se cargan de forma diferida (lazy loading). */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },

  // ---------- Autenticación (públicas)
  {
    path: 'login',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./pages/autenticacion/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./pages/autenticacion/registro.component').then((m) => m.RegistroComponent),
  },

  // ---------- Beneficiario
  {
    path: 'beneficiario',
    component: LayoutComponent,
    canActivate: [authGuard, rolGuard('BENEFICIARIO')],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/beneficiario/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'caracterizacion',
        loadComponent: () =>
          import('./pages/beneficiario/caracterizacion/caracterizacion.component').then((m) => m.CaracterizacionComponent),
      },
      {
        path: 'oportunidades',
        loadComponent: () =>
          import('./pages/beneficiario/oportunidades/oportunidades.component').then((m) => m.OportunidadesComponent),
      },
      {
        path: 'rutas',
        loadComponent: () =>
          import('./pages/beneficiario/rutas-educativas/rutas-educativas.component').then((m) => m.RutasEducativasComponent),
      },
      {
        path: 'postulaciones',
        loadComponent: () =>
          import('./pages/beneficiario/postulaciones/postulaciones.component').then((m) => m.PostulacionesComponent),
      },
      {
        path: 'postulaciones/:id',
        loadComponent: () =>
          import('./pages/beneficiario/seguimiento/seguimiento.component').then((m) => m.SeguimientoComponent),
      },
    ],
  },

  // ---------- Administrador
  {
    path: 'admin',
    component: LayoutComponent,
    canActivate: [authGuard, rolGuard('ADMINISTRADOR')],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/administrador/dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
      },
      {
        path: 'instituciones',
        loadComponent: () =>
          import('./pages/administrador/instituciones/instituciones.component').then((m) => m.InstitucionesComponent),
      },
      {
        path: 'programas',
        loadComponent: () =>
          import('./pages/administrador/programas/programas.component').then((m) => m.ProgramasComponent),
      },
      {
        path: 'oportunidades',
        loadComponent: () =>
          import('./pages/administrador/oportunidades/admin-oportunidades.component').then(
            (m) => m.AdminOportunidadesComponent,
          ),
      },
      {
        path: 'requisitos',
        loadComponent: () =>
          import('./pages/administrador/requisitos/requisitos.component').then((m) => m.RequisitosComponent),
      },
      {
        path: 'beneficios',
        loadComponent: () =>
          import('./pages/administrador/beneficios/beneficios.component').then((m) => m.BeneficiosComponent),
      },
      {
        path: 'postulaciones',
        loadComponent: () =>
          import('./pages/administrador/postulaciones/admin-postulaciones.component').then(
            (m) => m.AdminPostulacionesComponent,
          ),
      },
      {
        path: 'beneficiarios',
        loadComponent: () =>
          import('./pages/administrador/beneficiarios/beneficiarios.component').then((m) => m.BeneficiariosComponent),
      },
    ],
  },

  { path: '**', redirectTo: 'login' },
];
