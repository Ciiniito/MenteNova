package com.mentenova.controller;
import com.mentenova.dto.*;
import com.mentenova.entity.*;
import com.mentenova.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.*;
import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
public class DisponibilidadController {
    private final DisponibilidadService servicio;
    private final AccesoPsicologoService acceso;
    private final UsuarioSesionService sesiones;
    private final Clock reloj;
    public DisponibilidadController(DisponibilidadService servicio,AccesoPsicologoService acceso,UsuarioSesionService sesiones,Clock reloj) {this.servicio=servicio;this.acceso=acceso;this.sesiones=sesiones;this.reloj=reloj;}
    @GetMapping("/psicologos/{id}/disponibilidad")
    public String pantalla(@PathVariable("id")Long id,@RequestParam(value="horarioId",required=false)Long horarioId,
            @RequestParam(value="excepcionId",required=false)Long excepcionId,
            @RequestParam(value="fecha",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fecha,
            @RequestParam(value="modalidad",defaultValue="PRESENCIAL")Modalidad modalidad,Authentication auth,Model model) {
        model.addAttribute("horarioId",horarioId);model.addAttribute("excepcionId",excepcionId);
        model.addAttribute("horarioForm",horarioId==null?new HorarioForm():servicio.formularioHorario(id,horarioId));
        model.addAttribute("excepcionForm",excepcionId==null?new ExcepcionForm():servicio.formularioExcepcion(id,excepcionId));
        cargar(id,auth,model,fecha,modalidad);return "disponibilidad/horarios";
    }
    @PostMapping("/psicologos/{id}/disponibilidad/horarios/guardar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarHorario(@PathVariable("id")Long id,@RequestParam(value="horarioId",required=false)Long horarioId,
            @Valid @ModelAttribute("horarioForm")HorarioForm form,BindingResult errores,Authentication auth,Model model,RedirectAttributes flash) {
        // Incluso con datos inválidos, se verifica la propiedad del profesional solicitado.
        acceso.consultar(id);
        if(!errores.hasErrors())try {servicio.guardarHorario(id,horarioId,form);flash.addFlashAttribute("exito","Horario guardado correctamente.");return ruta(id);}
            catch(IllegalArgumentException ex){errores.reject("horario",ex.getMessage());}
            catch(DataIntegrityViolationException ex){errores.reject("horario","No se pudo guardar. Revisa que la franja no esté repetida.");}
        model.addAttribute("horarioId",horarioId);model.addAttribute("excepcionForm",new ExcepcionForm());
        cargar(id,auth,model,null,Modalidad.PRESENCIAL);return "disponibilidad/horarios";
    }
    @PostMapping("/psicologos/{id}/disponibilidad/excepciones/guardar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarExcepcion(@PathVariable("id")Long id,@RequestParam(value="excepcionId",required=false)Long excepcionId,
            @Valid @ModelAttribute("excepcionForm")ExcepcionForm form,BindingResult errores,Authentication auth,Model model,RedirectAttributes flash) {
        acceso.consultar(id);
        if(!errores.hasErrors())try {servicio.guardarExcepcion(id,excepcionId,form);flash.addFlashAttribute("exito","Excepción guardada correctamente.");return ruta(id);}
            catch(IllegalArgumentException ex){errores.reject("excepcion",ex.getMessage());}
        model.addAttribute("excepcionId",excepcionId);model.addAttribute("horarioForm",new HorarioForm());
        cargar(id,auth,model,null,Modalidad.PRESENCIAL);return "disponibilidad/horarios";
    }
    @GetMapping("/psicologos/{id}/disponibilidad/{tipo:horarios|excepciones}/{registroId}/eliminar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String confirmar(@PathVariable("id")Long id,@PathVariable("tipo")String tipo,@PathVariable("registroId")Long registroId,Authentication auth,Model model) {
        acceso.modificar(id);model.addAttribute("usuarioSesion",sesiones.requerir(auth));model.addAttribute("psicologo",servicio.profesional(id));
        model.addAttribute("tipo",tipo);model.addAttribute("registroId",registroId);
        model.addAttribute("registro",tipo.equals("horarios")?servicio.formularioHorario(id,registroId):servicio.formularioExcepcion(id,registroId));
        return "disponibilidad/eliminar";
    }
    @PostMapping("/psicologos/{id}/disponibilidad/{tipo:horarios|excepciones}/{registroId}/eliminar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String eliminar(@PathVariable("id")Long id,@PathVariable("tipo")String tipo,@PathVariable("registroId")Long registroId,RedirectAttributes flash) {
        try {if(tipo.equals("horarios"))servicio.eliminarHorario(id,registroId);else servicio.eliminarExcepcion(id,registroId);flash.addFlashAttribute("exito","Registro eliminado correctamente.");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return ruta(id);
    }
    private void cargar(Long id,Authentication auth,Model model,LocalDate fecha,Modalidad modalidad) {
        var usuario=sesiones.requerir(auth);var p=servicio.profesional(id);
        model.addAttribute("usuarioSesion",usuario);model.addAttribute("psicologo",p);
        model.addAttribute("puedeEditar",usuario.esAdministrador()&&Boolean.TRUE.equals(p.getActivo())&&Boolean.TRUE.equals(p.getUsuario().getActivo()));
        model.addAttribute("horarios",servicio.horarios(id));model.addAttribute("excepciones",servicio.excepciones(id));
        model.addAttribute("dias",List.of("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"));
        LocalDate dia=fecha==null?LocalDate.now(reloj):fecha;
        model.addAttribute("fechaConsulta",dia);model.addAttribute("modalidadConsulta",modalidad);
        model.addAttribute("consulta",servicio.consultar(id,dia,modalidad,null));
    }
    private String ruta(Long id){return "redirect:/psicologos/"+id+"/disponibilidad";}
}
