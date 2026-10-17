import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CatalogoService } from '../../services/catalogo.service';
import { PedidosService } from '../../services/pedidos.service';
import { Producto } from '../../models/producto.model';
import { ItemPedidoRequest } from '../../models/pedido.model';

interface LineaCarrito extends ItemPedidoRequest {
  nombreProducto: string;
  precioUnitario: number;
}

@Component({
  selector: 'app-pedido-crear',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pedido-crear.html'
})
export class PedidoCrearComponent implements OnInit {
  productos = signal<Producto[]>([]);
  carrito = signal<LineaCarrito[]>([]);
  cargando = signal(true);
  enviando = signal(false);
  error = signal('');

  productoSeleccionadoId = signal<number | null>(null);
  cantidadSeleccionada = signal(1);

  total = computed(() =>
    this.carrito().reduce((acc, l) => acc + l.precioUnitario * l.cantidad, 0)
  );

  constructor(
    private catalogoService: CatalogoService,
    private pedidosService: PedidosService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.catalogoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos.filter((p) => p.activo && p.stock > 0));
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No se pudo cargar el catálogo: ' + (err.error?.mensaje ?? err.message));
        this.cargando.set(false);
      }
    });
  }

  agregarAlCarrito(): void {
    const productoId = this.productoSeleccionadoId();
    const cantidad = this.cantidadSeleccionada();
    if (!productoId || cantidad < 1) {
      return;
    }

    const producto = this.productos().find((p) => p.id === productoId);
    if (!producto) {
      return;
    }

    const existente = this.carrito().find((l) => l.productoId === productoId);
    if (existente) {
      existente.cantidad += cantidad;
      this.carrito.set([...this.carrito()]);
    } else {
      this.carrito.set([
        ...this.carrito(),
        {
          productoId: producto.id,
          nombreProducto: producto.nombre,
          cantidad,
          precioUnitario: producto.precio
        }
      ]);
    }

    this.productoSeleccionadoId.set(null);
    this.cantidadSeleccionada.set(1);
  }

  quitarLinea(productoId: number): void {
    this.carrito.set(this.carrito().filter((l) => l.productoId !== productoId));
  }

  confirmarPedido(): void {
    if (this.carrito().length === 0) {
      this.error.set('Agrega al menos un producto antes de confirmar.');
      return;
    }

    this.enviando.set(true);
    this.error.set('');

    const request = {
      items: this.carrito().map((l) => ({ productoId: l.productoId, cantidad: l.cantidad }))
    };

    this.pedidosService.crear(request).subscribe({
      next: (pedido) => {
        this.router.navigate(['/pedidos', pedido.id]);
      },
      error: (err) => {
        this.error.set(err.error?.mensaje ?? 'No se pudo crear el pedido.');
        this.enviando.set(false);
      }
    });
  }
}
