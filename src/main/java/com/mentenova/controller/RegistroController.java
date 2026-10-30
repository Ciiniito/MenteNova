package com.mentenova.controller;

import com.mentenova.dto.RegistroDTO;
import com.mentenova.entity.Rol;
import com.mentenova.entity.Usuario;
import com.mentenova.repository.RolRepository;
import com.mentenova.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/registro")
public class RegistroController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroController(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroDTO", new RegistroDTO());
        return "registro";
    }

    @PostMapping
    public String procesarRegistro(@Valid @ModelAttribute("registroDTO") RegistroDTO dto,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "registro";
        }

        // Validar que el correo no exista
        if (usuarioRepository.findByCorreo(dto.getCorreo()).isPresent()) {
            result.rejectValue("correo", "duplicate",
                    "Ya existe un usuario registrado con este correo.");
            return "registro";
        }

        // Buscar el rol PACIENTE (rol por defecto para nuevos registros)
        Rol rolPaciente = rolRepository.findByNombre("PACIENTE")
                .orElseThrow(() -> new IllegalStateException(
                        "El rol PACIENTE no existe en la base de datos"));

        // Crear el usuario
        Usuario usuario = new Usuario();
        usuario.setRol(rolPaciente);
        usuario.setNombres(dto.getNombres());
        usuario.setApellidos(dto.getApellidos());
        usuario.setCorreo(dto.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(dto.getTelefono());
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setFechaActualizacion(LocalDateTime.now());

        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("exito",
                "Cuenta creada exitosamente. Ya puede iniciar sesión.");
        return "redirect:/login";
    }
}