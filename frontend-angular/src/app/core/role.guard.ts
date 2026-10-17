import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, RolApp } from './auth.service';

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
