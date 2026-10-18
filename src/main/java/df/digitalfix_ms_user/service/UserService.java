package df.digitalfix_ms_user.service;

import df.digitalfix_ms_user.dto.UserRequestDto;
import df.digitalfix_ms_user.dto.UserResponseDto;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserResponseDto createUser(UserRequestDto userRequestDto);
    UserResponseDto getUserById(Long id);
    UserResponseDto getUserByEntraId(String entraId);
    // Perfil del usuario del token; si es su primer login lo vincula o lo da de alta.
    UserResponseDto getOrProvisionCurrentUser(String userEntraId, String email, String nombre, String apellido);
    // Código del rol (ADMIN, SUPERVISOR, ...) del usuario activo con ese OID.
    Optional<String> findActiveRoleCode(String userEntraId);
    UserResponseDto assignRole(Long id, String codigoRol);
    UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);
    void deleteUserLogical(Long id);
    List<UserResponseDto> getAllUsers();
}
