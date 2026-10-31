package com.mentenova.dto;
import java.time.LocalDate;
import java.util.List;
public record DiaAgendaDTO(LocalDate fecha,List<CitaAgendaDTO> citas) {}
