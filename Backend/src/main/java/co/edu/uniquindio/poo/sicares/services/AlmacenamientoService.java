package co.edu.uniquindio.poo.sicares.services;

import co.edu.uniquindio.poo.sicares.exceptions.RecursoNoEncontradoException;
import co.edu.uniquindio.poo.sicares.exceptions.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Guarda los documentos de postulación en disco (carpeta configurable sicares.almacenamiento.ruta-documentos). */
@Service
public class AlmacenamientoService {

    private final Path raiz;

    public AlmacenamientoService(@Value("${sicares.almacenamiento.ruta-documentos}") String ruta) {
        this.raiz = Path.of(ruta).toAbsolutePath().normalize();
    }

    /** Devuelve la ruta relativa donde quedó guardado el archivo. */
    public String guardar(MultipartFile archivo, String subcarpeta) {
        try {
            Path carpeta = raiz.resolve(subcarpeta).normalize();
            Files.createDirectories(carpeta);
            String original = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
            String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String nombre = UUID.randomUUID() + extension.toLowerCase();
            Path destino = carpeta.resolve(nombre).normalize();
            if (!destino.startsWith(raiz)) {
                throw new ReglaNegocioException("Ruta de archivo inválida");
            }
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return raiz.relativize(destino).toString();
        } catch (IOException e) {
            throw new ReglaNegocioException("No fue posible guardar el archivo");
        }
    }

    public Resource cargar(String rutaRelativa) {
        try {
            Path archivo = raiz.resolve(rutaRelativa).normalize();
            Resource recurso = new UrlResource(archivo.toUri());
            if (!archivo.startsWith(raiz) || !recurso.exists()) {
                throw new RecursoNoEncontradoException("Archivo no encontrado");
            }
            return recurso;
        } catch (MalformedURLException e) {
            throw new RecursoNoEncontradoException("Archivo no encontrado");
        }
    }

    public void eliminar(String rutaRelativa) {
        try {
            Files.deleteIfExists(raiz.resolve(rutaRelativa).normalize());
        } catch (IOException ignored) {
            // si no se puede borrar, no detiene la operación
        }
    }
}
