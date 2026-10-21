package com.osinergmin.osinergmin.security;

import com.osinergmin.osinergmin.model.Rol;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Convierte el claim "rol" del token en autoridades: ROLE_&lt;rol&gt; más cada permiso del rol.
 * Los permisos se calculan al validar el token, así que un cambio en la matriz aplica de inmediato.
 */
public class RolAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String claim = jwt.getClaimAsString(JwtService.CLAIM_ROL);
        Rol rol;
        try {
            rol = Rol.valueOf(claim);
        } catch (IllegalArgumentException | NullPointerException e) {
            return List.of(); // rol desconocido: autenticado pero sin permisos
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.permisos().forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso.name())));
        return authorities;
    }
}
