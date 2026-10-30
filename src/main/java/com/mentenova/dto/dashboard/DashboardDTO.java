package com.mentenova.dto.dashboard;

import com.mentenova.dto.reporte.CitaReporteDTO;
import com.mentenova.dto.reporte.ResumenEstadosDTO;

import java.time.LocalDate;
import java.util.List;

public record DashboardDTO(
        String alcance,
        boolean vinculado,
        LocalDate hoy,
        ResumenEstadosDTO resumenHoy,
        List<CitaReporteDTO> proximasCitas,
        CalendarioDTO calendario,
        LocalDate desde,
        LocalDate hasta,
        ResumenEstadosDTO resumenPeriodo) {

    public static final String ALCANCE_GENERAL = "GENERAL";
    public static final String ALCANCE_PSICOLOGO = "PSICOLOGO";
    public static final String ALCANCE_PACIENTE = "PACIENTE";
}
