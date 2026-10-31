package com.mentenova.controller;

import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.paciente.CitaPacienteDTO;
import com.mentenova.dto.paciente.PacienteDetalleDTO;
import com.mentenova.dto.paciente.PacienteFilaDTO;
import com.mentenova.dto.paciente.PacienteFormDTO;
import com.mentenova.service.PacienteService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.service.ValidacionPacienteException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteApiController {

    private final PacienteService pacienteService;
    private final UsuarioSesionService usuarioSesionService;

    public PacienteApiController(PacienteService pacienteService, UsuarioSesionService usuarioSesionService) {
        this.pacienteService = pacienteService;
        this.usuarioSesionService = usuarioSesionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public PaginaDTO<PacienteFilaDTO> buscar(@RequestParam(value = "q", required = false) String q,
                                             @RequestParam(value = "estado", required = false) String estado,
                                             @RequestParam(value = "pagina", defaultValue = "1") int pagina,
                                             Authentication authentication) {
        return pacienteService.buscar(usuarioSesionService.requerir(authentication), q, estado, pagina);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody PacienteFormDTO form,
                                                         Authentication authentication) {
        Long id = pacienteService.registrar(usuarioSesionService.requerir(authentication), form);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(ubicacion).body(Map.of("id", id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PSICOLOGO')")
    public PacienteDetalleDTO detalle(@PathVariable("id") Long id, Authentication authentication) {
        return pacienteService.detalle(usuarioSesionService.requerir(authentication), id, null);
    }

    @GetMapping("/{id}/historial-citas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'PSICOLOGO')")
    public List<CitaPacienteDTO> historial(@PathVariable("id") Long id, Authentication authentication) {
        return pacienteService.historial(usuarioSesionService.requerir(authentication), id);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return respuestaInvalida(errores);
    }

    @ExceptionHandler(ValidacionPacienteException.class)
    public ResponseEntity<Map<String, Object>> reglasIncumplidas(ValidacionPacienteException e) {
        return respuestaInvalida(e.getErrores());
    }

    private ResponseEntity<Map<String, Object>> respuestaInvalida(Map<String, String> errores) {
        return ResponseEntity.badRequest().body(Map.of(
                "mensaje", "Revisa los datos del paciente.",
                "errores", errores));
    }
}
