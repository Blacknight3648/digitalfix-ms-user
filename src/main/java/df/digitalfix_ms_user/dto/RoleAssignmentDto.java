package df.digitalfix_ms_user.dto;

import jakarta.validation.constraints.NotBlank;

public class RoleAssignmentDto {

    @NotBlank(message = "El código del rol es requerido")
    private String codigoRol;

    public String getCodigoRol() {
        return codigoRol;
    }

    public void setCodigoRol(String codigoRol) {
        this.codigoRol = codigoRol;
    }
}
