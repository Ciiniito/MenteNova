package com.mentenova.dto.dashboard;

import java.time.LocalDate;

public record DiaCalendarioDTO(LocalDate fecha, boolean delMes, boolean hoy, long citas) {
}
