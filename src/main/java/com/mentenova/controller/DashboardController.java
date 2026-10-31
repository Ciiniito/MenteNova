package com.mentenova.controller;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.service.DashboardService;
import com.mentenova.service.UsuarioSesionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UsuarioSesionService usuarioSesionService;

    public DashboardController(DashboardService dashboardService, UsuarioSesionService usuarioSesionService) {
        this.dashboardService = dashboardService;
        this.usuarioSesionService = usuarioSesionService;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboard(@RequestParam(value = "desde", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(value = "hasta", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            @RequestParam(value = "mes", required = false)
                            @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                            Authentication authentication,
                            Model model) {
        UsuarioSesionDTO usuario = usuarioSesionService.requerir(authentication);
        model.addAttribute("usuarioSesion", usuario);
        model.addAttribute("dashboard", dashboardService.construir(usuario, desde, hasta, mes));
        return "dashboard";
    }

    @PreAuthorize("hasRole('PACIENTE')")
    @GetMapping(value = "/mis-citas", produces = MediaType.TEXT_HTML_VALUE)
    public String misCitas(Authentication authentication, Model model) {
        UsuarioSesionDTO usuario = usuarioSesionService.requerir(authentication);
        model.addAttribute("usuarioSesion", usuario);
        // El paciente se resuelve desde la sesión; no acepta identificadores del cliente.
        model.addAttribute("dashboard", dashboardService.construir(usuario, null, null, null));
        return "dashboard";
    }
}
