package com.mentenova.controller;
import com.mentenova.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
@Controller
@RequestMapping("/agenda")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','PSICOLOGO')")
public class AgendaController {
    private final AgendaService agenda;
    private final AccesoPsicologoService acceso;
    private final UsuarioSesionService sesiones;
    private final Clock reloj;
    public AgendaController(AgendaService agenda,AccesoPsicologoService acceso,UsuarioSesionService sesiones,Clock reloj) {this.agenda=agenda;this.acceso=acceso;this.sesiones=sesiones;this.reloj=reloj;}
    @GetMapping
    public String consultar(@RequestParam(value="fecha",required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate fecha,
            @RequestParam(value="vista",defaultValue="dia")String vista,@RequestParam(value="psicologoId",required=false)Long psicologoId,
            Authentication auth,Model model) {
        if(!vista.equals("dia")&&!vista.equals("semana"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona día o semana.");
        var dia=fecha==null?LocalDate.now(reloj):fecha;var id=acceso.filtro(psicologoId);
        var dias=agenda.consultar(id,dia,vista.equals("semana"));
        model.addAttribute("usuarioSesion",sesiones.requerir(auth));model.addAttribute("profesionales",agenda.profesionales());
        model.addAttribute("fecha",dia);model.addAttribute("vista",vista);model.addAttribute("psicologoId",id);model.addAttribute("diasAgenda",dias);
        model.addAttribute("desde",dias.get(0).fecha());model.addAttribute("hasta",dias.get(dias.size()-1).fecha());
        model.addAttribute("anterior",dia.minusDays(vista.equals("semana")?7:1));model.addAttribute("siguiente",dia.plusDays(vista.equals("semana")?7:1));
        model.addAttribute("idioma",java.util.Locale.forLanguageTag("es-PE"));
        model.addAttribute("hoy",LocalDate.now(reloj));model.addAttribute("totalCitas",dias.stream().mapToInt(d->d.citas().size()).sum());
        return "agenda/index";
    }
}
