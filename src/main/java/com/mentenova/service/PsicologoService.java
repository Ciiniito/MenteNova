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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

    public PsicologoService(PsicologoRepository psicologos,
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
        return psicologos.findAllByOrderByUsuario_ApellidosAscUsuario_NombresAsc();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void registrar(PsicologoForm form) {
        String correo = form.getCorreo().toLowerCase(Locale.ROOT);

        validarPassword(form.getPassword(), true);
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con ese correo.");
        }
        if (psicologos.existsByNumeroColegiatura(form.getNumeroColegiatura())) {
            throw new IllegalArgumentException("Ya existe un psicólogo con esa colegiatura.");
        }

        Rol rol = roles.findByNombre("PSICOLOGO")
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .orElseThrow(() -> new IllegalStateException("El rol PSICOLOGO no está disponible."));

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombres(form.getNombres());
        usuario.setApellidos(form.getApellidos());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        usuario.setTelefono(form.getTelefono());
        usuario.setActivo(true);
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);
        usuario = usuarios.save(usuario);

        Psicologo psicologo = new Psicologo();
        psicologo.setUsuario(usuario);
        psicologo.setNumeroColegiatura(form.getNumeroColegiatura());
        psicologo.setTelefono(form.getTelefono());
        psicologo.setActivo(true);
        psicologos.saveAndFlush(psicologo);
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PsicologoForm obtenerFormulario(Long id) {
        Psicologo psicologo = requerir(id);
        Usuario usuario = psicologo.getUsuario();
        PsicologoForm form = new PsicologoForm();
        form.setNombres(usuario.getNombres());
        form.setApellidos(usuario.getApellidos());
        form.setCorreo(usuario.getCorreo());
        form.setNumeroColegiatura(psicologo.getNumeroColegiatura());
        form.setTelefono(psicologo.getTelefono());
        form.setActivo(Boolean.TRUE.equals(psicologo.getActivo())
                && Boolean.TRUE.equals(usuario.getActivo()));
        return form;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void actualizar(Long id, PsicologoForm form) {
        Psicologo psicologo = requerir(id);
        Usuario usuario = psicologo.getUsuario();
        String correo = form.getCorreo().toLowerCase(Locale.ROOT);
        validarPassword(form.getPassword(), false);
        if (form.getActivo() == null) {
            throw new IllegalArgumentException("Selecciona el estado de la cuenta.");
        }
        if (usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, usuario.getId())) {
            throw new IllegalArgumentException("Ya existe otro usuario registrado con ese correo.");
        }
        if (psicologos.existsByNumeroColegiaturaAndIdNot(form.getNumeroColegiatura(), id)) {
            throw new IllegalArgumentException("Ya existe otro psicólogo con esa colegiatura.");
        }
        usuario.setNombres(form.getNombres());
        usuario.setApellidos(form.getApellidos());
        usuario.setCorreo(correo);
        usuario.setTelefono(form.getTelefono());
        usuario.setActivo(form.getActivo());
        usuario.setFechaActualizacion(LocalDateTime.now(reloj));
        if (form.getPassword() != null) {
            usuario.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
        psicologo.setNumeroColegiatura(form.getNumeroColegiatura());
        psicologo.setTelefono(form.getTelefono());
        psicologo.setActivo(form.getActivo());
        // Ambas entidades están administradas por JPA en esta transacción.
        // El flush permite comunicar los errores de integridad al controlador.
        usuarios.flush();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Psicologo obtenerParaEliminar(Long id) {
        return requerir(id);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(Long id) {
        Psicologo psicologo = requerir(id);
        Usuario usuario = psicologo.getUsuario();
        psicologos.delete(psicologo);
        psicologos.flush();
        usuarios.delete(usuario);
        usuarios.flush();
        // Si una clave foránea impide cualquiera de los borrados,
        // @Transactional revierte la operación completa.
    }

    private Psicologo requerir(Long id) {
        return psicologos.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El psicólogo no existe."));
    }

    private static void validarPassword(String password, boolean obligatoria) {
        if (password == null) {
            if (obligatoria) throw new IllegalArgumentException("La contraseña es obligatoria.");
            return;
        }
        if (password.isBlank() || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y no puede contener solo espacios.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña es demasiado larga; utiliza una más corta.");
        }
    }
}
