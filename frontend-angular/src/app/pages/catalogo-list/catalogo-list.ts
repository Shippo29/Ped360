import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CatalogoService } from '../../services/catalogo.service';
import { Producto } from '../../models/producto.model';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-catalogo-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './catalogo-list.html'
})
export class CatalogoListComponent implements OnInit {
  productos = signal<Producto[]>([]);
  cargando = signal(true);
  error = signal('');

  constructor(
    private catalogoService: CatalogoService,
    protected auth: AuthService
  ) {}

  ngOnInit(): void {
    this.catalogoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No se pudo cargar el catálogo: ' + (err.error?.mensaje ?? err.message));
        this.cargando.set(false);
      }
    });
  }
}
