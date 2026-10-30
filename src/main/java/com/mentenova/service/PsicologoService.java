package com.mentenova.service;

import com.mentenova.dto.PsicologoForm;
import com.mentenova.entity.Psicologo;
import com.mentenova.entity.Rol;
import com.mentenova.entity.Usuario;
import com.mentenova.repository.PsicologoRepository;
import com.mentenova.repository.RolRepository;
import com.mentenova.repository.UsuarioRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PsicologoService {

    private final PsicologoRepository psicologos;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    public PsicologoService(
            PsicologoRepository psicologos,
            UsuarioRepository usuarios,
            RolRepository roles,
            PasswordEncoder passwordEncoder,
            Clock reloj) {

        this.psicologos = psicologos;
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public List<Psicologo> listar() {
        return psicologos
                .findAllByOrderByUsuario_ApellidosAscUsuario_NombresAsc();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void registrar(PsicologoForm form) {

        String correo = form.getCorreo().toLowerCase(Locale.ROOT);

        if (form.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                "La contraseña es demasiado larga; utiliza una más corta."
            );
        }

        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException(
                "Ya existe un usuario registrado con ese correo."
            );
        }

        if (psicologos.existsByNumeroColegiatura(
                form.getNumeroColegiatura())) {

            throw new IllegalArgumentException(
                "Ya existe un psicólogo con esa colegiatura."
            );
        }

        Rol rol = roles.findByNombre("PSICOLOGO")
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .orElseThrow(() -> new IllegalStateException(
                    "El rol PSICOLOGO no está disponible."
                ));

        LocalDateTime ahora = LocalDateTime.now(reloj);

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombres(form.getNombres());
        usuario.setApellidos(form.getApellidos());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(
            passwordEncoder.encode(form.getPassword())
        );
        usuario.setTelefono(form.getTelefono());
        usuario.setActivo(true);
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);

        usuario = usuarios.save(usuario);

        Psicologo psicologo = new Psicologo();
        psicologo.setUsuario(usuario);
        psicologo.setNumeroColegiatura(
            form.getNumeroColegiatura()
        );
        psicologo.setTelefono(form.getTelefono());
        psicologo.setActivo(true);

        psicologos.saveAndFlush(psicologo);
    }
}