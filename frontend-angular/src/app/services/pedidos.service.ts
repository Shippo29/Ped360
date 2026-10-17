import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CrearPedidoRequest, EstadoPedido, Pedido } from '../models/pedido.model';

@Injectable({ providedIn: 'root' })
export class PedidosService {
  private readonly baseUrl = `${environment.ordersApiUrl}/orders`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.baseUrl);
  }

  obtener(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.baseUrl}/${id}`);
  }

  crear(request: CrearPedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.baseUrl, request);
  }

  cambiarEstado(id: number, nuevoEstado: EstadoPedido): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.baseUrl}/${id}/status`, { nuevoEstado });
  }
}
