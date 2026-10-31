package com.mentenova.dto.cita;

import java.time.LocalDate;

/**
 * Criterios de la bandeja de citas. Sin fecha se listan las citas desde {@code desde} en adelante.
 */
public record FiltroCitaDTO(LocalDate fecha, LocalDate desde, String estado, String paciente) {
}
