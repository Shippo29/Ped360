import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PedidosService } from '../../services/pedidos.service';
import { Pedido } from '../../models/pedido.model';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-pedidos-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pedidos-list.html'
})
export class PedidosListComponent implements OnInit {
  pedidos = signal<Pedido[]>([]);
  cargando = signal(true);
  error = signal('');

  constructor(
    private pedidosService: PedidosService,
    protected auth: AuthService
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.pedidosService.listar().subscribe({
      next: (pedidos) => {
        this.pedidos.set(pedidos);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No se pudieron cargar los pedidos: ' + (err.error?.mensaje ?? err.message));
        this.cargando.set(false);
      }
    });
  }
}
