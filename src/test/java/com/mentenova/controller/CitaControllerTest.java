package com.mentenova.controller;

import com.mentenova.config.ZonaHorariaConfig;
import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.cita.CitaDTO;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.entity.Modalidad;
import com.mentenova.security.MethodSecurityConfig;
import com.mentenova.security.SecurityConfig;
import com.mentenova.service.CitaService;
import com.mentenova.service.UsuarioSesionService;
import com.mentenova.service.ValidacionCitaException;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
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

@WebMvcTest(CitaController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class, ZonaHorariaConfig.class})
class CitaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CitaService citaService;

    @MockBean
    private UsuarioSesionService usuarioSesionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void configurar() {
        when(citaService.estados()).thenReturn(List.of(new OpcionDTO("PENDIENTE", "Pendiente")));
        when(citaService.pacientes()).thenReturn(List.of(new OpcionDTO("5", "María González Pérez · DNI 70000001")));
        when(citaService.psicologos()).thenReturn(List.of(new OpcionDTO("1", "Diego Ramos Chávez")));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void laBandejaMuestraLasProximasCitas() throws Exception {
        sesion("RECEPCIONISTA");
        when(citaService.buscar(any(), any(), any(), anyInt())).thenReturn(PaginaDTO.de(List.of(new CitaDTO(
                600L, LocalDate.of(2026, 11, 2), LocalTime.of(9, 0), LocalTime.of(10, 0), 5L,
                "María González Pérez", "Diego Ramos Chávez", "PRESENCIAL", "PENDIENTE", "Pendiente")), 1, 15, 1));

        mvc.perform(get("/citas"))
                .andExpect(status().isOk())
                .andExpect(view().name("citas/index"))
                .andExpect(content().string(containsString("Próximas citas")))
                .andExpect(content().string(containsString("María González Pérez")))
                .andExpect(content().string(containsString("02/11/2026")))
                .andExpect(content().string(containsString("09:00 – 10:00")))
                .andExpect(content().string(containsString("Gestión de citas")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void elFormularioListaPacientesYPsicologosActivos() throws Exception {
        sesion("ADMINISTRADOR");

        mvc.perform(get("/citas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("citas/registrar"))
                .andExpect(content().string(containsString("María González Pérez · DNI 70000001")))
                .andExpect(content().string(containsString("Diego Ramos Chávez")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void registrarRedirigeALaBandejaDeLaFecha() throws Exception {
        sesion("RECEPCIONISTA");
        when(citaService.registrar(any(), any())).thenReturn(600L);

        mvc.perform(post("/citas").with(csrf())
                        .param("idPaciente", "5")
                        .param("idPsicologo", "1")
                        .param("fecha", "2026-11-02")
                        .param("horaInicio", "09:00")
                        .param("horaFin", "10:00")
                        .param("modalidad", "VIRTUAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citas?fecha=2026-11-02&registrada=600"))
                .andExpect(flash().attributeExists("exito"));
        verify(citaService).registrar(any(), argThat(form -> form.getIdPaciente() == 5L
                && form.getHoraInicio().equals(LocalTime.of(9, 0))
                && form.getModalidad() == Modalidad.VIRTUAL));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void losCamposObligatoriosNoLleganAlServicio() throws Exception {
        sesion("RECEPCIONISTA");

        mvc.perform(post("/citas").with(csrf()).param("modalidad", "PRESENCIAL"))
                .andExpect(status().isOk())
                .andExpect(view().name("citas/registrar"))
                .andExpect(model().attributeHasFieldErrors("cita", "idPaciente", "idPsicologo", "fecha", "horaInicio", "horaFin"));
        verify(citaService, never()).registrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void losRechazosDelServicioSeMuestranEnElFormulario() throws Exception {
        sesion("RECEPCIONISTA");
        when(citaService.registrar(any(), any())).thenThrow(new ValidacionCitaException(
                Map.of("horaInicio", "El psicólogo no está disponible en ese horario.")));

        mvc.perform(post("/citas").with(csrf())
                        .param("idPaciente", "5")
                        .param("idPsicologo", "1")
                        .param("fecha", "2026-11-02")
                        .param("horaInicio", "15:00")
                        .param("horaFin", "16:00")
                        .param("modalidad", "PRESENCIAL"))
                .andExpect(status().isOk())
                .andExpect(view().name("citas/registrar"))
                .andExpect(content().string(containsString("El psicólogo no está disponible en ese horario.")));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void registrarSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/citas").param("idPaciente", "5")).andExpect(status().isForbidden());
        verify(citaService, never()).registrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "PSICOLOGO")
    void elPsicologoNoGestionaCitas() throws Exception {
        mvc.perform(get("/citas")).andExpect(status().isForbidden());
        mvc.perform(get("/citas/nueva")).andExpect(status().isForbidden());
        mvc.perform(post("/citas").with(csrf()).param("idPaciente", "5")).andExpect(status().isForbidden());
        verify(citaService, never()).registrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void elPacienteNoGestionaCitas() throws Exception {
        mvc.perform(get("/citas")).andExpect(status().isForbidden());
    }

    private void sesion(String rol) {
        when(usuarioSesionService.requerir(any())).thenReturn(
                new UsuarioSesionDTO(1L, "Ana", "Ana Martínez Soto", "AM", rol, rol));
    }
}
