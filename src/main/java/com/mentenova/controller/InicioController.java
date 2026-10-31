package com.mentenova.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.mentenova.service.UsuarioSesionService;

@Controller
public class InicioController {
    @GetMapping("/inicio")
    public String inicio(Authentication authentication) {
        if (UsuarioSesionService.tieneRol(authentication, "PSICOLOGO")) return "redirect:/agenda";
        if (UsuarioSesionService.tieneRol(authentication, "PACIENTE")) return "redirect:/mis-citas";
        return "redirect:/dashboard";
    }
}
