import { Injectable, computed, signal } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo, InteractionRequiredAuthError } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export type RolApp = 'Admin' | 'Operador' | 'Cliente';

const ROLES_APP = new Set<string>(['Admin', 'Operador', 'Cliente']);

function rolesDeClaims(claims: unknown): RolApp[] {
  if (!claims || typeof claims !== 'object' || !('roles' in claims)) {
    return [];
  }

  const roles = (claims as { roles?: unknown }).roles;
  if (!Array.isArray(roles)) {
    return [];
  }

  return roles.filter(
    (rol): rol is RolApp => typeof rol === 'string' && ROLES_APP.has(rol)
  );
}

function decodificarPayload(token: string): unknown {
  const segmento = token.split('.')[1];
  if (!segmento) {
    throw new Error('El access token no tiene un payload JWT válido.');
  }

  const base64 = segmento.replace(/-/g, '+').replace(/_/g, '/');
  const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')), (c) =>
    c.charCodeAt(0)
  );
  return JSON.parse(new TextDecoder().decode(bytes)) as unknown;
}

export function combinarRolesTokens(idTokenClaims: unknown, accessTokenClaims: unknown): RolApp[] {
  return [...new Set([...rolesDeClaims(idTokenClaims), ...rolesDeClaims(accessTokenClaims)])];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _cuenta = signal<AccountInfo | null>(null);
  private readonly _accessTokenClaims = signal<unknown>(null);
  private readonly _rolesListos = signal(false);
  private readonly _errorRoles = signal('');
  private cargaRolesEnCurso: Promise<void> | null = null;

  readonly cuenta = this._cuenta.asReadonly();
  readonly nombreUsuario = computed(() => this._cuenta()?.username ?? '');
  readonly estaAutenticado = computed(() => this._cuenta() !== null);
  readonly rolesListos = this._rolesListos.asReadonly();
  readonly errorRoles = this._errorRoles.asReadonly();
  readonly roles = computed<RolApp[]>(() =>
    combinarRolesTokens(this._cuenta()?.idTokenClaims, this._accessTokenClaims())
  );

  readonly esAdmin = computed(() => this.roles().includes('Admin'));
  readonly esOperador = computed(() => this.roles().includes('Operador'));
  readonly esCliente = computed(() => this.roles().includes('Cliente'));
  /** Admin/Operador comparten las pantallas de gestión operativa de pedidos. */
  readonly puedeGestionarPedidos = computed(() => this.esAdmin() || this.esOperador());

  constructor(private msalService: MsalService) {}

  inicializarCuentaActiva(): void {
    let cuenta = this.msalService.instance.getActiveAccount();
    if (!cuenta && this.msalService.instance.getAllAccounts().length > 0) {
      cuenta = this.msalService.instance.getAllAccounts()[0];
      this.msalService.instance.setActiveAccount(cuenta);
    }

    const cuentaAnterior = this._cuenta();
    if (cuentaAnterior?.homeAccountId !== cuenta?.homeAccountId) {
      this._accessTokenClaims.set(null);
      this._errorRoles.set('');
      this._rolesListos.set(!cuenta);
    }
    this._cuenta.set(cuenta);

    if (!cuenta) {
      this._accessTokenClaims.set(null);
      this._errorRoles.set('');
      this._rolesListos.set(true);
      return;
    }

    if (!this._rolesListos() && !this.cargaRolesEnCurso) {
      void this.cargarRoles();
    }
  }

  cargarRoles(): Promise<void> {
    if (this.cargaRolesEnCurso) {
      return this.cargaRolesEnCurso;
    }

    const cuenta = this._cuenta();
    if (!cuenta) {
      this._rolesListos.set(true);
      return Promise.resolve();
    }

    this._rolesListos.set(false);
    this._errorRoles.set('');
    this._accessTokenClaims.set(null);
    this.cargaRolesEnCurso = this.obtenerRolesAccessToken(cuenta)
      .finally(() => {
        this.cargaRolesEnCurso = null;
      });

    return this.cargaRolesEnCurso;
  }

  login(): void {
    this.msalService.loginRedirect({ scopes: [environment.azure.scope] });
  }

  logout(): void {
    this.msalService.logoutRedirect();
  }

  tieneRol(rol: RolApp): boolean {
    return this.roles().includes(rol);
  }

  private async obtenerRolesAccessToken(cuenta: AccountInfo): Promise<void> {
    try {
      const resultado = await firstValueFrom(
        this.msalService.acquireTokenSilent({
          scopes: [environment.azure.scope],
          account: cuenta
        })
      );
      this._accessTokenClaims.set(decodificarPayload(resultado.accessToken));
      this._rolesListos.set(true);
    } catch (error: unknown) {
      if (error instanceof InteractionRequiredAuthError) {
        try {
          await firstValueFrom(
            this.msalService.acquireTokenRedirect({
              scopes: [environment.azure.scope],
              account: cuenta
            })
          );
          return;
        } catch (redirectError: unknown) {
          this.registrarErrorRoles(redirectError);
          return;
        }
      }

      this.registrarErrorRoles(error);
    }
  }

  private registrarErrorRoles(error: unknown): void {
    this._errorRoles.set(
      `No se pudieron cargar los roles de la API: ${
        error instanceof Error ? error.message : 'error de autenticación desconocido'
      }`
    );
    this._rolesListos.set(true);
  }
}
