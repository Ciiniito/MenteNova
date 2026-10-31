package com.mentenova.dto;
import java.time.LocalDate;
import java.util.List;
import com.mentenova.entity.Modalidad;
public record ConsultaDisponibilidadDTO(Long psicologoId, LocalDate fecha,
        Modalidad modalidad, List<TramoHorario> intervalos) {}
