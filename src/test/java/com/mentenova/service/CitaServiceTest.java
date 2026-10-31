package com.mentenova.service;

import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.cita.FiltroCitaDTO;
import com.mentenova.dto.cita.RegistrarCitaDTO;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.entity.Cita;
import com.mentenova.entity.Modalidad;
import com.mentenova.repository.CitaConsultaRepository;
import com.mentenova.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final UsuarioSesionDTO RECEPCION =
            new UsuarioSesionDTO(2L, "Rosa", "Rosa Huamán Quispe", "RH", "RECEPCIONISTA", "Recepcionista");
    private static final LocalDate MANANA = LocalDate.of(2026, 10, 31);
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime DIEZ = LocalTime.of(10, 0);

    @Mock
    private CitaRepository citas;

    @Mock
    private CitaConsultaRepository consultas;

    @Mock
    private DisponibilidadService disponibilidad;

    private CitaService servicio;

    @BeforeEach
    void crearServicio() {
        // 30/10/2026 a las 10:00 en Lima.
        Clock reloj = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new CitaService(citas, consultas, disponibilidad, reloj);
    }

    @Test
    void registraLaCitaPendienteDentroDeLaTransaccionDeDisponibilidad() {
        participantesActivos();
        when(consultas.pacienteTieneCruce(5L, MANANA, NUEVE, DIEZ)).thenReturn(false);
        when(consultas.idEstado("PENDIENTE")).thenReturn(Optional.of((short) 1));
        when(citas.saveAndFlush(any(Cita.class))).thenAnswer(invocacion -> {
            Cita cita = invocacion.getArgument(0);
            cita.setId(600L);
            return cita;
        });
        RegistrarCitaDTO form = formulario();
        form.setModalidad(Modalidad.VIRTUAL);
        form.setMotivoAdministrativo("  Primera consulta  ");
        form.setObservacion("   ");

        Long id = servicio.registrar(RECEPCION, form);

        assertThat(id).isEqualTo(600L);
        ArgumentCaptor<Cita> guardada = ArgumentCaptor.forClass(Cita.class);
        InOrder orden = inOrder(disponibilidad, consultas, citas);
        orden.verify(disponibilidad).validarParaReserva(1L, MANANA, NUEVE, DIEZ, Modalidad.VIRTUAL);
        orden.verify(consultas).registrarUsuarioAuditoria(2L);
        orden.verify(citas).saveAndFlush(guardada.capture());
        orden.verify(consultas).registrarHistorialCreacion(600L, 2L, (short) 1, MANANA, NUEVE);
        Cita cita = guardada.getValue();
        assertThat(cita.getIdPaciente()).isEqualTo(5L);
        assertThat(cita.getIdPsicologo()).isEqualTo(1L);
        assertThat(cita.getIdEstadoCita()).isEqualTo((short) 1);
        assertThat(cita.getModalidad()).isEqualTo("VIRTUAL");
        assertThat(cita.getMotivoAdministrativo()).isEqualTo("Primera consulta");
        assertThat(cita.getObservacion()).isNull();
        assertThat(cita.getCreadoPor()).isEqualTo(2L);
    }

    @Test
    void rechazaUnaFechaAnteriorAHoy() {
        participantesActivos();
        RegistrarCitaDTO form = formulario();
        form.setFecha(LocalDate.of(2026, 10, 29));

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e ->
                        assertThat(e.getErrores()).containsEntry("fecha", "La fecha no puede ser anterior a hoy."));
        verifyNoInteractions(disponibilidad);
        verify(citas, never()).saveAndFlush(any());
    }

    @Test
    void paraHoyExigeUnaHoraPosteriorALaActual() {
        participantesActivos();
        RegistrarCitaDTO form = formulario();
        form.setFecha(LocalDate.of(2026, 10, 30));

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e ->
                        assertThat(e.getErrores()).containsKey("horaInicio"));
        verify(citas, never()).saveAndFlush(any());
    }

    @Test
    void exigeQueLaHoraDeFinSeaPosteriorAlInicio() {
        participantesActivos();
        RegistrarCitaDTO form = formulario();
        form.setHoraFin(NUEVE);

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, form))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e ->
                        assertThat(e.getErrores()).containsEntry("horaFin", "La hora de fin debe ser posterior a la de inicio."));
    }

    @Test
    void noAgendaAUnPacienteInactivoNiAUnPsicologoInexistente() {
        when(consultas.pacienteActivo(5L)).thenReturn(Optional.of(false));
        when(consultas.psicologoActivo(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, formulario()))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e -> assertThat(e.getErrores())
                        .containsEntry("idPaciente", "El paciente está inactivo. Actívalo antes de agendarle una cita.")
                        .containsEntry("idPsicologo", "El psicólogo seleccionado no existe."));
        verifyNoInteractions(disponibilidad);
    }

    @Test
    void siElPsicologoNoEstaDisponibleNoGuarda() {
        participantesActivos();
        doThrow(new IllegalArgumentException("El intervalo solicitado ya no está disponible."))
                .when(disponibilidad).validarParaReserva(1L, MANANA, NUEVE, DIEZ, Modalidad.PRESENCIAL);

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, formulario()))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e ->
                        assertThat(e.getErrores().get("horaInicio")).contains("no está disponible"));
        verify(citas, never()).saveAndFlush(any());
    }

    @Test
    void noPermiteQueElPacienteTengaDosCitasCruzadas() {
        participantesActivos();
        when(consultas.pacienteTieneCruce(5L, MANANA, NUEVE, DIEZ)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, formulario()))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e ->
                        assertThat(e.getErrores().get("horaInicio")).contains("El paciente ya tiene otra cita"));
        verify(citas, never()).saveAndFlush(any());
    }

    @Test
    void traduceElRechazoDelTriggerDeSolapamiento() {
        participantesActivos();
        when(consultas.pacienteTieneCruce(5L, MANANA, NUEVE, DIEZ)).thenReturn(false);
        when(consultas.idEstado("PENDIENTE")).thenReturn(Optional.of((short) 1));
        when(citas.saveAndFlush(any(Cita.class))).thenThrow(new DataIntegrityViolationException("error",
                new RuntimeException("El psicologo ya posee una cita activa que se solapa con el horario solicitado")));

        assertThatThrownBy(() -> servicio.registrar(RECEPCION, formulario()))
                .isInstanceOfSatisfying(ValidacionCitaException.class, e ->
                        assertThat(e.getErrores().get("horaInicio")).contains("ya tiene una cita activa"));
        verify(consultas, never()).registrarHistorialCreacion(any(), any(), any(), any(), any());
    }

    @Test
    void laBandejaIgnoraEstadosDesconocidosYBuscaSinTildes() {
        when(consultas.estados()).thenReturn(List.of(new OpcionDTO("PENDIENTE", "Pendiente")));
        when(consultas.contar(any())).thenReturn(0L);
        when(consultas.listar(any(), anyInt(), anyInt())).thenReturn(List.of());

        servicio.buscar(null, "INVENTADO", "  María  ", 1);

        ArgumentCaptor<FiltroCitaDTO> filtro = ArgumentCaptor.forClass(FiltroCitaDTO.class);
        verify(consultas).listar(filtro.capture(), eq(CitaService.TAMANO_PAGINA), eq(0));
        assertThat(filtro.getValue()).isEqualTo(new FiltroCitaDTO(null, LocalDate.of(2026, 10, 30), null, "maria"));
    }

    private void participantesActivos() {
        when(consultas.pacienteActivo(5L)).thenReturn(Optional.of(true));
        when(consultas.psicologoActivo(1L)).thenReturn(Optional.of(true));
    }

    private static RegistrarCitaDTO formulario() {
        RegistrarCitaDTO form = new RegistrarCitaDTO();
        form.setIdPaciente(5L);
        form.setIdPsicologo(1L);
        form.setFecha(MANANA);
        form.setHoraInicio(NUEVE);
        form.setHoraFin(DIEZ);
        form.setModalidad(Modalidad.PRESENCIAL);
        return form;
    }
}
