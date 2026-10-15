package co.edu.uniquindio.poo.sicares.security;

import co.edu.uniquindio.poo.sicares.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(UsuarioPrincipal::de)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
