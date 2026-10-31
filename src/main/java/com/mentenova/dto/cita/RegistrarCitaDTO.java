package com.mentenova.dto.cita;

import com.mentenova.entity.Modalidad;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class RegistrarCitaDTO {

    @NotNull(message = "Selecciona el paciente")
    private Long idPaciente;

    @NotNull(message = "Selecciona el psicólogo")
    private Long idPsicologo;

    @NotNull(message = "Indica la fecha de la cita")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull(message = "Indica la hora de inicio")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "Indica la hora de fin")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    @NotNull(message = "Selecciona la modalidad")
    private Modalidad modalidad = Modalidad.PRESENCIAL;

    @Size(max = 250, message = "El motivo no puede exceder 250 caracteres")
    private String motivoAdministrativo;

    @Size(max = 500, message = "La observación no puede exceder 500 caracteres")
    private String observacion;

    public Long getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(Long idPaciente) {
        this.idPaciente = idPaciente;
    }

    public Long getIdPsicologo() {
        return idPsicologo;
    }

    public void setIdPsicologo(Long idPsicologo) {
        this.idPsicologo = idPsicologo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public String getMotivoAdministrativo() {
        return motivoAdministrativo;
    }

    public void setMotivoAdministrativo(String motivoAdministrativo) {
        this.motivoAdministrativo = motivoAdministrativo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
