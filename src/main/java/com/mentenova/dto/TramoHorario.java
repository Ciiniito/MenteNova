package com.mentenova.dto;
import java.time.LocalTime;
public record TramoHorario(LocalTime inicio, LocalTime fin) {
    public TramoHorario {
        if (inicio == null || fin == null || !fin.isAfter(inicio))
            throw new IllegalArgumentException("La hora de fin debe ser posterior al inicio.");
    }
}
