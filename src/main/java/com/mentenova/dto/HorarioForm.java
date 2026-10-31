package com.mentenova.dto;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import com.mentenova.entity.*;
import java.time.*;
public class HorarioForm {
    @NotNull(message="Selecciona el día.") @Min(1) @Max(7)
    private Short diaSemana = 1;
    @NotNull(message="Indica la hora de inicio.") @DateTimeFormat(pattern="HH:mm")
    private LocalTime horaInicio;
    @NotNull(message="Indica la hora de fin.") @DateTimeFormat(pattern="HH:mm")
    private LocalTime horaFin;
    @NotNull(message="Selecciona la modalidad.")
    private Modalidad modalidad = Modalidad.PRESENCIAL;
    @NotNull(message="Selecciona el estado.")
    private Boolean activo = true;
    public Short getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Short valor) { this.diaSemana=valor; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime valor) { this.horaInicio=valor; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime valor) { this.horaFin=valor; }
    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad valor) { this.modalidad=valor; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean valor) { this.activo=valor; }
}
