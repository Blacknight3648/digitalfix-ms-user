package df.digitalfix_ms_user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Alta de un área nueva desde la gestión de usuarios.
public record AreaRequestDto(
        @NotBlank(message = "El nombre del área es obligatorio")
        @Size(max = 80, message = "El nombre del área no debe superar los 80 caracteres")
        @Pattern(regexp = "^\\s*[\\p{L}\\p{N}][\\p{L}\\p{N} .,&/-]*$", message = "El nombre del área contiene caracteres no permitidos")
        String nombre
) {
}
