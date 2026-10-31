package com.mentenova.dto.cita;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaDTO(
        Long id,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long idPaciente,
        String paciente,
        String psicologo,
        String modalidad,
        String estadoCodigo,
        String estadoNombre) {
}
