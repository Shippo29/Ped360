export interface Producto {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  stock: number;
  activo: boolean;
}

export interface ProductoRequest {
  nombre: string;
  descripcion: string | null;
  precio: number;
  stock: number;
}
