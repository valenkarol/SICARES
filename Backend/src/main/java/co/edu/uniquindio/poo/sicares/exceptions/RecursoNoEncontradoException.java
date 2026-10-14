package co.edu.uniquindio.poo.sicares.exceptions;

/** Responde 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " con id " + id + " no existe");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
