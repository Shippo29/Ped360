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

@Service
public class ProxyService {

    private static final Set<String> CABECERAS_A_NO_REENVIAR = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "accept-encoding"
    );

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
