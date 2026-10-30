package com.mentenova.controller;
import com.mentenova.dto.ConsultaDisponibilidadDTO;
import com.mentenova.entity.Modalidad;
import com.mentenova.service.DisponibilidadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/disponibilidad")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','PSICOLOGO')")
public class DisponibilidadApiController {
    private final DisponibilidadService servicio;
    public DisponibilidadApiController(DisponibilidadService servicio){this.servicio=servicio;}
    @GetMapping
    public ConsultaDisponibilidadDTO consultar(@RequestParam("psicologoId")Long id,
            @RequestParam("fecha")@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fecha,
            @RequestParam(value="modalidad",defaultValue="PRESENCIAL")Modalidad modalidad,
            @RequestParam(value="especialidadId",required=false)Short especialidadId) {
        return servicio.consultar(id,fecha,modalidad,especialidadId);
    }
}
