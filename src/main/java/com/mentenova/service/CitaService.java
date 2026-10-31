package com.mentenova.service;

import com.mentenova.dto.PaginaDTO;
import com.mentenova.dto.UsuarioSesionDTO;
import com.mentenova.dto.cita.CitaDTO;
import com.mentenova.dto.cita.FiltroCitaDTO;
import com.mentenova.dto.cita.RegistrarCitaDTO;
import com.mentenova.dto.reporte.OpcionDTO;
import com.mentenova.entity.Cita;
import com.mentenova.repository.CitaConsultaRepository;
import com.mentenova.repository.CitaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class CitaService {

    public static final int TAMANO_PAGINA = 15;
    static final String ESTADO_INICIAL = "PENDIENTE";
    private static final int MAXIMO_MOTIVO = 250;
    private static final int MAXIMO_OBSERVACION = 500;

    private final CitaRepository citaRepository;
    private final CitaConsultaRepository consultas;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public CitaService(CitaRepository citaRepository, CitaConsultaRepository consultas,
                       DisponibilidadService disponibilidad, Clock reloj) {
        this.citaRepository = citaRepository;
        this.consultas = consultas;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    public PaginaDTO<CitaDTO> buscar(LocalDate fecha, String estado, String paciente, int pagina) {
        String texto = PacienteService.normalizarTexto(paciente);
        FiltroCitaDTO filtro = new FiltroCitaDTO(fecha, LocalDate.now(reloj), normalizarEstado(estado),
                texto.isEmpty() ? null : texto);
        long total = consultas.contar(filtro);
        int totalPaginas = (int) Math.max(1, (total + TAMANO_PAGINA - 1) / TAMANO_PAGINA);
        int paginaActual = Math.min(Math.max(1, pagina), totalPaginas);
        return PaginaDTO.de(consultas.listar(filtro, TAMANO_PAGINA, (paginaActual - 1) * TAMANO_PAGINA),
                paginaActual, TAMANO_PAGINA, total);
    }

    /** Devuelve el código si corresponde a un estado de cita; de lo contrario, {@code null} (sin filtro). */
    public String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String codigo = estado.strip();
        return estados().stream().anyMatch(e -> e.valor().equals(codigo)) ? codigo : null;
    }

    public List<OpcionDTO> estados() {
        return consultas.estados();
    }

    public List<OpcionDTO> pacientes() {
        return consultas.pacientesActivos();
    }

    public List<OpcionDTO> psicologos() {
        return consultas.psicologosActivos();
    }

    @Transactional
    public Long registrar(UsuarioSesionDTO usuario, RegistrarCitaDTO form) {
        validar(form);
        LocalDate fecha = form.getFecha();
        LocalTime inicio = form.getHoraInicio();
        LocalTime fin = form.getHoraFin();

        // Bloquea al psicólogo y revalida su disponibilidad en esta misma transacción (Docs/API_DISPONIBILIDAD.md).
        try {
            disponibilidad.validarParaReserva(form.getIdPsicologo(), fecha, inicio, fin, form.getModalidad());
        } catch (IllegalArgumentException e) {
            throw new ValidacionCitaException(Map.of("horaInicio",
                    "El psicólogo no está disponible en ese horario. Elige uno de los horarios libres."));
        }
        if (consultas.pacienteTieneCruce(form.getIdPaciente(), fecha, inicio, fin)) {
            throw new ValidacionCitaException(Map.of("horaInicio",
                    "El paciente ya tiene otra cita pendiente o confirmada que se cruza con ese horario."));
        }

        Short idEstado = consultas.idEstado(ESTADO_INICIAL)
                .orElseThrow(() -> new IllegalStateException("Falta el estado PENDIENTE en estado_cita."));
        Cita cita = new Cita();
        cita.setIdPaciente(form.getIdPaciente());
        cita.setIdPsicologo(form.getIdPsicologo());
        cita.setIdEstadoCita(idEstado);
        cita.setFecha(fecha);
        cita.setHoraInicio(inicio);
        cita.setHoraFin(fin);
        cita.setModalidad(form.getModalidad().name());
        cita.setMotivoAdministrativo(limpiar(form.getMotivoAdministrativo()));
        cita.setObservacion(limpiar(form.getObservacion()));
        cita.setCreadoPor(usuario.idUsuario());

        consultas.registrarUsuarioAuditoria(usuario.idUsuario());
        Cita guardada;
        try {
            guardada = citaRepository.saveAndFlush(cita);
        } catch (DataAccessException e) {
            String detalle = String.valueOf(e.getMostSpecificCause().getMessage());
            if (detalle.contains("solapa")) {
                throw new ValidacionCitaException(Map.of("horaInicio",
                        "El psicólogo ya tiene una cita activa que se cruza con ese horario."));
            }
            throw new ValidacionCitaException(Map.of(ValidacionCitaException.GENERAL,
                    "No se pudo registrar la cita. Revisa los datos e intenta nuevamente."));
        }
        consultas.registrarHistorialCreacion(guardada.getId(), usuario.idUsuario(), idEstado, fecha, inicio);
        return guardada.getId();
    }

    private void validar(RegistrarCitaDTO form) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (form.getIdPaciente() == null) {
            errores.put("idPaciente", "Selecciona el paciente.");
        } else {
            Optional<Boolean> activo = consultas.pacienteActivo(form.getIdPaciente());
            if (activo.isEmpty()) {
                errores.put("idPaciente", "El paciente seleccionado no existe.");
            } else if (!activo.get()) {
                errores.put("idPaciente", "El paciente está inactivo. Actívalo antes de agendarle una cita.");
            }
        }
        if (form.getIdPsicologo() == null) {
            errores.put("idPsicologo", "Selecciona el psicólogo.");
        } else {
            Optional<Boolean> activo = consultas.psicologoActivo(form.getIdPsicologo());
            if (activo.isEmpty()) {
                errores.put("idPsicologo", "El psicólogo seleccionado no existe.");
            } else if (!activo.get()) {
                errores.put("idPsicologo", "El psicólogo está inactivo y no puede recibir citas.");
            }
        }
        if (form.getModalidad() == null) {
            errores.put("modalidad", "Selecciona la modalidad.");
        }

        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fecha = form.getFecha();
        LocalTime inicio = form.getHoraInicio();
        LocalTime fin = form.getHoraFin();
        if (fecha == null) {
            errores.put("fecha", "Indica la fecha de la cita.");
        } else if (fecha.isBefore(hoy)) {
            errores.put("fecha", "La fecha no puede ser anterior a hoy.");
        }
        if (inicio == null) {
            errores.put("horaInicio", "Indica la hora de inicio.");
        }
        if (fin == null) {
            errores.put("horaFin", "Indica la hora de fin.");
        }
        if (inicio != null && fin != null && !fin.isAfter(inicio)) {
            errores.put("horaFin", "La hora de fin debe ser posterior a la de inicio.");
        } else if (inicio != null && hoy.equals(fecha) && !inicio.isAfter(LocalTime.now(reloj))) {
            errores.put("horaInicio", "Para hoy, la hora de inicio debe ser posterior a la hora actual.");
        }

        String motivo = limpiar(form.getMotivoAdministrativo());
        if (motivo != null && motivo.length() > MAXIMO_MOTIVO) {
            errores.put("motivoAdministrativo", "El motivo no puede exceder 250 caracteres.");
        }
        String observacion = limpiar(form.getObservacion());
        if (observacion != null && observacion.length() > MAXIMO_OBSERVACION) {
            errores.put("observacion", "La observación no puede exceder 500 caracteres.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionCitaException(errores);
        }
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
