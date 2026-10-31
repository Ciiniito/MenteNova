package com.mentenova.controller;

import com.mentenova.config.ZonaHorariaConfig;
import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.paciente.CambioPacienteDTO;
import com.mentenova.dto.paciente.CitaPacienteDTO;
import com.mentenova.dto.paciente.PacienteDetalleDTO;
import com.mentenova.dto.paciente.PacienteFilaDTO;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.security.MethodSecurityConfig;
import com.mentenova.security.SecurityConfig;
import com.mentenova.service.PacienteService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.service.ValidacionPacienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PacienteController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class, ZonaHorariaConfig.class})
class PacienteControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PacienteService pacienteService;

    @MockBean
    private UsuarioSesionService usuarioSesionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void configurar() {
        when(pacienteService.tiposDocumento()).thenReturn(List.of(new OpcionDTO("DNI", "DNI")));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void laBandejaMuestraLosPacientesYLaPaginacion() throws Exception {
        sesion("RECEPCIONISTA");
        when(pacienteService.buscar(any(), any(), eq("ACTIVOS"), eq(1))).thenReturn(pagina());

        mvc.perform(get("/pacientes"))
                .andExpect(status().isOk())
                .andExpect(view().name("pacientes/lista"))
                .andExpect(content().string(containsString("María González Pérez")))
                .andExpect(content().string(containsString("DNI 70000001")))
                .andExpect(content().string(containsString("Mostrando 1 - 1 de 1 pacientes")))
                .andExpect(content().string(containsString("Nuevo paciente")));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void laFichaLateralMuestraElResumenDelPaciente() throws Exception {
        sesion("RECEPCIONISTA");
        when(pacienteService.buscar(any(), any(), any(), anyInt())).thenReturn(pagina());
        when(pacienteService.detalle(any(), eq(1L), eq(5))).thenReturn(detalle());

        mvc.perform(get("/pacientes").param("seleccionado", "1").param("pestana", "historial"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Psicólogo asignado")))
                .andExpect(content().string(containsString("Laura Méndez Ríos")))
                .andExpect(content().string(containsString("Pagado")));
    }

    @Test
    @WithMockUser(roles = "PSICOLOGO")
    void elPsicologoVeSusPacientesSinOpcionesDeGestion() throws Exception {
        sesion("PSICOLOGO");
        when(pacienteService.buscar(any(), any(), any(), anyInt())).thenReturn(pagina());

        mvc.perform(get("/pacientes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Citas contigo")))
                .andExpect(content().string(not(containsString("Nuevo paciente"))));
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void elPacienteNoAccedeALaBandeja() throws Exception {
        sesion("PACIENTE");
        mvc.perform(get("/pacientes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PSICOLOGO")
    void elPsicologoNoRegistraPacientes() throws Exception {
        sesion("PSICOLOGO");
        mvc.perform(get("/pacientes/nuevo")).andExpect(status().isForbidden());
        mvc.perform(post("/pacientes").with(csrf()).param("nombres", "Lucía")).andExpect(status().isForbidden());
        verify(pacienteService, never()).registrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void unRegistroValidoLlevaALaFichaDelPaciente() throws Exception {
        sesion("ADMINISTRADOR");
        when(pacienteService.registrar(any(), any())).thenReturn(30L);

        mvc.perform(post("/pacientes").with(csrf())
                        .param("nombres", "Lucía")
                        .param("apellidos", "Fernández Rivas")
                        .param("telefono", "+51 987 654 321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pacientes?seleccionado=30"))
                .andExpect(flash().attribute("exito", "Paciente registrado correctamente."));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void unRegistroIncompletoMuestraLosErroresEnElFormulario() throws Exception {
        sesion("ADMINISTRADOR");

        mvc.perform(post("/pacientes").with(csrf()).param("telefono", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("pacientes/formulario"))
                .andExpect(model().attributeHasFieldErrors("paciente", "nombres", "apellidos", "telefono"))
                .andExpect(content().string(containsString("Los nombres son obligatorios")));
        verify(pacienteService, never()).registrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void unDocumentoDuplicadoSeMarcaEnSuCampo() throws Exception {
        sesion("RECEPCIONISTA");
        when(pacienteService.registrar(any(), any())).thenThrow(new ValidacionPacienteException(
                Map.of("numeroDocumento", "Ya existe un paciente registrado con ese documento.")));

        mvc.perform(post("/pacientes").with(csrf())
                        .param("nombres", "Lucía")
                        .param("apellidos", "Fernández Rivas")
                        .param("tipoDocumento", "DNI")
                        .param("numeroDocumento", "70000001")
                        .param("telefono", "+51 987 654 321"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("paciente", "numeroDocumento"))
                .andExpect(content().string(containsString("Ya existe un paciente registrado con ese documento.")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void laFichaCompletaMuestraElHistorialYLosCambios() throws Exception {
        sesion("ADMINISTRADOR");
        when(pacienteService.detalle(any(), eq(1L), eq(null))).thenReturn(detalle());

        mvc.perform(get("/pacientes/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pacientes/detalle"))
                .andExpect(content().string(containsString("Historial de citas")))
                .andExpect(content().string(containsString("Cambios recientes")))
                .andExpect(content().string(containsString("Teléfono, Correo")));
    }

    private void sesion(String rol) {
        when(usuarioSesionService.requerir(any())).thenReturn(
                new UsuarioSesionDTO(1L, "Ana", "Ana Martínez Soto", "AM", rol, rol));
    }

    private PaginaDTO<PacienteFilaDTO> pagina() {
        return PaginaDTO.de(List.of(
                PacienteFilaDTO.de(1L, "María", "González Pérez", "DNI", "70000001", "+51 987 654 321", true, 3)),
                1, 10, 1);
    }

    private PacienteDetalleDTO detalle() {
        CitaPacienteDTO cita = new CitaPacienteDTO(100L, LocalDate.of(2026, 10, 20), LocalTime.of(9, 0),
                LocalTime.of(10, 0), "Laura Méndez Ríos", "PRESENCIAL", "ATENDIDA", "Atendida", "PAGADO");
        return new PacienteDetalleDTO(1L, "María González Pérez", "MG", "María", "González Pérez",
                "DNI 70000001", LocalDate.of(1997, 3, 14), 29, "+51 987 654 321", "maria@correo.com",
                "Av. Arequipa 1234", true, LocalDateTime.of(2026, 10, 29, 23, 0), "paciente@demo.mentenova.local",
                1, null, "Laura Méndez Ríos", List.of(cita),
                List.of(new CambioPacienteDTO(LocalDateTime.of(2026, 10, 30, 10, 0),
                        "Ana Martínez Soto", "UPDATE", "Teléfono, Correo")),
                true);
    }
}
