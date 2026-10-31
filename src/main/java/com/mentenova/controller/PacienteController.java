package com.mentenova.controller;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.paciente.PacienteFormDTO;
import com.mentenova.service.PacienteService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.service.ValidacionPacienteException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private static final int HISTORIAL_EN_FICHA = 5;

    private final PacienteService pacienteService;
    private final UsuarioSesionService usuarioSesionService;
    private final Clock reloj;

    public PacienteController(PacienteService pacienteService, UsuarioSesionService usuarioSesionService, Clock reloj) {
        this.pacienteService = pacienteService;
        this.usuarioSesionService = usuarioSesionService;
        this.reloj = reloj;
    }

    @ModelAttribute("usuarioSesion")
    public UsuarioSesionDTO usuarioSesion(Authentication authentication) {
        return usuarioSesionService.requerir(authentication);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PSICOLOGO')")
    public String bandeja(@RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "estado", required = false) String estado,
                          @RequestParam(value = "pagina", defaultValue = "1") int pagina,
                          @RequestParam(value = "seleccionado", required = false) Long seleccionado,
                          @RequestParam(value = "pestana", required = false) String pestana,
                          @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                          Model model) {
        String estadoFiltro = PacienteService.normalizarEstado(estado);
        model.addAttribute("q", q == null || q.isBlank() ? null : q.strip());
        model.addAttribute("estado", estadoFiltro);
        model.addAttribute("resultado", pacienteService.buscar(usuario, q, estadoFiltro, pagina));
        if (seleccionado != null) {
            model.addAttribute("seleccion", pacienteService.detalle(usuario, seleccionado, HISTORIAL_EN_FICHA));
            model.addAttribute("pestana", "historial".equals(pestana) ? "historial" : "resumen");
        }
        return "pacientes/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public String nuevo(@RequestParam(value = "q", required = false) String q, Model model) {
        PacienteFormDTO form = new PacienteFormDTO();
        if (q != null && !q.isBlank()) {
            String texto = q.strip();
            if (texto.matches("\\d{8}")) {
                form.setTipoDocumento("DNI");
                form.setNumeroDocumento(texto);
            } else {
                form.setNombres(texto);
            }
        }
        model.addAttribute("paciente", form);
        prepararFormulario(model, null);
        return "pacientes/formulario";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public String registrar(@Valid @ModelAttribute("paciente") PacienteFormDTO form,
                            BindingResult result,
                            @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                Long id = pacienteService.registrar(usuario, form);
                redirectAttributes.addFlashAttribute("exito", "Paciente registrado correctamente.");
                return "redirect:/pacientes?seleccionado=" + id;
            } catch (ValidacionPacienteException e) {
                rechazar(result, e);
            }
        }
        prepararFormulario(model, null);
        return "pacientes/formulario";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PSICOLOGO')")
    public String detalle(@PathVariable("id") Long id,
                          @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                          Model model) {
        model.addAttribute("paciente", pacienteService.detalle(usuario, id, null));
        return "pacientes/detalle";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public String editar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("paciente", pacienteService.formulario(id));
        prepararFormulario(model, id);
        return "pacientes/formulario";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public String actualizar(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("paciente") PacienteFormDTO form,
                             BindingResult result,
                             @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                pacienteService.actualizar(usuario, id, form);
                redirectAttributes.addFlashAttribute("exito", "Datos del paciente actualizados.");
                return "redirect:/pacientes/" + id;
            } catch (ValidacionPacienteException e) {
                rechazar(result, e);
            }
        }
        prepararFormulario(model, id);
        return "pacientes/formulario";
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public String cambiarEstado(@PathVariable("id") Long id,
                                @RequestParam("activo") boolean activo,
                                @ModelAttribute(name = "usuarioSesion", binding = false) UsuarioSesionDTO usuario,
                                RedirectAttributes redirectAttributes) {
        pacienteService.cambiarEstado(usuario, id, activo);
        redirectAttributes.addFlashAttribute("exito", activo ? "Paciente activado." : "Paciente desactivado.");
        return "redirect:/pacientes/" + id;
    }

    private void prepararFormulario(Model model, Long idPaciente) {
        model.addAttribute("idPaciente", idPaciente);
        model.addAttribute("tiposDocumento", pacienteService.tiposDocumento());
        model.addAttribute("hoy", LocalDate.now(reloj));
    }

    static void rechazar(BindingResult result, ValidacionPacienteException e) {
        e.getErrores().forEach((campo, mensaje) -> {
            if (ValidacionPacienteException.GENERAL.equals(campo)) {
                result.reject(campo, mensaje);
            } else {
                result.rejectValue(campo, "invalido", mensaje);
            }
        });
    }
}
