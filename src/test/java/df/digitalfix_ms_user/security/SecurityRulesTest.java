package df.digitalfix_ms_user.security;

import df.digitalfix_ms_user.config.SecurityConfig;
import df.digitalfix_ms_user.controller.UserController;
import df.digitalfix_ms_user.dto.UserResponseDto;
import df.digitalfix_ms_user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Reglas de SecurityConfig de ms-user. jwt() entrega un token ya validado con las
// autoridades indicadas (el JwtDecoder real contra Entra ID se reemplaza por un mock).
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, EntraJwtAuthenticationConverter.class, JsonSecurityErrorHandler.class})
@TestPropertySource(properties = {"AZURE_TENANT_ID=test-tenant", "AZURE_CLIENT_ID=test-client"})
class SecurityRulesTest {

    private static final SimpleGrantedAuthority API_SCOPE = new SimpleGrantedAuthority("SCOPE_access_as_user");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private UserService userService;

    @Test
    void sinTokenResponde401EnJson() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void perfilPropioSoloPideElScopeDeLaApi() throws Exception {
        when(userService.getOrProvisionCurrentUser(any(), any(), any(), any())).thenReturn(new UserResponseDto());

        mockMvc.perform(get("/api/user/me").with(jwt().jwt(token -> token.claim("oid", "oid-1")).authorities(API_SCOPE)))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeGestionarUsuarios() throws Exception {
        mockMvc.perform(get("/api/user").with(jwt().authorities(API_SCOPE, new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void superadminGestionaUsuariosPorJerarquiaDeRoles() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/user").with(jwt().authorities(API_SCOPE, new SimpleGrantedAuthority("ROLE_SUPERADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminAsignaRoles() throws Exception {
        when(userService.assignRole(eq(5L), eq("AUDITOR"))).thenReturn(new UserResponseDto());

        mockMvc.perform(put("/api/user/5/rol")
                        .with(jwt().authorities(API_SCOPE, new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoRol\":\"AUDITOR\"}"))
                .andExpect(status().isOk());
    }
}
