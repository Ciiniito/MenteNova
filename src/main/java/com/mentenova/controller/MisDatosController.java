package com.mentenova.controller;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.paciente.DatosContactoDTO;
import com.mentenova.dto.paciente.PacienteDetalleDTO;
import com.mentenova.service.PacienteService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.service.ValidacionPacienteException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/mis-datos")
@PreAuthorize("hasRole('PACIENTE')")
public class MisDatosController {

    private final PacienteService pacienteService;
    private final UsuarioSesionService usuarioSesionService;

    public MisDatosController(PacienteService pacienteService, UsuarioSesionService usuarioSesionService) {
        this.pacienteService = pacienteService;
        this.usuarioSesionService = usuarioSesionService;
    }

    @ModelAttribute("usuarioSesion")
    public UsuarioSesionDTO usuarioSesion(Authentication authentication) {
        return usuarioSesionService.requerir(authentication);
    }

    @GetMapping
    public String ver(@ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario, Model model) {
        Optional<PacienteDetalleDTO> ficha = pacienteService.misDatos(usuario);
        model.addAttribute("ficha", ficha.orElse(null));
        model.addAttribute("contacto", ficha.map(DatosContactoDTO::de).orElseGet(DatosContactoDTO::new));
        return "pacientes/mis-datos";
    }

    @PostMapping
    public String actualizar(@Valid @ModelAttribute("contacto") DatosContactoDTO contacto,
                             BindingResult result,
                             @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                pacienteService.actualizarContacto(usuario, contacto);
                redirectAttributes.addFlashAttribute("exito", "Tus datos de contacto se actualizaron.");
                return "redirect:/mis-datos";
            } catch (ValidacionPacienteException e) {
                e.getErrores().forEach((campo, mensaje) -> result.rejectValue(campo, "invalido", mensaje));
            }
        }
        model.addAttribute("ficha", pacienteService.misDatos(usuario).orElse(null));
        return "pacientes/mis-datos";
    }
}
