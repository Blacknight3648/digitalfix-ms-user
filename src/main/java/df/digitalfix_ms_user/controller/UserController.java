package df.digitalfix_ms_user.controller;

import df.digitalfix_ms_user.dto.AreaDto;
import df.digitalfix_ms_user.dto.AreaRequestDto;
import df.digitalfix_ms_user.dto.CompanyResponseDto;
import df.digitalfix_ms_user.dto.RoleAssignmentDto;
import df.digitalfix_ms_user.dto.RoleResponseDto;
import df.digitalfix_ms_user.dto.UserRequestDto;
import df.digitalfix_ms_user.dto.UserResponseDto;
import df.digitalfix_ms_user.security.EntraTokenClaims;
import df.digitalfix_ms_user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Las reglas de acceso están centralizadas en SecurityConfig (igual que en
// ms-login): /me exige el scope de la API y el resto de las rutas, rol ADMIN.
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Perfil del usuario del token, identificado por su OID (claim "oid", el mismo
    // que usa ms-login). En su primer login se vincula o se da de alta; ms-login
    // lo consulta reenviando el JWT del usuario para resolver su rol.
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getOrProvisionCurrentUser(
                EntraTokenClaims.userEntraId(jwt),
                EntraTokenClaims.email(jwt),
                EntraTokenClaims.givenName(jwt),
                EntraTokenClaims.familyName(jwt)));
    }

    // Catálogos para los formularios de gestión de usuarios (solo ADMIN). Se
    // declaran antes que /{id}: Spring prioriza la ruta literal sobre la variable.

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDto>> getRoles() {
        return ResponseEntity.ok(userService.getAllRoles());
    }

    @GetMapping("/empresas")
    public ResponseEntity<List<CompanyResponseDto>> getCompanies() {
        return ResponseEntity.ok(userService.getActiveCompanies());
    }

    @GetMapping("/areas")
    public ResponseEntity<List<AreaDto>> getAreas() {
        return ResponseEntity.ok(userService.getActiveAreas());
    }

    @PostMapping("/areas")
    public ResponseEntity<AreaDto> createArea(@Valid @RequestBody AreaRequestDto dto) {
        return new ResponseEntity<>(userService.createArea(dto), HttpStatus.CREATED);
    }

    // Operaciones CRUD (solo ADMIN)

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        return new ResponseEntity<>(userService.createUser(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    // Asigna un rol del catálogo (ADMIN, SUPERVISOR, TECNICO, CLIENTE, AUDITOR).
    // ms-login lo refleja en un máximo de 5 minutos (caché de roles por usuario).
    @PutMapping("/{id}/rol")
    public ResponseEntity<UserResponseDto> assignRole(@PathVariable Long id, @Valid @RequestBody RoleAssignmentDto dto) {
        return ResponseEntity.ok(userService.assignRole(id, dto.getCodigoRol()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserLogical(id);
        return ResponseEntity.noContent().build();
    }
}
