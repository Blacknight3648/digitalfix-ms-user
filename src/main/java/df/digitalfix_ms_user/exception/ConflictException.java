package df.digitalfix_ms_user.exception;

// El recurso ya existe (ej. un usuario con el mismo correo): se responde 409 en vez de 500.
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
