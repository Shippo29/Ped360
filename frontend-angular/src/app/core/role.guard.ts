import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, RolApp } from './auth.service';

/**
 * Restringe una ruta a uno o más App Roles. Se usa en app.routes.ts, p.ej.
 * canActivate: [roleGuard(['Admin'])] para las pantallas de administración
 * del catálogo.
 */
export function roleGuard(rolesPermitidos: RolApp[]): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    auth.inicializarCuentaActiva();
    await auth.cargarRoles();

    if (!auth.rolesListos()) {
      return false;
    }

    const permitido = rolesPermitidos.some((rol) => auth.tieneRol(rol));
    if (!permitido) {
      router.navigate(['/']);
      return false;
    }
    return true;
  };
}
