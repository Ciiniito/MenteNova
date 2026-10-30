package com.mentenova.dto.reporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CitaReporteDTO(
        Long idCita,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String paciente,
        String psicologo,
        String modalidad,
        String estadoCodigo,
        String estadoNombre,
        LocalDateTime fechaRegistro) {
}
