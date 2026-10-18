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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    // Sembrados por la migración V2__catalogo_roles_y_empresa_interna.sql
    private static final String DEFAULT_ROLE_CODE = "CLIENTE";
    private static final String INTERNAL_COMPANY_RUT = "DIGITALFIX";

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

    // Alta en el primer login (JIT provisioning), en este orden:
    //   1. Ya existe un usuario con ese OID -> se devuelve (si está activo).
    //   2. Existe un usuario pre-asignado con ese correo y sin OID (creado por un
    //      ADMIN o por una migración Flyway) -> se le vincula el OID y conserva su rol.
    //   3. No existe -> se crea con el rol de menor privilegio (CLIENTE) en la
    //      empresa interna; un ADMIN luego le asigna su rol y su PyME.
    @Override
    @Transactional
    public UserResponseDto getOrProvisionCurrentUser(String userEntraId, String email, String nombre, String apellido) {
        Optional<UserEntity> byEntraId = userRepository.findByUserEntraId(userEntraId);
        if (byEntraId.isPresent()) {
            return mapToResponseDto(requireActive(byEntraId.get()));
        }

        if (email == null) {
            throw new AccessDeniedException("El token no incluye el correo del usuario; no se puede registrar.");
        }

        Optional<UserEntity> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            UserEntity preassigned = requireActive(byEmail.get());
            if (preassigned.getUserEntraId() != null) {
                throw new AccessDeniedException("El correo " + email + " ya está vinculado a otra cuenta de Entra ID.");
            }
            preassigned.setUserEntraId(userEntraId);
            return mapToResponseDto(userRepository.save(preassigned));
        }

        UserEntity user = new UserEntity();
        user.setUserEntraId(userEntraId);
        user.setEmail(email);
        user.setNombre(truncate(nombre != null ? nombre : email.substring(0, email.indexOf('@')), 50));
        user.setApellido(truncate(apellido != null ? apellido : "-", 50));
        user.setEmpresa(companyRepository.findByRut(INTERNAL_COMPANY_RUT)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa interna no encontrada (migración V2)")));
        user.setRol(roleRepository.findByCodigo(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Rol " + DEFAULT_ROLE_CODE + " no encontrado")));
        user.setEstado(EntityStatus.ACTIVO);
        return mapToResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findActiveRoleCode(String userEntraId) {
        return userRepository.findByUserEntraIdAndEstado(userEntraId, EntityStatus.ACTIVO)
                .map(user -> user.getRol().getCodigo());
    }

    @Override
    @Transactional
    public UserResponseDto assignRole(Long id, String codigoRol) {
        UserEntity user = userRepository.findByIdAndEstado(id, EntityStatus.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        RoleEntity rol = roleRepository.findByCodigo(codigoRol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con código: " + codigoRol));
        user.setRol(rol);
        return mapToResponseDto(userRepository.save(user));
    }

    private UserEntity requireActive(UserEntity user) {
        if (user.getEstado() != EntityStatus.ACTIVO) {
            throw new AccessDeniedException("El usuario " + user.getEmail() + " está dado de baja.");
        }
        return user;
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
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
        dto.setCodigoRol(entity.getRol().getCodigo());
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setFechaActualizacion(entity.getFechaActualizacion());
        return dto;
    }
}
