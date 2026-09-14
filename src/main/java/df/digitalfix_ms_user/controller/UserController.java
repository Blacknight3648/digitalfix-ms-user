package df.digitalfix_ms_user.controller;

import df.digitalfix_ms_user.dto.UserRequestDto;
import df.digitalfix_ms_user.dto.UserResponseDto;
import df.digitalfix_ms_user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint principal para que el usuario consultado por el Token (su propio perfil)
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SCOPE_User.Read') or hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<UserResponseDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String entraId = jwt.getSubject();
        return ResponseEntity.ok(userService.getUserByEntraId(entraId));
    }

    // Operaciones CRUD (Solo accesibles con User.Manage)
    
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto dto) {
        return new ResponseEntity<>(userService.createUser(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_User.Manage')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserLogical(id);
        return ResponseEntity.noContent().build();
    }
}
