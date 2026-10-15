package co.edu.uniquindio.poo.sicares.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

/** Genera y valida el token JWT de sesión (HU-02). */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${sicares.jwt.secret}") String secretoBase64,
                      @Value("${sicares.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.expiracionMs = expiracionMinutos * 60_000;
    }

    public String generarToken(UsuarioPrincipal usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.correo())
                .claim("id", usuario.id())
                .claim("rol", usuario.rol())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /** Devuelve el correo (subject) si el token es válido y no ha expirado. */
    public Optional<String> extraerCorreoSiValido(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
