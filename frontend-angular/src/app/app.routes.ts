import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardComponent)
  },
  {
    path: 'pedidos',
    canActivate: [MsalGuard],
    loadComponent: () =>
      import('./pages/pedidos-list/pedidos-list').then((m) => m.PedidosListComponent)
  },
  {
    path: 'pedidos/nuevo',
    canActivate: [MsalGuard, roleGuard(['Cliente', 'Operador', 'Admin'])],
    loadComponent: () =>
      import('./pages/pedido-crear/pedido-crear').then((m) => m.PedidoCrearComponent)
  },
  {
    path: 'pedidos/:id',
    canActivate: [MsalGuard],
    loadComponent: () =>
      import('./pages/pedido-detalle/pedido-detalle').then((m) => m.PedidoDetalleComponent)
  },
  {
    path: 'catalogo',
    canActivate: [MsalGuard],
    loadComponent: () =>
      import('./pages/catalogo-list/catalogo-list').then((m) => m.CatalogoListComponent)
  },
  {
    path: 'catalogo/nuevo',
    canActivate: [MsalGuard, roleGuard(['Admin'])],
    loadComponent: () =>
      import('./pages/producto-form/producto-form').then((m) => m.ProductoFormComponent)
  },
  {
    path: 'catalogo/:id/editar',
    canActivate: [MsalGuard, roleGuard(['Admin'])],
    loadComponent: () =>
      import('./pages/producto-form/producto-form').then((m) => m.ProductoFormComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
