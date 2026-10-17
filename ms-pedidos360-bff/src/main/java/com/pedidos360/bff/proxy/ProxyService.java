package com.pedidos360.bff.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lógica central del Back for Frontend: reenvía tal cual (mismo método,
 * mismo path+query, mismo body, mismo Authorization) la request que llegó
 * del navegador hacia el microservicio real correspondiente, y devuelve la
 * respuesta tal cual vino de allá.
 *
 * El BFF NO valida el JWT ni reimplementa reglas de rol: eso ya lo hacen
 * ms-pedidos360-orders y ms-pedidos360-catalog (son Resource Servers OAuth2
 * independientes). El BFF solo evita que el frontend tenga que conocer dos
 * hosts/puertos distintos y le da un único punto de entrada — igual que
 * tendrá en AWS, donde el frontend seguirá hablándole solo al BFF y el BFF
 * (o el API Gateway delante de él) es quien sabe cómo llegar a cada
 * microservicio.
 */
@Service
public class ProxyService {

    // Cabeceras "por conexión" que no se deben reenviar tal cual: el cliente
    // HTTP (RestClient) recalcula las suyas según el body/transporte real.
    private static final Set<String> CABECERAS_A_NO_REENVIAR = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "accept-encoding"
    );

    /**
     * Reenvía la request entrante tal cual al microservicio real. El path
     * (incluyendo "/api/orders/..." o "/api/catalog/...") se reenvía sin
     * modificar, porque los microservicios exponen exactamente esos mismos
     * paths — el BFF no reescribe rutas, solo cambia de host.
     */
    public ResponseEntity<byte[]> reenviar(RestClient cliente, HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String uri = path + (query != null && !query.isBlank() ? "?" + query : "");

        HttpMethod metodo = HttpMethod.valueOf(request.getMethod());
        byte[] body = leerBody(request);

        RestClient.RequestBodySpec spec = cliente.method(metodo).uri(uri);
        copiarCabeceras(request, spec);

        if (body.length > 0) {
            spec.body(body);
        }

        return spec.exchange((req, resp) -> {
            byte[] respBody = StreamUtils.copyToByteArray(resp.getBody());
            HttpHeaders headersRespuesta = new HttpHeaders();
            resp.getHeaders().forEach((nombre, valores) -> {
                if (!CABECERAS_A_NO_REENVIAR.contains(nombre.toLowerCase(Locale.ROOT))) {
                    headersRespuesta.addAll(nombre, valores);
                }
            });
            return ResponseEntity.status(resp.getStatusCode())
                    .headers(headersRespuesta)
                    .body(respBody);
        });
    }

    private byte[] leerBody(HttpServletRequest request) {
        try {
            return StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el body de la request entrante", e);
        }
    }

    private void copiarCabeceras(HttpServletRequest request, RestClient.RequestBodySpec spec) {
        Enumeration<String> nombres = request.getHeaderNames();
        if (nombres == null) {
            return;
        }
        while (nombres.hasMoreElements()) {
            String nombre = nombres.nextElement();
            if (CABECERAS_A_NO_REENVIAR.contains(nombre.toLowerCase(Locale.ROOT))) {
                continue;
            }
            List<String> valores = java.util.Collections.list(request.getHeaders(nombre));
            valores.forEach(valor -> spec.header(nombre, valor));
        }
    }
}
