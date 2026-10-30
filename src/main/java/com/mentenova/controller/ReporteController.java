package com.mentenova.controller;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.reporte.FiltroReporte;
import com.mentenova.service.ReporteService;
import com.mentenova.service.UsuarioSesionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Clock;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/reportes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioSesionService usuarioSesionService;
    private final Clock reloj;

    public ReporteController(ReporteService reporteService, UsuarioSesionService usuarioSesionService, Clock reloj) {
        this.reporteService = reporteService;
        this.usuarioSesionService = usuarioSesionService;
        this.reloj = reloj;
    }

    @ModelAttribute("usuarioSesion")
    public UsuarioSesionDTO usuarioSesion(Authentication authentication) {
        return usuarioSesionService.requerir(authentication);
    }

    @ModelAttribute("generadoEn")
    public LocalDateTime generadoEn() {
        return LocalDateTime.now(reloj);
    }

    @GetMapping
    public String index() {
        return "reportes/index";
    }

    @GetMapping("/citas")
    public String citas(@ModelAttribute("filtro") FiltroReporte filtro, BindingResult bindingResult, Model model) {
        model.addAttribute("reporte", reporteService.reporteCitas(filtro));
        model.addAttribute("psicologos", reporteService.psicologos());
        model.addAttribute("especialidades", reporteService.especialidades());
        model.addAttribute("modalidades", reporteService.modalidades());
        model.addAttribute("estados", reporteService.estadosCita());
        model.addAttribute("limiteDetalle", ReporteService.LIMITE_DETALLE);
        return "reportes/citas";
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String ingresos(@ModelAttribute("filtro") FiltroReporte filtro, BindingResult bindingResult, Model model) {
        model.addAttribute("reporte", reporteService.reporteIngresos(filtro));
        model.addAttribute("psicologos", reporteService.psicologos());
        model.addAttribute("mediosPago", reporteService.mediosPago());
        model.addAttribute("estadosPago", reporteService.estadosPago());
        model.addAttribute("limiteDetalle", ReporteService.LIMITE_DETALLE);
        return "reportes/ingresos";
    }

    @GetMapping("/inasistencias")
    public String inasistencias(@ModelAttribute("filtro") FiltroReporte filtro, BindingResult bindingResult,
                                Authentication authentication, Model model) {
        boolean vistaCompleta = UsuarioSesionService.tieneRol(authentication, UsuarioSesionDTO.ADMINISTRADOR);
        model.addAttribute("reporte", reporteService.reporteInasistencias(filtro, vistaCompleta));
        model.addAttribute("vistaCompleta", vistaCompleta);
        model.addAttribute("psicologos", reporteService.psicologos());
        model.addAttribute("modalidades", reporteService.modalidades());
        model.addAttribute("agrupaciones", reporteService.agrupaciones());
        return "reportes/inasistencias";
    }
}
