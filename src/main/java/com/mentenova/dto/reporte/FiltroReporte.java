package com.mentenova.dto.reporte;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;

public class FiltroReporte {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;

    private Long idPsicologo;

    private Short idEspecialidad;

    private String modalidad;

    private String estado;

    private String medioPago;

    private String estadoPago;

    private String agrupacion;

    public FiltroReporte() {
    }

    public FiltroReporte(LocalDate desde, LocalDate hasta, Long idPsicologo) {
        this.desde = desde;
        this.hasta = hasta;
        this.idPsicologo = idPsicologo;
    }

    public void completarPeriodo(LocalDate desdePorDefecto, LocalDate hastaPorDefecto) {
        if (desde == null) {
            desde = hasta != null && hasta.isBefore(desdePorDefecto) ? hasta.withDayOfMonth(1) : desdePorDefecto;
        }
        if (hasta == null) {
            hasta = desde.isAfter(hastaPorDefecto) ? YearMonth.from(desde).atEndOfMonth() : hastaPorDefecto;
        }
        if (desde.isAfter(hasta)) {
            LocalDate fecha = desde;
            desde = hasta;
            hasta = fecha;
        }
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public Long getIdPsicologo() {
        return idPsicologo;
    }

    public void setIdPsicologo(Long idPsicologo) {
        this.idPsicologo = idPsicologo;
    }

    public Short getIdEspecialidad() {
        return idEspecialidad;
    }

    public void setIdEspecialidad(Short idEspecialidad) {
        this.idEspecialidad = idEspecialidad;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getAgrupacion() {
        return agrupacion;
    }

    public void setAgrupacion(String agrupacion) {
        this.agrupacion = agrupacion;
    }
}
