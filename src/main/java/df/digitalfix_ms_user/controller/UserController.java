package df.digitalfix_ms_user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    // Requiere Scope User.Read o User.Manage
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SCOPE_User.Read') or hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<String> getMyProfile() {
        return ResponseEntity.ok("Perfil validado correctamente mediante Token JWT de Entra ID");
    }

    // Requiere obligatoriamente Scope User.Manage
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<String> createUser() {
        return ResponseEntity.ok("Usuario creado con permisos de gestión");
    }
}
