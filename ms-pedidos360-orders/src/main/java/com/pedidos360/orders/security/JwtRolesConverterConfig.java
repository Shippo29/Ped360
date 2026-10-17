package com.pedidos360.orders.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Configura cómo se extraen las authorities (roles) desde el Access Token
 * JWT emitido por Microsoft Entra ID.
 *
 * Entra ID entrega los App Roles asignados al usuario/grupo en el claim
 * "roles" (array de strings, p.ej. ["Admin"] o ["Operador"]). Spring
 * Security espera authorities con el prefijo "ROLE_", por eso se configura
 * explícitamente el nombre del claim y el prefijo.
 */
@Configuration
public class JwtRolesConverterConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
