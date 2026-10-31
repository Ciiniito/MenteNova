package com.mentenova.controller;

import com.mentenova.entity.Cita;
import com.mentenova.service.CitaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    @GetMapping("/citas")
    public String listarCitas(Model model) {

        model.addAttribute("citas",
                citaService.listarTodas());

        return "citas/listar";
    }

    @GetMapping("/citas/nueva")
    public String nuevaCita(Model model) {

        model.addAttribute("cita",
                new Cita());

        return "citas/registrar";
    }
}