package df.digitalfix_ms_user.service;

import df.digitalfix_ms_user.dto.UserResponseDto;
import df.digitalfix_ms_user.model.CompanyEntity;
import df.digitalfix_ms_user.model.RoleEntity;
import df.digitalfix_ms_user.model.UserEntity;
import df.digitalfix_ms_user.model.enums.EntityStatus;
import df.digitalfix_ms_user.repository.AreaRepository;
import df.digitalfix_ms_user.repository.CompanyRepository;
import df.digitalfix_ms_user.repository.RoleRepository;
import df.digitalfix_ms_user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProvisioningTest {

    private static final String OID = "11111111-2222-3333-4444-555555555555";
    private static final String EMAIL = "prueba@duocuc.cl";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final AreaRepository areaRepository = mock(AreaRepository.class);
    private final UserServiceImpl service = new UserServiceImpl(userRepository, companyRepository, roleRepository, areaRepository);

    @BeforeEach
    void setUp() {
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static RoleEntity role(String codigo) {
        RoleEntity role = new RoleEntity();
        role.setCodigo(codigo);
        role.setNombre(codigo);
        return role;
    }

    private static CompanyEntity company() {
        CompanyEntity company = new CompanyEntity();
        company.setRazonSocial("DigitalFix (interna)");
        company.setRut("DIGITALFIX");
        return company;
    }

    private static UserEntity user(String oid, String codigoRol, EntityStatus estado) {
        UserEntity user = new UserEntity();
        user.setUserEntraId(oid);
        user.setEmail(EMAIL);
        user.setNombre("Usuario");
        user.setApellido("Prueba");
        user.setEmpresa(company());
        user.setRol(role(codigoRol));
        user.setEstado(estado);
        return user;
    }

    @Test
    void usuarioExistenteSeDevuelveSinCrearOtro() {
        when(userRepository.findByUserEntraId(OID)).thenReturn(Optional.of(user(OID, "SUPERVISOR", EntityStatus.ACTIVO)));

        UserResponseDto dto = service.getOrProvisionCurrentUser(OID, EMAIL, "Usuario", "Prueba");

        assertThat(dto.getCodigoRol()).isEqualTo("SUPERVISOR");
        verify(userRepository, never()).save(any());
    }

    @Test
    void usuarioPreAsignadoPorCorreoSeVinculaYConservaSuRol() {
        when(userRepository.findByUserEntraId(OID)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(null, "ADMIN", EntityStatus.ACTIVO)));

        UserResponseDto dto = service.getOrProvisionCurrentUser(OID, EMAIL, "Usuario", "Prueba");

        assertThat(dto.getUserEntraId()).isEqualTo(OID);
        assertThat(dto.getCodigoRol()).isEqualTo("ADMIN");
    }

    @Test
    void primerLoginSinRegistroCreaUsuarioConRolCliente() {
        when(userRepository.findByUserEntraId(OID)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(companyRepository.findByRut("DIGITALFIX")).thenReturn(Optional.of(company()));
        when(roleRepository.findByCodigo("CLIENTE")).thenReturn(Optional.of(role("CLIENTE")));

        UserResponseDto dto = service.getOrProvisionCurrentUser(OID, EMAIL, "Usuario", null);

        assertThat(dto.getUserEntraId()).isEqualTo(OID);
        assertThat(dto.getCodigoRol()).isEqualTo("CLIENTE");
        assertThat(dto.getApellido()).isEqualTo("-");
    }

    @Test
    void usuarioDadoDeBajaNoSeReactivaAlIniciarSesion() {
        when(userRepository.findByUserEntraId(OID)).thenReturn(Optional.of(user(OID, "CLIENTE", EntityStatus.INACTIVO)));

        assertThatThrownBy(() -> service.getOrProvisionCurrentUser(OID, EMAIL, "Usuario", "Prueba"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void correoVinculadoAOtraCuentaSeRechaza() {
        when(userRepository.findByUserEntraId(OID)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user("otro-oid", "CLIENTE", EntityStatus.ACTIVO)));

        assertThatThrownBy(() -> service.getOrProvisionCurrentUser(OID, EMAIL, "Usuario", "Prueba"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
