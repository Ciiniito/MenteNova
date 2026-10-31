package com.mentenova.controller;

import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.paciente.CitaPacienteDTO;
import com.mentenova.dto.paciente.PacienteFilaDTO;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteApiController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class PacienteApiControllerTest {

    private static final String PACIENTE_VALIDO = """
            {"nombres": "Lucía", "apellidos": "Fernández Rivas", "tipoDocumento": "DNI",
             "numeroDocumento": "70000077", "telefono": "+51 987 654 321"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PacienteService pacienteService;

    @MockBean
    private UsuarioSesionService usuarioSesionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void sesion() {
        when(usuarioSesionService.requerir(any())).thenReturn(
                new UsuarioSesionDTO(1L, "Ana", "Ana Martínez Soto", "AM", "ADMINISTRADOR", "Administrador"));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/api/pacientes")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void laRecepcionistaBuscaPacientes() throws Exception {
        when(pacienteService.buscar(any(), eq("maria"), any(), anyInt())).thenReturn(PaginaDTO.de(List.of(
                PacienteFilaDTO.de(1L, "María", "González Pérez", "DNI", "70000001", "+51 987 654 321", true, 3)),
                1, 10, 1));

        mvc.perform(get("/api/pacientes").param("q", "maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombreCompleto").value("María González Pérez"))
                .andExpect(jsonPath("$.contenido[0].documento").value("DNI 70000001"));
    }

    @Test
    @WithMockUser(roles = "PSICOLOGO")
    void elPsicologoNoListaTodosLosPacientes() throws Exception {
        mvc.perform(get("/api/pacientes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PSICOLOGO")
    void elPsicologoConsultaElHistorialAutorizado() throws Exception {
        when(pacienteService.historial(any(), eq(5L))).thenReturn(List.of(new CitaPacienteDTO(100L,
                LocalDate.of(2026, 10, 20), LocalTime.of(9, 0), LocalTime.of(10, 0), "Laura Méndez Ríos",
                "PRESENCIAL", "ATENDIDA", "Atendida", null)));

        mvc.perform(get("/api/pacientes/5/historial-citas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoCodigo").value("ATENDIDA"))
                .andExpect(jsonPath("$[0].fecha").value("2026-10-20"));
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void elPacienteNoConsultaHistorialesDeOtros() throws Exception {
        mvc.perform(get("/api/pacientes/5/historial-citas")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void registraUnPacienteValido() throws Exception {
        when(pacienteService.registrar(any(), any())).thenReturn(30L);

        mvc.perform(post("/api/pacientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/pacientes/30")))
                .andExpect(jsonPath("$.id").value(30));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaUnPacienteSinCamposObligatorios() throws Exception {
        mvc.perform(post("/api/pacientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombres").exists())
                .andExpect(jsonPath("$.errores.apellidos").exists())
                .andExpect(jsonPath("$.errores.telefono").exists());
        verify(pacienteService, never()).registrar(any(), any());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void informaLasReglasDeNegocioIncumplidas() throws Exception {
        when(pacienteService.registrar(any(), any())).thenThrow(new ValidacionPacienteException(
                Map.of("numeroDocumento", "Ya existe un paciente registrado con ese documento.")));

        mvc.perform(post("/api/pacientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numeroDocumento")
                        .value("Ya existe un paciente registrado con ese documento."));
    }

    @Test
    @WithMockUser(roles = "PSICOLOGO")
    void elPsicologoNoRegistraPacientes() throws Exception {
        mvc.perform(post("/api/pacientes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isForbidden());
        verify(pacienteService, never()).registrar(any(), any());
    }
}
