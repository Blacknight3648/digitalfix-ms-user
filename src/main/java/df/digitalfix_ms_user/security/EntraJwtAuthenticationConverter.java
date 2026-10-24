package df.digitalfix_ms_user.security;

import df.digitalfix_ms_user.service.UserService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Mismas autoridades que ms-login (EntraJwtAuthenticationConverter):
//   - "scp" (scopes delegados)          -> SCOPE_<scope>
//   - "roles" (App Roles de Entra ID)   -> ROLE_<ROL>
//   - rol del usuario en USUARIOS/ROLES -> ROLE_<ROL>
// ms-user es dueño de esas tablas, así que aquí el rol se lee directo de la BD.
@Component
public class EntraJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    public EntraJwtAuthenticationConverter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        String scopes = jwt.getClaimAsString("scp");
        if (scopes != null) {
            for (String scope : scopes.split(" ")) {
                if (!scope.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
        }

        List<String> tokenRoles = jwt.getClaimAsStringList("roles");
        if (tokenRoles != null) {
            tokenRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        }

        String userEntraId = EntraTokenClaims.userEntraId(jwt);
        userService.findActiveRoleCode(userEntraId)
                .ifPresent(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));

        return new JwtAuthenticationToken(jwt, authorities, userEntraId);
    }
}
