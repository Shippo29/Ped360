package com.pedidos360.rabbitadmin.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (mensaje.isBlank()) {
            mensaje = ex.getBindingResult().getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        return respuesta(mensaje);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> manejarRestriccion(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(violacion -> violacion.getPropertyPath() + ": " + violacion.getMessage())
                .collect(Collectors.joining("; "));
        return respuesta(mensaje);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, String>> manejarValidacionDeRuta(
            org.springframework.web.method.annotation.HandlerMethodValidationException ex) {
        return respuesta("Uno o más parámetros de ruta no son válidos");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> manejarSolicitudInvalida(Exception ex) {
        String mensaje = ex instanceof HttpMessageNotReadableException
                ? "El cuerpo de la solicitud no contiene JSON válido"
                : ex.getMessage();
        return respuesta(mensaje);
    }

    private ResponseEntity<Map<String, String>> respuesta(String mensaje) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("mensaje", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
