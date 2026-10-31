package com.mentenova.dto.paciente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PacienteDetalleDTO(
        Long id,
        String nombreCompleto,
        String iniciales,
        String nombres,
        String apellidos,
        String documento,
        LocalDate fechaNacimiento,
        Integer edad,
        String telefono,
        String correo,
        String direccion,
        boolean activo,
        LocalDateTime fechaRegistro,
        String correoCuenta,
        long citasRegistradas,
        CitaPacienteDTO proximaCita,
        String psicologoAsignado,
        List<CitaPacienteDTO> historial,
        List<CambioPacienteDTO> cambios,
        boolean muestraPagos) {
}
