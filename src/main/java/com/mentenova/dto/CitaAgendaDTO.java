package com.mentenova.dto;
import java.time.*;
public record CitaAgendaDTO(Long id, LocalDate fecha, LocalTime inicio, LocalTime fin,
        String paciente, String psicologo, String modalidad, String estado, String estadoNombre) {}
