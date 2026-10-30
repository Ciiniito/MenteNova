package com.mentenova.controller;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.reporte.FiltroReporte;
import com.mentenova.dto.reporte.ReporteCitasDTO;
import com.mentenova.dto.reporte.ReporteInasistenciasDTO;
import com.mentenova.dto.reporte.ReporteIngresosDTO;
import com.mentenova.service.ReporteService;
import com.mentenova.service.UsuarioSesionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class ReporteApiController {

    private final ReporteService reporteService;

    public ReporteApiController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/citas")
    public ReporteCitasDTO citas(FiltroReporte filtro) {
        return reporteService.reporteCitas(filtro);
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ReporteIngresosDTO ingresos(FiltroReporte filtro) {
        return reporteService.reporteIngresos(filtro);
    }

    @GetMapping("/inasistencias")
    public ReporteInasistenciasDTO inasistencias(FiltroReporte filtro, Authentication authentication) {
        boolean vistaCompleta = UsuarioSesionService.tieneRol(authentication, UsuarioSesionDTO.ADMINISTRADOR);
        return reporteService.reporteInasistencias(filtro, vistaCompleta);
    }
}
