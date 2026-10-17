import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PedidosService } from '../../services/pedidos.service';
import { CatalogoService } from '../../services/catalogo.service';
import { Pedido, EstadoPedido } from '../../models/pedido.model';
import { Producto } from '../../models/producto.model';
import { AuthService } from '../../core/auth.service';

const ESTADOS_ACTIVOS: EstadoPedido[] = ['CREADO', 'ACEPTADO', 'EN_PREPARACION', 'DESPACHADO'];
const TODOS_LOS_ESTADOS: EstadoPedido[] = [
  'CREADO', 'ACEPTADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'CANCELADO'
];
const UMBRAL_STOCK_BAJO = 5;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {
  readonly todosLosEstados = TODOS_LOS_ESTADOS;

  cargando = signal(true);
  error = signal('');

  pedidos = signal<Pedido[]>([]);
  productos = signal<Producto[]>([]);

  totalPedidos = computed(() => this.pedidos().length);
  pedidosActivos = computed(() =>
    this.pedidos().filter((p) => ESTADOS_ACTIVOS.includes(p.estado)).length
  );
  contadoresPorEstado = computed(() => {
    const contadores: Partial<Record<EstadoPedido, number>> = {};
    for (const p of this.pedidos()) {
      contadores[p.estado] = (contadores[p.estado] ?? 0) + 1;
    }
    return contadores;
  });
  ultimosPedidos = computed(() =>
    [...this.pedidos()]
      .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
      .slice(0, 5)
  );

  totalProductos = computed(() => this.productos().length);
  productosConStockBajo = computed(() =>
    this.productos().filter((p) => p.activo && p.stock <= UMBRAL_STOCK_BAJO)
  );

  constructor(
    private pedidosService: PedidosService,
    private catalogoService: CatalogoService,
    protected auth: AuthService
  ) {}

  ngOnInit(): void {
    void this.inicializarDashboard();
  }

  private async inicializarDashboard(): Promise<void> {
    this.auth.inicializarCuentaActiva();

    if (!this.auth.estaAutenticado()) {
      this.cargando.set(false);
      return;
    }

    await this.auth.cargarRoles();
    if (!this.auth.rolesListos() || this.auth.errorRoles() || this.auth.roles().length === 0) {
      this.cargando.set(false);
      return;
    }

    this.pedidosService.listar().subscribe({
      next: (pedidos) => {
        this.pedidos.set(pedidos);
        this.cargarProductosSiCorresponde();
      },
      error: (err) => {
        this.error.set('No se pudo cargar el resumen de pedidos: ' + (err.error?.mensaje ?? err.message));
        this.cargarProductosSiCorresponde();
      }
    });
  }

  private cargarProductosSiCorresponde(): void {
    this.catalogoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }
}
