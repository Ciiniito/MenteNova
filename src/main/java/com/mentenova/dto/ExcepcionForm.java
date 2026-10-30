package com.mentenova.dto;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import com.mentenova.entity.*;
import java.time.*;
public class ExcepcionForm {
    @NotNull(message="Selecciona la fecha.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate fecha;
    @NotNull(message="Indica la hora de inicio.") @DateTimeFormat(pattern="HH:mm")
    private LocalTime horaInicio;
    @NotNull(message="Indica la hora de fin.") @DateTimeFormat(pattern="HH:mm")
    private LocalTime horaFin;
    @NotNull(message="Selecciona bloqueo o habilitación.")
    private TipoExcepcion tipo = TipoExcepcion.BLOQUEO;
    @Size(max=250,message="El motivo admite hasta 250 caracteres.")
    private String motivo;
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate valor) { this.fecha=valor; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime valor) { this.horaInicio=valor; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime valor) { this.horaFin=valor; }
    public TipoExcepcion getTipo() { return tipo; }
    public void setTipo(TipoExcepcion valor) { this.tipo=valor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String valor) { this.motivo=valor == null ? null : valor.strip(); }
}
