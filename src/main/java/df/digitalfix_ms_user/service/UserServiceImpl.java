package df.digitalfix_ms_user.service;

import df.digitalfix_ms_user.dto.AreaDto;
import df.digitalfix_ms_user.dto.AreaRequestDto;
import df.digitalfix_ms_user.dto.CompanyResponseDto;
import df.digitalfix_ms_user.dto.RoleResponseDto;
import df.digitalfix_ms_user.dto.UserRequestDto;
import df.digitalfix_ms_user.dto.UserResponseDto;
import df.digitalfix_ms_user.exception.ConflictException;
import df.digitalfix_ms_user.exception.ResourceNotFoundException;
import df.digitalfix_ms_user.model.AreaEntity;
import df.digitalfix_ms_user.model.CompanyEntity;
import df.digitalfix_ms_user.model.RoleEntity;
import df.digitalfix_ms_user.model.UserEntity;
import df.digitalfix_ms_user.model.enums.EntityStatus;
import df.digitalfix_ms_user.repository.AreaRepository;
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
    private final AreaRepository areaRepository;

    public UserServiceImpl(UserRepository userRepository, CompanyRepository companyRepository,
                           RoleRepository roleRepository, AreaRepository areaRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.areaRepository = areaRepository;
    }

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto dto) {
        CompanyEntity empresa = companyRepository.findById(dto.getIdEmpresa())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + dto.getIdEmpresa()));

        RoleEntity rol = roleRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getIdRol()));
        String email = normalizeEmail(dto.getEmail());
        requireEmailAvailable(email, null);

        UserEntity user = new UserEntity();
        user.setUserEntraId(dto.getUserEntraId());
        user.setEmail(email);
        user.setNombre(normalizeText(dto.getNombre()));
        user.setApellido(normalizeText(dto.getApellido()));
        user.setTelefono(normalizePhone(dto.getTelefono()));
        user.setArea(resolveArea(dto.getIdArea()));
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

    // EMAIL es único en USUARIOS (también entre usuarios dados de baja): sin esta
    // verificación el insert fallaría y el handler genérico respondería 500.
    private void requireEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new ConflictException("Ya existe un usuario con el correo " + email
                            + (existing.getEstado() == EntityStatus.ACTIVO ? "" : " (dado de baja)"));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaDto> getActiveAreas() {
        return areaRepository.findByEstadoOrderByNombreAsc(EntityStatus.ACTIVO).stream()
                .map(a -> new AreaDto(a.getId(), a.getNombre()))
                .collect(Collectors.toList());
    }

    // Nombre con la primera letra en mayúscula; si ya existe (sin distinguir mayúsculas)
    // se rechaza en vez de crear un duplicado como "operaciones" junto a "Operaciones".
    @Override
    @Transactional
    public AreaDto createArea(AreaRequestDto dto) {
        String nombre = capitalizeFirst(normalizeText(dto.nombre()));
        areaRepository.findByNombreIgnoreCase(nombre).ifPresent(existing -> {
            throw new ConflictException("Ya existe el área " + existing.getNombre());
        });
        AreaEntity area = new AreaEntity();
        area.setNombre(nombre);
        area = areaRepository.save(area);
        return new AreaDto(area.getId(), area.getNombre());
    }

    private AreaEntity resolveArea(Long idArea) {
        if (idArea == null) {
            return null;
        }
        return areaRepository.findById(idArea)
                .filter(a -> a.getEstado() == EntityStatus.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada con ID: " + idArea));
    }

    // Sanitización en el borde del servicio: espacios repetidos fuera, correo en minúsculas
    // y teléfono como +56XXXXXXXXX (mismo formato que ms-workorders).
    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.replaceAll("[<>]", "").replaceAll("\\s+", " ").trim();
        return clean.isEmpty() ? null : clean;
    }

    private static String normalizeEmail(String value) {
        String clean = normalizeText(value);
        return clean == null ? null : clean.toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizePhone(String value) {
        String clean = normalizeText(value);
        if (clean == null) {
            return null;
        }
        String digits = clean.replaceAll("\\D", "");
        if (digits.length() == 11 && digits.startsWith("56")) {
            digits = digits.substring(2);
        }
        return digits.length() == 9 ? "+56" + digits : clean;
    }

    private static String capitalizeFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int first = value.codePointAt(0);
        return new StringBuilder().appendCodePoint(Character.toUpperCase(first))
                .append(value.substring(Character.charCount(first))).toString();
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
        String email = normalizeEmail(dto.getEmail());
        requireEmailAvailable(email, id);

        user.setEmail(email);
        user.setNombre(normalizeText(dto.getNombre()));
        user.setApellido(normalizeText(dto.getApellido()));
        user.setTelefono(normalizePhone(dto.getTelefono()));
        user.setArea(resolveArea(dto.getIdArea()));
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

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleResponseDto(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponseDto> getActiveCompanies() {
        return companyRepository.findAll().stream()
                .filter(c -> c.getEstado() == EntityStatus.ACTIVO)
                .map(c -> new CompanyResponseDto(c.getId(), c.getRazonSocial(), c.getRut(), c.getTipoEmpresa()))
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
        dto.setArea(entity.getArea() != null ? entity.getArea().getNombre() : null);
        dto.setIdArea(entity.getArea() != null ? entity.getArea().getId() : null);
        dto.setIdEmpresa(entity.getEmpresa().getId());
        dto.setIdRol(entity.getRol().getId());
        dto.setEstado(entity.getEstado().name());
        dto.setNombreEmpresa(entity.getEmpresa().getRazonSocial());
        dto.setNombreRol(entity.getRol().getNombre());
        dto.setCodigoRol(entity.getRol().getCodigo());
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setFechaActualizacion(entity.getFechaActualizacion());
        return dto;
    }
}
