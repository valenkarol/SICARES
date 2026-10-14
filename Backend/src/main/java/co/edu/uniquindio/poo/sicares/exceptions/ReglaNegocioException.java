package co.edu.uniquindio.poo.sicares.exceptions;

/** Se lanza cuando se viola una regla de negocio (responde 400 Bad Request). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
