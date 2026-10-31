package com.mentenova.controller;

import com.mentenova.entity.Cita;
import com.mentenova.service.CitaService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.dto.UsuarioSesionDTO;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CitaController {

    private final CitaService citaService;
    private final UsuarioSesionService usuarioSesionService;

    public CitaController(
            CitaService citaService,
            UsuarioSesionService usuarioSesionService) {

        this.citaService = citaService;
        this.usuarioSesionService = usuarioSesionService;
    }

    @GetMapping("/citas")
    public String listarCitas(
            Authentication authentication,
            Model model) {

        UsuarioSesionDTO usuario =
                usuarioSesionService.requerir(authentication);

        model.addAttribute("usuarioSesion", usuario);
        model.addAttribute("activo", "citas");
        model.addAttribute("citas", citaService.listarTodas());

        return "citas/index";
    }

    @GetMapping("/citas/nueva")
    public String nuevaCita(
            Authentication authentication,
            Model model) {

        UsuarioSesionDTO usuario =
                usuarioSesionService.requerir(authentication);

        model.addAttribute("usuarioSesion", usuario);
        model.addAttribute("activo", "citas");
        model.addAttribute("cita", new Cita());

        return "citas/registrar";
    }
}