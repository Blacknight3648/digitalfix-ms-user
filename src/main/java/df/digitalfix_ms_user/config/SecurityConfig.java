package df.digitalfix_ms_user.config;

import df.digitalfix_ms_user.security.EntraJwtAuthenticationConverter;
import df.digitalfix_ms_user.security.JsonSecurityErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Set;

// Misma validación y mismo modelo de autorización que ms-login, para que un
// token aceptado por uno lo acepte el otro y los roles signifiquen lo mismo.
//
// Validación del JWT de Entra ID:
//   - Firma con las claves públicas (JWKS) del tenant.
//   - Vigencia ("exp", "nbf").
//   - Issuer del tenant, en su formato v2 o v1 (según la versión de token que
//     emita la App Registration de la API).
//   - Audience: el client ID de la API o api://<client ID>.
// Reglas por ruta:
//   - GET /api/user/me: cualquier usuario con el scope de la API.
//   - Resto de /api/user/**: solo ADMIN o SUPERADMIN (gestión de usuarios y roles).
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String API_SCOPE = "SCOPE_access_as_user";

    private final EntraJwtAuthenticationConverter jwtAuthenticationConverter;
    private final JsonSecurityErrorHandler jsonSecurityErrorHandler;

    @Value("${AZURE_TENANT_ID}")
    private String tenantId;

    @Value("${AZURE_CLIENT_ID}")
    private String clientId;

    public SecurityConfig(EntraJwtAuthenticationConverter jwtAuthenticationConverter,
                          JsonSecurityErrorHandler jsonSecurityErrorHandler) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.jsonSecurityErrorHandler = jsonSecurityErrorHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                /** SALUD Y SWAGGER */
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                /** PERFIL PROPIO – cualquier usuario autenticado con el scope de la API */
                .requestMatchers(HttpMethod.GET, "/api/user/me").hasAuthority(API_SCOPE)

                /** GESTIÓN DE USUARIOS Y ROLES – solo admin */
                .requestMatchers("/api/user/**").hasRole("ADMIN")

                /** RESTO */
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(jsonSecurityErrorHandler)
                .accessDeniedHandler(jsonSecurityErrorHandler))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jsonSecurityErrorHandler)
                .accessDeniedHandler(jsonSecurityErrorHandler));

        return http.build();
    }

    // SUPERADMIN hereda los permisos de todos los roles del caso (misma jerarquía
    // que ms-login): hasRole("ADMIN") también deja pasar a un SUPERADMIN.
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPERADMIN").implies("ADMIN", "SUPERVISOR", "TECNICO", "CLIENTE", "AUDITOR")
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerV2 = "https://login.microsoftonline.com/" + tenantId + "/v2.0";
        String issuerV1 = "https://sts.windows.net/" + tenantId + "/";

        // Las claves de firma son las mismas para tokens v1 y v2 del tenant.
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerV2);

        Set<String> allowedIssuers = Set.of(issuerV2, issuerV1);
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtClaimValidator<Object>(
                JwtClaimNames.ISS, iss -> iss != null && allowedIssuers.contains(iss.toString()));

        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Object>(
                JwtClaimNames.AUD, aud -> {
                    if (aud instanceof String s) {
                        return s.equals(clientId) || s.equals("api://" + clientId);
                    }
                    if (aud instanceof List<?> list) {
                        return list.contains(clientId) || list.contains("api://" + clientId);
                    }
                    return false;
                });

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), issuerValidator, audienceValidator));
        return jwtDecoder;
    }
}
