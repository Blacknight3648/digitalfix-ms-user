package df.digitalfix_ms_user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRequestDto {

    // OID de Entra ID. Opcional: si se omite, el usuario queda pre-asignado y se
    // vincula por correo en su primer login (UserServiceImpl.getOrProvisionCurrentUser).
    private String userEntraId;

    @NotBlank(message = "El email es requerido")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$", message = "Formato de email inválido (ej. nombre@empresa.cl)")
    @Size(max = 100, message = "El email no debe superar los 100 caracteres")
    private String email;

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 50, message = "El nombre no debe superar los 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es requerido")
    @Size(max = 50, message = "El apellido no debe superar los 50 caracteres")
    private String apellido;

    // Mismo formato que ms-workorders: +56 y 9 dígitos (móvil +569XXXXXXXX o fijo +56 2 XXXX XXXX).
    // UserServiceImpl lo normaliza antes de guardar; aquí se aceptan espacios y guiones.
    @Pattern(regexp = "^\\s*\\+?\\s*(56)?[\\s-]*\\d([\\s-]*\\d){8}\\s*$",
            message = "El teléfono debe ser chileno: +56 9 1234 5678")
    private String telefono;

    // Área del catálogo AREAS (opcional).
    private Long idArea;

    @NotNull(message = "El ID de la empresa es requerido")
    private Long idEmpresa;

    @NotNull(message = "El ID del rol es requerido")
    private Long idRol;

    // Getters and Setters

    public String getUserEntraId() {
        return userEntraId;
    }

    public void setUserEntraId(String userEntraId) {
        this.userEntraId = userEntraId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Long getIdArea() {
        return idArea;
    }

    public void setIdArea(Long idArea) {
        this.idArea = idArea;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdRol() {
        return idRol;
    }

    public void setIdRol(Long idRol) {
        this.idRol = idRol;
    }
}
