package df.digitalfix_ms_user.dto;

// Empresa (PyME o la interna DigitalFix), para los selectores de la gestión de usuarios.
public record CompanyResponseDto(Long id, String razonSocial, String rut, String tipoEmpresa) {
}
