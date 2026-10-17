import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogoService } from '../../services/catalogo.service';
import { ProductoRequest } from '../../models/producto.model';

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './producto-form.html'
})
export class ProductoFormComponent implements OnInit {
  modoEdicion = signal(false);
  cargando = signal(false);
  guardando = signal(false);
  error = signal('');

  form = signal<ProductoRequest>({
    nombre: '',
    descripcion: '',
    precio: 0,
    stock: 0
  });

  private id: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private catalogoService: CatalogoService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.id = Number(idParam);
      this.modoEdicion.set(true);
      this.cargando.set(true);
      this.catalogoService.obtener(this.id).subscribe({
        next: (producto) => {
          this.form.set({
            nombre: producto.nombre,
            descripcion: producto.descripcion,
            precio: producto.precio,
            stock: producto.stock
          });
          this.cargando.set(false);
        },
        error: (err) => {
          this.error.set('No se pudo cargar el producto: ' + (err.error?.mensaje ?? err.message));
          this.cargando.set(false);
        }
      });
    }
  }

  actualizarCampo<K extends keyof ProductoRequest>(campo: K, valor: ProductoRequest[K]): void {
    this.form.set({ ...this.form(), [campo]: valor });
  }

  guardar(): void {
    this.guardando.set(true);
    this.error.set('');

    const peticion = this.modoEdicion() && this.id
      ? this.catalogoService.editar(this.id, this.form())
      : this.catalogoService.crear(this.form());

    peticion.subscribe({
      next: () => this.router.navigate(['/catalogo']),
      error: (err) => {
        this.error.set(err.error?.mensaje ?? 'No se pudo guardar el producto.');
        this.guardando.set(false);
      }
    });
  }
}
