package df.digitalfix_ms_user.service;

import df.digitalfix_ms_user.dto.UserRequestDto;
import df.digitalfix_ms_user.dto.UserResponseDto;

import java.util.List;

public interface UserService {
    UserResponseDto createUser(UserRequestDto userRequestDto);
    UserResponseDto getUserById(Long id);
    UserResponseDto getUserByEntraId(String entraId);
    UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);
    void deleteUserLogical(Long id);
    List<UserResponseDto> getAllUsers();
}
