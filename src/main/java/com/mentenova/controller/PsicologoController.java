package com.mentenova.controller;

import com.mentenova.dto.PsicologoForm;
import com.mentenova.service.PsicologoService;
import com.mentenova.service.UsuarioSesionService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
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

@Controller
@RequestMapping("/psicologos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class PsicologoController {

    private final PsicologoService psicologos;
    private final UsuarioSesionService sesiones;

    public PsicologoController(
            PsicologoService psicologos,
            UsuarioSesionService sesiones) {

        this.psicologos = psicologos;
        this.sesiones = sesiones;
    }

    @GetMapping
    public String listar(
            Authentication authentication,
            Model model) {

        model.addAttribute(
            "usuarioSesion",
            sesiones.requerir(authentication)
        );
        model.addAttribute("psicologos", psicologos.listar());

        return "psicologos/listado";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevo(
            Authentication authentication,
            Model model) {

        model.addAttribute(
            "usuarioSesion",
            sesiones.requerir(authentication)
        );
        model.addAttribute("psicologoForm", new PsicologoForm());

        return "psicologos/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardar(
            @Valid @ModelAttribute("psicologoForm") PsicologoForm form,
            BindingResult errores,
            Authentication authentication,
            Model model,
            RedirectAttributes redirect) {

        model.addAttribute(
            "usuarioSesion",
            sesiones.requerir(authentication)
        );

        if (errores.hasErrors()) {
            form.setPassword(null);
            return "psicologos/formulario";
        }

        try {
            psicologos.registrar(form);

        } catch (IllegalArgumentException | IllegalStateException ex) {

            errores.reject("registro", ex.getMessage());
            form.setPassword(null);

            return "psicologos/formulario";

        } catch (DataIntegrityViolationException ex) {

            errores.reject(
                "duplicado",
                "No se pudo registrar. Verifica que el correo y la colegiatura sean únicos."
            );
            form.setPassword(null);

            return "psicologos/formulario";
        }

        redirect.addFlashAttribute(
            "exito",
            "Psicólogo registrado correctamente."
        );

        return "redirect:/psicologos";
    }
}