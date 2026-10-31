package com.mentenova.controller;
import com.mentenova.dto.*;
import com.mentenova.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EspecialidadController {
    private final EspecialidadService especialidades;
    private final PsicologoService psicologos;
    private final UsuarioSesionService sesiones;
    public EspecialidadController(EspecialidadService especialidades,PsicologoService psicologos,UsuarioSesionService sesiones) {this.especialidades=especialidades;this.psicologos=psicologos;this.sesiones=sesiones;}
    private void sesion(Authentication auth,Model model) {model.addAttribute("usuarioSesion",sesiones.requerir(auth));}
    @GetMapping("/especialidades")
    public String listado(Authentication auth,Model model) {
        sesion(auth,model);model.addAttribute("especialidades",especialidades.listar());return "especialidades/listado";
    }
    @GetMapping("/especialidades/nuevo")
    public String nuevo(Authentication auth,Model model) {
        sesion(auth,model);model.addAttribute("especialidadForm",new EspecialidadForm());return "especialidades/formulario";
    }
    @GetMapping("/especialidades/{id}/editar")
    public String editar(@PathVariable("id")Short id,Authentication auth,Model model) {
        sesion(auth,model);model.addAttribute("especialidadId",id);model.addAttribute("especialidadForm",especialidades.formulario(id));return "especialidades/formulario";
    }
    @PostMapping("/especialidades/guardar")
    public String guardar(@RequestParam(value="id",required=false)Short id,@Valid @ModelAttribute("especialidadForm")EspecialidadForm form,
            BindingResult errores,Authentication auth,Model model,RedirectAttributes flash) {
        sesion(auth,model);model.addAttribute("especialidadId",id);
        if(!errores.hasErrors()) {
            try {especialidades.guardar(id,form);flash.addFlashAttribute("exito","Especialidad guardada correctamente.");return "redirect:/especialidades";}
            catch(IllegalArgumentException ex){errores.reject("especialidad",ex.getMessage());}
            catch(DataIntegrityViolationException ex){errores.reject("duplicado","No se pudo guardar. Revisa que el nombre sea único.");}
        }
        return "especialidades/formulario";
    }
    @GetMapping("/psicologos/{id}/especialidades")
    public String asignacion(@PathVariable("id")Long id,Authentication auth,Model model) {
        sesion(auth,model);cargarAsignacion(id,model);return "especialidades/asignacion";
    }
    @PostMapping("/psicologos/{id}/especialidades")
    public String asignar(@PathVariable("id")Long id,@RequestParam(value="especialidadIds",required=false)List<Short> seleccion,
            Authentication auth,Model model,RedirectAttributes flash) {
        try {especialidades.asignar(id,seleccion);flash.addFlashAttribute("exito","Especialidades del psicólogo actualizadas.");return "redirect:/psicologos";}
        catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        sesion(auth,model);cargarAsignacion(id,model);return "especialidades/asignacion";
    }
    private void cargarAsignacion(Long id,Model model) {
        model.addAttribute("psicologo",psicologos.obtenerParaEliminar(id));model.addAttribute("especialidades",especialidades.listar());model.addAttribute("asignadas",especialidades.asignadas(id));
    }
}
