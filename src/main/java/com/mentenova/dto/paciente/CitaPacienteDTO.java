package com.mentenova.dto.paciente;

import java.time.LocalDate;
import java.time.LocalTime;

public record CitaPacienteDTO(
        Long idCita,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String psicologo,
        String modalidad,
        String estadoCodigo,
        String estadoNombre,
        String estadoPago) {

    public CitaPacienteDTO sinPago() {
        return new CitaPacienteDTO(idCita, fecha, horaInicio, horaFin, psicologo, modalidad,
                estadoCodigo, estadoNombre, null);
    }
}
