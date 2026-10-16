import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/auth.model';
import { AuthService } from '../services/auth.service';

/** Exige sesión iniciada. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.estaAutenticado() ? true : inject(Router).createUrlTree(['/login']);
};

/** Exige un rol específico (HU-03). Uso: canActivate: [rolGuard('ADMINISTRADOR')] */
export function rolGuard(rol: Rol): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.estaAutenticado()) return router.createUrlTree(['/login']);
    return auth.rol === rol ? true : router.createUrlTree([auth.rutaInicio()]);
  };
}

/** Si ya hay sesión, no muestra login/registro. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.estaAutenticado() ? inject(Router).createUrlTree([auth.rutaInicio()]) : true;
};
