package com.mentenova.controller;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.cita.RegistrarCitaDTO;
import com.mentenova.service.CitaService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.service.ValidacionCitaException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDate;

@Controller
@RequestMapping("/citas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class CitaController {

    private final CitaService citaService;
    private final UsuarioSesionService usuarioSesionService;
    private final Clock reloj;

    public CitaController(CitaService citaService, UsuarioSesionService usuarioSesionService, Clock reloj) {
        this.citaService = citaService;
        this.usuarioSesionService = usuarioSesionService;
        this.reloj = reloj;
    }

    @ModelAttribute("usuarioSesion")
    public UsuarioSesionDTO usuarioSesion(Authentication authentication) {
        return usuarioSesionService.requerir(authentication);
    }

    @GetMapping
    public String listarCitas(@RequestParam(value = "fecha", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                              @RequestParam(value = "estado", required = false) String estado,
                              @RequestParam(value = "paciente", required = false) String paciente,
                              @RequestParam(value = "pagina", defaultValue = "1") int pagina,
                              @RequestParam(value = "registrada", required = false) Long registrada,
                              Model model) {
        model.addAttribute("fecha", fecha);
        model.addAttribute("estado", citaService.normalizarEstado(estado));
        model.addAttribute("paciente", paciente == null || paciente.isBlank() ? null : paciente.strip());
        model.addAttribute("registrada", registrada);
        model.addAttribute("estados", citaService.estados());
        model.addAttribute("resultado", citaService.buscar(fecha, estado, paciente, pagina));
        return "citas/index";
    }

    @GetMapping("/nueva")
    public String nuevaCita(Model model) {
        model.addAttribute("cita", new RegistrarCitaDTO());
        prepararFormulario(model);
        return "citas/registrar";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("cita") RegistrarCitaDTO form,
                            BindingResult result,
                            @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                Long id = citaService.registrar(usuario, form);
                redirectAttributes.addFlashAttribute("exito", "Cita registrada correctamente. Queda pendiente de confirmación.");
                return "redirect:" + UriComponentsBuilder.fromPath("/citas")
                        .queryParam("fecha", form.getFecha())
                        .queryParam("registrada", id)
                        .toUriString();
            } catch (ValidacionCitaException e) {
                e.getErrores().forEach((campo, mensaje) -> {
                    if (ValidacionCitaException.GENERAL.equals(campo)) {
                        result.reject(campo, mensaje);
                    } else {
                        result.rejectValue(campo, "invalido", mensaje);
                    }
                });
            }
        }
        prepararFormulario(model);
        return "citas/registrar";
    }

    private void prepararFormulario(Model model) {
        model.addAttribute("pacientes", citaService.pacientes());
        model.addAttribute("psicologos", citaService.psicologos());
        model.addAttribute("hoy", LocalDate.now(reloj));
    }
}
