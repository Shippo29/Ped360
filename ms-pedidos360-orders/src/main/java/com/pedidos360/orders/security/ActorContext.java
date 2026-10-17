package com.pedidos360.orders.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ActorContext {

    /**
     * Identificador estable del usuario autenticado (claim "oid" de Entra ID;
     * si no viene, cae a "sub"). Se usa para saber de quién es cada pedido.
     */
    public String obtenerIdActor() {
        Jwt jwt = obtenerJwt();
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    public boolean tieneRol(String rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authority = "ROLE_" + rol;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase(authority));
    }

    public boolean esAdmin() {
        return tieneRol("Admin");
    }

    public boolean esCliente() {
        return tieneRol("Cliente");
    }

    public boolean esOperador() {
        return tieneRol("Operador");
    }

    private Jwt obtenerJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("El principal autenticado no es un JWT válido");
    }
}
