package com.mentenova.service;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.repository.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioSesionService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioSesionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<UsuarioSesionDTO> obtener(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarioRepository.findByCorreo(authentication.getName())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .map(UsuarioSesionDTO::de);
    }

    public UsuarioSesionDTO requerir(Authentication authentication) {
        return obtener(authentication).orElseThrow(SesionInvalidaException::new);
    }

    public static boolean tieneRol(Authentication authentication, String rol) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
