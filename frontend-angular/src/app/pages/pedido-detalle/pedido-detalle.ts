import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PedidosService } from '../../services/pedidos.service';
import { EstadoPedido, Pedido, TRANSICIONES_VALIDAS } from '../../models/pedido.model';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-pedido-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pedido-detalle.html'
})
export class PedidoDetalleComponent implements OnInit {
  pedido = signal<Pedido | null>(null);
  cargando = signal(true);
  error = signal('');
  actualizando = signal(false);

  /** Solo Operador/Admin pueden avanzar el estado (regla del backend). */
  puedeCambiarEstado = computed(() => this.auth.puedeGestionarPedidos());

  siguientesEstados = computed<EstadoPedido[]>(() => {
    const actual = this.pedido()?.estado;
    return actual ? TRANSICIONES_VALIDAS[actual] : [];
  });

  private id!: number;

  constructor(
    private route: ActivatedRoute,
    private pedidosService: PedidosService,
    protected auth: AuthService
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.pedidosService.obtener(this.id).subscribe({
      next: (pedido) => {
        this.pedido.set(pedido);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No se pudo cargar el pedido: ' + (err.error?.mensaje ?? err.message));
        this.cargando.set(false);
      }
    });
  }

  cambiarEstado(nuevoEstado: EstadoPedido): void {
    this.actualizando.set(true);
    this.error.set('');
    this.pedidosService.cambiarEstado(this.id, nuevoEstado).subscribe({
      next: (pedido) => {
        this.pedido.set(pedido);
        this.actualizando.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.mensaje ?? 'No se pudo cambiar el estado.');
        this.actualizando.set(false);
      }
    });
  }
}
