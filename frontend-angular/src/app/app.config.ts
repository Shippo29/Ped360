import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalService,
  MsalGuard,
  MsalBroadcastService,
  MsalInterceptor,
  MsalGuardConfiguration,
  MsalInterceptorConfiguration
} from '@azure/msal-angular';
import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation
} from '@azure/msal-browser';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { environment } from '../environments/environment';
import { routes } from './app.routes';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azure.clientId,
      authority: `https://login.microsoftonline.com/${environment.azure.tenantId}`,
      redirectUri: environment.azure.redirectUri,
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage
    }
  });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: [environment.azure.scope]
    }
  };
}

/**
 * Mapea qué URLs necesitan token y con qué scope. Ambos microservicios
 * (orders y catalog) comparten el mismo App Registration/scope de backend,
 * así que un solo access token sirve para llamar a los dos.
 */
export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string> | null>();
  protectedResourceMap.set(`${environment.ordersApiUrl}/*`, [environment.azure.scope]);
  protectedResourceMap.set(`${environment.catalogApiUrl}/*`, [environment.azure.scope]);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}

/**
 * MSAL necesita inicializarse y procesar la respuesta de redirección de
 * Microsoft ANTES de que la app renderice cualquier componente; si no, el
 * login por redirect deja la cuenta "a medio setear". Antes esto vivía en
 * App.ngOnInit — se movió acá para que corra siempre, sin depender de qué
 * componente se monte primero.
 */
export function initializeMsalFactory(msalService: MsalService): () => Promise<void> {
  return () =>
    msalService.instance.initialize().then(() =>
      msalService.instance.handleRedirectPromise().then((resultado) => {
        if (resultado?.account) {
          msalService.instance.setActiveAccount(resultado.account);
        }
      })
    );
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: MSALGuardConfigFactory
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: MSALInterceptorConfigFactory
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true
    },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeMsalFactory,
      deps: [MsalService],
      multi: true
    }
  ]
};
