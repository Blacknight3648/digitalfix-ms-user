package df.digitalfix_ms_user.service;

import df.digitalfix_ms_user.dto.UserRequestDto;
import df.digitalfix_ms_user.dto.UserResponseDto;
import df.digitalfix_ms_user.exception.ResourceNotFoundException;
import df.digitalfix_ms_user.model.CompanyEntity;
import df.digitalfix_ms_user.model.RoleEntity;
import df.digitalfix_ms_user.model.UserEntity;
import df.digitalfix_ms_user.model.enums.EntityStatus;
import df.digitalfix_ms_user.repository.CompanyRepository;
import df.digitalfix_ms_user.repository.RoleRepository;
import df.digitalfix_ms_user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;

    public UserServiceImpl(UserRepository userRepository, CompanyRepository companyRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto dto) {
        CompanyEntity empresa = companyRepository.findById(dto.getIdEmpresa())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + dto.getIdEmpresa()));

        RoleEntity rol = roleRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getIdRol()));

        UserEntity user = new UserEntity();
        user.setUserEntraId(dto.getUserEntraId());
        user.setEmail(dto.getEmail());
        user.setNombre(dto.getNombre());
        user.setApellido(dto.getApellido());
        user.setTelefono(dto.getTelefono());
        user.setArea(dto.getArea());
        user.setEmpresa(empresa);
        user.setRol(rol);
        user.setEstado(EntityStatus.ACTIVO);

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        UserEntity user = userRepository.findByIdAndEstado(id, EntityStatus.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return mapToResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEntraId(String entraId) {
        UserEntity user = userRepository.findByUserEntraIdAndEstado(entraId, EntityStatus.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con Entra ID: " + entraId));
        return mapToResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        UserEntity user = userRepository.findByIdAndEstado(id, EntityStatus.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        CompanyEntity empresa = companyRepository.findById(dto.getIdEmpresa())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + dto.getIdEmpresa()));

        RoleEntity rol = roleRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getIdRol()));

        user.setEmail(dto.getEmail());
        user.setNombre(dto.getNombre());
        user.setApellido(dto.getApellido());
        user.setTelefono(dto.getTelefono());
        user.setArea(dto.getArea());
        user.setEmpresa(empresa);
        user.setRol(rol);

        user = userRepository.save(user);
        return mapToResponseDto(user);
    }

    @Override
    @Transactional
    public void deleteUserLogical(Long id) {
        UserEntity user = userRepository.findByIdAndEstado(id, EntityStatus.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        
        user.setEstado(EntityStatus.INACTIVO);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getEstado() == EntityStatus.ACTIVO)
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private UserResponseDto mapToResponseDto(UserEntity entity) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(entity.getId());
        dto.setUserEntraId(entity.getUserEntraId());
        dto.setEmail(entity.getEmail());
        dto.setNombre(entity.getNombre());
        dto.setApellido(entity.getApellido());
        dto.setTelefono(entity.getTelefono());
        dto.setArea(entity.getArea());
        dto.setEstado(entity.getEstado().name());
        dto.setNombreEmpresa(entity.getEmpresa().getRazonSocial());
        dto.setNombreRol(entity.getRol().getNombre());
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setFechaActualizacion(entity.getFechaActualizacion());
        return dto;
    }
}
