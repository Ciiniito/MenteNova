package com.mentenova.dto.paciente;

import java.time.LocalDateTime;

public record CambioPacienteDTO(LocalDateTime fecha, String usuario, String accion, String descripcion) {
}
