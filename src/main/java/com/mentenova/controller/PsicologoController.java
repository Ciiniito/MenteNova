package com.mentenova.controller;

import com.mentenova.dto.PsicologoForm;
import com.mentenova.service.PsicologoService;
import com.mentenova.service.UsuarioSesionService;
import jakarta.validation.groups.Default;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/psicologos")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class PsicologoController {
    private final PsicologoService psicologos;
    private final UsuarioSesionService sesiones;

    public PsicologoController(PsicologoService psicologos, UsuarioSesionService sesiones) {
        this.psicologos = psicologos;
        this.sesiones = sesiones;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        sesion(authentication, model);
        model.addAttribute("psicologos", psicologos.listar());
        return "psicologos/listado";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String nuevo(Authentication authentication, Model model) {
        prepararFormulario(authentication, model, false, null);
        model.addAttribute("psicologoForm", new PsicologoForm());
        return "psicologos/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardar(
            @Validated({Default.class, PsicologoForm.Creacion.class})
            @ModelAttribute("psicologoForm") PsicologoForm form,
            BindingResult errores, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        prepararFormulario(authentication, model, false, null);
        if (errores.hasErrors()) return formularioConErrores(form);
        try {
            psicologos.registrar(form);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errores.reject("registro", ex.getMessage());
            return formularioConErrores(form);
        } catch (DataIntegrityViolationException ex) {
            errores.reject("duplicado", "No se pudo registrar. Verifica que el correo y la colegiatura sean únicos.");
            return formularioConErrores(form);
        }
        redirect.addFlashAttribute("exito", "Psicólogo registrado correctamente.");
        return "redirect:/psicologos";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@PathVariable("id") Long id, Authentication authentication, Model model) {
        prepararFormulario(authentication, model, true, id);
        model.addAttribute("psicologoForm", psicologos.obtenerFormulario(id));
        return "psicologos/formulario";
    }

    @PostMapping("/{id}/actualizar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String actualizar(@PathVariable("id") Long id,
            @Validated({Default.class, PsicologoForm.Edicion.class})
            @ModelAttribute("psicologoForm") PsicologoForm form,
            BindingResult errores, Authentication authentication,
            Model model, RedirectAttributes redirect) {
        prepararFormulario(authentication, model, true, id);
        if (errores.hasErrors()) return formularioConErrores(form);
        try {
            psicologos.actualizar(id, form);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errores.reject("actualizacion", ex.getMessage());
            return formularioConErrores(form);
        } catch (DataIntegrityViolationException ex) {
            errores.reject("duplicado", "No se pudieron guardar los cambios. Verifica que el correo y la colegiatura sean únicos.");
            return formularioConErrores(form);
        }
        redirect.addFlashAttribute("exito", "Datos del psicólogo actualizados correctamente.");
        return "redirect:/psicologos";
    }

    @GetMapping("/{id}/eliminar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String confirmarEliminacion(@PathVariable("id") Long id,
            Authentication authentication, Model model) {
        sesion(authentication, model);
        model.addAttribute("psicologo", psicologos.obtenerParaEliminar(id));
        return "psicologos/eliminar";
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            psicologos.eliminar(id);
            redirect.addFlashAttribute("exito", "Psicólogo y cuenta de acceso eliminados correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("error", "No se puede eliminar porque tiene citas, horarios u otros registros vinculados. Puedes desactivar su cuenta desde Editar.");
        }
        return "redirect:/psicologos";
    }

    private void sesion(Authentication authentication, Model model) {
        model.addAttribute("usuarioSesion", sesiones.requerir(authentication));
    }

    private void prepararFormulario(Authentication authentication, Model model,
            boolean edicion, Long id) {
        sesion(authentication, model);
        model.addAttribute("edicion", edicion);
        model.addAttribute("psicologoId", id);
    }

    private String formularioConErrores(PsicologoForm form) {
        form.setPassword(null);
        return "psicologos/formulario";
    }
}
